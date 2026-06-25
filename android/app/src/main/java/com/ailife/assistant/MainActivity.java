package com.ailife.assistant;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ailife.assistant.action.ActionExecutor;
import com.ailife.assistant.network.ApiClient;
import com.ailife.assistant.network.model.ActionCommand;
import com.ailife.assistant.network.model.ChatResponse;
import com.ailife.assistant.network.model.Conversation;
import com.ailife.assistant.network.model.MsgRecord;
import com.ailife.assistant.util.NetworkMonitor;
import com.ailife.assistant.util.PermissionHelper;
import com.ailife.assistant.util.TokenManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String BASE_URL = "http://39.105.51.168:8082";

    private DrawerLayout drawerLayout;
    private RecyclerView recyclerMessages;
    private RecyclerView recyclerConversations;
    private EditText inputMessage;
    private Button btnSend;
    private Button btnLogout;
    private Button btnMenu;
    private Button btnNewChatSidebar;
    private TextView textTitle;

    private ChatAdapter adapter;
    private ConversationAdapter convAdapter;
    private ApiClient apiClient;
    private ActionExecutor actionExecutor;
    private NetworkMonitor networkMonitor;
    private TextView textNetworkStatus;
    private String token;
    private long userId;
    private long conversationId = 0;
    private boolean waitingReply = false;
    private boolean needRefreshTitle = false;  // 新对话首次发消息后需要刷新标题
    private List<Conversation> conversationList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Intent intent = getIntent();
        token = intent.getStringExtra("token");
        userId = intent.getLongExtra("userId", 0);

        if (token == null) {
            new TokenManager(this).logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        apiClient = new ApiClient(BASE_URL, token);
        actionExecutor = new ActionExecutor(this, apiClient);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, 100);
            }
        }

        // 延迟检查其他权限（等通知权限弹完后）
        new android.os.Handler(android.os.Looper.getMainLooper())
                .postDelayed(() -> PermissionHelper.checkAll(this), 800);

        // 绑定控件
        drawerLayout = findViewById(R.id.drawer_layout);
        recyclerMessages = findViewById(R.id.recycler_messages);
        recyclerConversations = findViewById(R.id.recycler_conversations);
        inputMessage = findViewById(R.id.input_message);
        btnSend = findViewById(R.id.btn_send);
        btnLogout = findViewById(R.id.btn_logout);
        btnMenu = findViewById(R.id.btn_menu);
        btnNewChatSidebar = findViewById(R.id.btn_new_chat_sidebar);
        textTitle = findViewById(R.id.text_title);

        // 消息列表
        adapter = new ChatAdapter();
        recyclerMessages.setLayoutManager(new LinearLayoutManager(this));
        recyclerMessages.setAdapter(adapter);

        // 侧边栏对话列表
        convAdapter = new ConversationAdapter();
        recyclerConversations.setLayoutManager(new LinearLayoutManager(this));
        recyclerConversations.setAdapter(convAdapter);

        convAdapter.setListener(new ConversationAdapter.OnConvClickListener() {
            @Override
            public void onClick(Conversation conv) {
                textTitle.setText(conv.getTitle());
                switchConversation(conv.getId());
                drawerLayout.closeDrawers();
            }

            @Override
            public void onDelete(Conversation conv) {
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("删除对话")
                        .setMessage("确定要删除「" + conv.getTitle() + "」吗？\n删除后无法恢复。")
                        .setPositiveButton("删除", (d, w) -> deleteConversation(conv))
                        .setNegativeButton("取消", null)
                        .show();
            }

            @Override
            public void onRename(Conversation conv) {
                final EditText input = new EditText(MainActivity.this);
                input.setText(conv.getTitle());
                input.setSelectAllOnFocus(true);
                new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                        .setTitle("重命名")
                        .setView(input)
                        .setPositiveButton("确定", (d, w) -> {
                            String newTitle = input.getText().toString().trim();
                            if (!newTitle.isEmpty() && !newTitle.equals(conv.getTitle())) {
                                renameConversation(conv, newTitle);
                            }
                        })
                        .setNegativeButton("取消", null)
                        .show();
            }
        });

        // 菜单按钮 → 打开/关闭侧边栏
        btnMenu.setOnClickListener(v -> {
            if (drawerLayout.isDrawerOpen(findViewById(R.id.drawer_sidebar))) {
                drawerLayout.closeDrawers();
            } else {
                drawerLayout.openDrawer(findViewById(R.id.drawer_sidebar));
            }
        });

        // 侧边栏「+」新对话
        btnNewChatSidebar.setOnClickListener(v -> {
            createNewConversation();
            drawerLayout.closeDrawers();
        });

        btnSend.setOnClickListener(v -> sendMessage());
        inputMessage.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendMessage();
                return true;
            }
            return false;
        });

        btnLogout.setOnClickListener(v -> {
            new TokenManager(this).logout();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        // 网络状态
        textNetworkStatus = findViewById(R.id.text_network_status);
        networkMonitor = new NetworkMonitor(this);
        networkMonitor.start(new NetworkMonitor.OnNetworkChangeListener() {
            @Override
            public void onLost() {
                showNetworkBar(true);
            }

            @Override
            public void onAvailable() {
                showNetworkBar(false);
                Toast.makeText(MainActivity.this, "网络已恢复", Toast.LENGTH_SHORT).show();
            }
        });

        // 启动加载
        loadConversations();
    }

    private void showNetworkBar(boolean show) {
        textNetworkStatus.setVisibility(show ? View.VISIBLE : View.GONE);
        textNetworkStatus.setText(show ? "⚠️ 网络已断开，请检查连接" : "");
    }

    // ==================== 对话管理 ====================

    private void loadConversations() {
        new Thread(() -> {
            try {
                conversationList = apiClient.getConversations(userId);
                runOnUiThread(() -> {
                    convAdapter.setItems(conversationList);
                    if (!conversationList.isEmpty()) {
                        Conversation first = conversationList.get(0);
                        convAdapter.setSelected(first.getId());
                        textTitle.setText(first.getTitle());
                        switchConversation(first.getId());
                    } else {
                        adapter.add(new ChatMessage(ChatMessage.TYPE_AI,
                                "👋 欢迎使用 AI 生活助手！开始聊天吧～"));
                        scrollToBottom();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    adapter.add(new ChatMessage(ChatMessage.TYPE_AI,
                            "👋 欢迎使用 AI 生活助手！开始聊天吧～"));
                    scrollToBottom();
                });
            }
        }).start();
    }

    private void switchConversation(long convId) {
        conversationId = convId;
        convAdapter.setSelected(convId);
        adapter.clear();
        new Thread(() -> {
            try {
                List<MsgRecord> records = apiClient.getMessages(convId);
                runOnUiThread(() -> {
                    for (MsgRecord r : records) {
                        int type = "user".equals(r.getRole()) ? ChatMessage.TYPE_USER : ChatMessage.TYPE_AI;
                        adapter.add(new ChatMessage(type, r.getContent()));
                    }
                    scrollToBottom();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "加载消息失败", Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void createNewConversation() {
        new Thread(() -> {
            try {
                Conversation conv = apiClient.createConversation(userId, "新对话");
                conversationList.add(0, conv);
                runOnUiThread(() -> {
                    conversationId = conv.getId();
                    adapter.clear();
                    textTitle.setText("新对话");
                    convAdapter.addFirst(conv);
                    convAdapter.setSelected(conv.getId());
                    needRefreshTitle = true;
                    Toast.makeText(this, "已创建新对话", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "创建失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void renameConversation(Conversation conv, String newTitle) {
        new Thread(() -> {
            try {
                apiClient.renameConversation(conv.getId(), newTitle);
                conv.setTitle(newTitle);
                runOnUiThread(() -> {
                    convAdapter.updateTitle(conv.getId(), newTitle);
                    if (conv.getId() == conversationId) {
                        textTitle.setText(newTitle);
                    }
                    Toast.makeText(this, "已重命名", Toast.LENGTH_SHORT).show();
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "重命名失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void deleteConversation(Conversation conv) {
        new Thread(() -> {
            try {
                apiClient.deleteConversation(conv.getId(), userId);
                conversationList.remove(conv);
                runOnUiThread(() -> {
                    convAdapter.remove(conv);
                    Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
                    // 如果删的是当前对话，切到第一个
                    if (conv.getId() == conversationId) {
                        if (!conversationList.isEmpty()) {
                            Conversation first = conversationList.get(0);
                            textTitle.setText(first.getTitle());
                            switchConversation(first.getId());
                        } else {
                            conversationId = 0;
                            adapter.clear();
                            textTitle.setText("AI 生活助手");
                        }
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }

    private void refreshConversationList() {
        new Thread(() -> {
            try {
                List<Conversation> fresh = apiClient.getConversations(userId);
                conversationList = fresh;
                runOnUiThread(() -> {
                    convAdapter.setItems(fresh);
                    // 更新标题
                    for (Conversation c : fresh) {
                        if (c.getId() == conversationId) {
                            textTitle.setText(c.getTitle());
                            break;
                        }
                    }
                });
            } catch (Exception ignored) {}
        }).start();
    }

    // ==================== 发送消息 ====================

    private void sendMessage() {
        String text = inputMessage.getText().toString().trim();
        if (text.isEmpty() || waitingReply) return;

        inputMessage.setText("");
        waitingReply = true;

        adapter.add(new ChatMessage(ChatMessage.TYPE_USER, text));
        scrollToBottom();

        // 本地拦截：取消闹钟（先本地执行，消息照发服务器记录）
        boolean localCancel = tryLocalCancelAlarm(text);

        boolean alarmSetLocally = tryLocalAlarm(text);
        String msgToSend = appendCurrentTime(text);

        // 如果本地已经处理了取消闹钟，不发占位，后端回复作为新消息追加
        if (!localCancel) {
            adapter.add(new ChatMessage(ChatMessage.TYPE_AI, "思考中..."));
        }
        scrollToBottom();

        // 同步请求
        new Thread(() -> {
            try {
                ChatResponse resp = apiClient.sendMessage(msgToSend, conversationId);
                final String fullReply = resp.getReply();
                List<ActionCommand> actions = resp.getActionCommands();

                if (alarmSetLocally && actions != null) {
                    actions.removeIf(cmd -> "set_alarm".equals(cmd.getAction()));
                }

                if (conversationId == 0 && resp.getConversationId() != null) {
                    try { conversationId = Long.parseLong(resp.getConversationId()); } catch (Exception ignored) {}
                }

                if (localCancel) {
                    // 取消操作：后端回复直接追加为新消息
                    runOnUiThread(() -> {
                        adapter.add(new ChatMessage(ChatMessage.TYPE_AI, fullReply, actions));
                        waitingReply = false;
                    });
                    return;
                }

                // 正常聊天：打字效果
                final boolean[] typing = {true};
                final int[] idx = {0};
                Runnable typeNext = new Runnable() {
                    @Override
                    public void run() {
                        if (!typing[0] || idx[0] >= fullReply.length()) {
                            runOnUiThread(() -> {
                                adapter.updateLastAi(fullReply);
                                if (actions != null && !actions.isEmpty()) {
                                    adapter.setLastAiActions(actions);
                                    new Thread(() -> actionExecutor.execute(actions, fullReply)).start();
                                }
                                waitingReply = false;
                                if (needRefreshTitle) { needRefreshTitle = false; refreshConversationList(); }
                            });
                            return;
                        }
                        idx[0] += 2;
                        int end = Math.min(idx[0], fullReply.length());
                        String partial = fullReply.substring(0, end);
                        runOnUiThread(() -> { adapter.updateLastAi(partial); scrollToBottom(); });
                        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(this, 30);
                    }
                };
                runOnUiThread(() -> typeNext.run());
            } catch (Exception e) {
                runOnUiThread(() -> {
                    if (localCancel) {
                        adapter.add(new ChatMessage(ChatMessage.TYPE_AI, "❌ 连接失败: " + e.getMessage()));
                    } else {
                        adapter.updateLastAi("❌ 连接失败: " + e.getMessage());
                    }
                    waitingReply = false;
                });
            }
        }).start();
    }

    // ==================== 本地闹钟 ====================

    private boolean tryLocalCancelAlarm(String text) {
        if ((!text.contains("取消") && !text.contains("关掉") && !text.contains("关闭")
                && !text.contains("删除") && !text.contains("删掉")) || !text.contains("闹钟")) {
            return false;
        }
        runOnUiThread(() -> {
            adapter.add(new ChatMessage(ChatMessage.TYPE_AI, "🔕 正在取消闹钟..."));
            scrollToBottom();
        });
        new Thread(() -> {
            com.ailife.assistant.action.AlarmHelper helper =
                    new com.ailife.assistant.action.AlarmHelper(this);
            String label = null;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(取消|关掉|关闭|删除|删掉)(\\S+)").matcher(text);
            if (m.find()) {
                label = m.group(2).replace("闹钟", "").replace("的", "").trim();
                if (label.isEmpty()) label = null;
            }
            int n = label != null ? helper.cancelByLabel(label) : helper.cancelAll();
            String result = n > 0
                    ? "✅ 已取消闹钟（" + n + " 个）" + (label != null ? "：" + label : "")
                    : "⚠️ 未找到匹配的闹钟";
            runOnUiThread(() -> {
                adapter.add(new ChatMessage(ChatMessage.TYPE_AI, result));
                scrollToBottom();
            });
        }).start();
        return true;
    }

    private boolean tryLocalAlarm(String text) {
        if (!text.contains("闹钟") && !text.contains("提醒") && !text.contains("叫我")) return false;

        Calendar now = Calendar.getInstance();
        Calendar target = null;
        String label = "AI 提醒";
        String dayContext = ""; // "今天" / "明天" / "后天" / "大后天"

        // 提取标签：叫我XXX
        java.util.regex.Matcher labelMatcher = java.util.regex.Pattern.compile("叫我(\\S+)").matcher(text);
        if (labelMatcher.find()) label = labelMatcher.group(1);

        // ===== 方式1：相对时间 — X秒后 / X分钟后 / X小时后 =====
        java.util.regex.Matcher mSec = java.util.regex.Pattern.compile("(\\d+)\\s*秒后").matcher(text);
        if (mSec.find()) { target = (Calendar) now.clone(); target.add(Calendar.SECOND, Integer.parseInt(mSec.group(1))); }
        if (target == null) {
            java.util.regex.Matcher mMin = java.util.regex.Pattern.compile("(\\d+)\\s*分钟后").matcher(text);
            if (mMin.find()) { target = (Calendar) now.clone(); target.add(Calendar.MINUTE, Integer.parseInt(mMin.group(1))); }
        }
        if (target == null) {
            java.util.regex.Matcher mHour = java.util.regex.Pattern.compile("(\\d+)\\s*小时后").matcher(text);
            if (mHour.find()) { target = (Calendar) now.clone(); target.add(Calendar.HOUR_OF_DAY, Integer.parseInt(mHour.group(1))); }
        }

        // ===== 方式2：绝对时间 — 明天早上10点 / 下午3点半 / 晚上8点 =====
        if (target == null) {
            target = parseAbsoluteTime(text);
            dayContext = detectDayContext(text);
        }

        if (target == null) return false;

        // 相对时间用 HH:mm:ss（保留秒），绝对时间用 HH:mm + dayContext
        final SimpleDateFormat sdf;
        final String timeStr;
        if (dayContext.isEmpty()) {
            sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        } else {
            sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        }
        timeStr = sdf.format(target.getTime());

        final String finalLabel = label;
        final String aiReply = dayContext.isEmpty() ? null : dayContext;

        java.util.Map<String, String> alarmParams = new java.util.HashMap<>();
        alarmParams.put("time", timeStr);
        alarmParams.put("label", finalLabel);
        String result = new com.ailife.assistant.action.AlarmHelper(this).setAlarm(alarmParams, aiReply);

        runOnUiThread(() -> {
            adapter.add(new ChatMessage(ChatMessage.TYPE_AI, result));
            scrollToBottom();
        });

        return true;
    }

    // ===== 绝对时间解析 =====

    /**
     * 解析绝对时间，如 "明天早上10点" "下午3点半" "晚上8点"
     * @return 今天的时间（不含日期偏移），日期偏移由 detectDayContext 处理
     */
    private Calendar parseAbsoluteTime(String text) {
        // 匹配: [时段]HH[点:](半|MM分)?
        java.util.regex.Matcher m = java.util.regex.Pattern.compile(
                "(凌晨|早上|上午|中午|下午|晚上)?(\\d{1,2})[点:：](半|(\\d{1,2})[分]?)?"
        ).matcher(text);

        if (!m.find()) return null;

        String period = m.group(1);       // 凌晨/早上/上午/中午/下午/晚上
        int hour = Integer.parseInt(m.group(2));
        String halfOrMin = m.group(3);    // "半" or "XX分"
        int minute = 0;
        if (halfOrMin != null) {
            if ("半".equals(halfOrMin)) {
                minute = 30;
            } else {
                minute = Integer.parseInt(m.group(4));
            }
        }

        // 转 24 小时制
        if (period == null) {
            // 没有时段修饰，默认 0-6 点为凌晨，7-12 为上午，13-23 为下午
            // 不做转换，保持原样（用户的 24 小时制输入如 "15:30"）
        } else if ("下午".equals(period) || "晚上".equals(period)) {
            if (hour < 12) hour += 12;
        }
        // 凌晨/早上/上午/中午: 保持原小时

        // 晚上12点 = 00:00（次日凌晨），特殊处理
        if ("晚上".equals(period) && hour == 12) {
            hour = 0;
        }

        Calendar target = Calendar.getInstance();
        target.set(Calendar.HOUR_OF_DAY, hour);
        target.set(Calendar.MINUTE, minute);
        target.set(Calendar.SECOND, 0);
        target.set(Calendar.MILLISECOND, 0);
        return target;
    }

    /**
     * 检测日期偏移关键词
     */
    private String detectDayContext(String text) {
        if (text.contains("大后天")) return "大后天";
        if (text.contains("后天")) return "后天";
        if (text.contains("明天") || text.contains("次日")) return "明天";
        if (text.contains("今天")) return "";
        return "";
    }

    private String appendCurrentTime(String text) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        return text + "（现在时间是 " + sdf.format(new Date()) + "，星期" +
                new String[]{"日","一","二","三","四","五","六"}[Calendar.getInstance().get(Calendar.DAY_OF_WEEK) - 1] + "）";
    }

    private void scrollToBottom() {
        recyclerMessages.post(() -> {
            int last = adapter.getItemCount() - 1;
            if (last >= 0) recyclerMessages.smoothScrollToPosition(last);
        });
    }
}

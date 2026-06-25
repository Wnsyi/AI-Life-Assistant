package com.ailife.assistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ailife.assistant.network.model.ActionCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import io.noties.markwon.Markwon;

/**
 * 聊天消息的 RecyclerView 适配器
 * 用户消息：纯文本蓝底、AI 消息：Markdown 渲染灰底
 */
public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.BaseHolder> {

    private static final int TYPE_USER = 0;
    private static final int TYPE_AI = 1;

    private final List<ChatMessage> messages = new ArrayList<>();
    private Markwon markwon;

    public void add(ChatMessage msg) {
        messages.add(msg);
        notifyItemInserted(messages.size() - 1);
    }

    public void clear() {
        messages.clear();
        notifyDataSetChanged();
    }

    /** 更新最后一条 AI 消息的文本（用于流式输出） */
    public void updateLastAi(String newContent) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getType() == ChatMessage.TYPE_AI) {
                messages.get(i).setContent(newContent);
                notifyItemChanged(i);
                return;
            }
        }
    }

    /** 给最后一条 AI 消息设置 actionCommands */
    public void setLastAiActions(List<ActionCommand> actions) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            if (messages.get(i).getType() == ChatMessage.TYPE_AI) {
                messages.get(i).setActions(actions);
                notifyItemChanged(i);
                return;
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return messages.get(position).getType();
    }

    @NonNull
    @Override
    public BaseHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (markwon == null) {
            markwon = Markwon.create(parent.getContext());
        }
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        if (viewType == TYPE_USER) {
            View v = inflater.inflate(R.layout.item_message_user, parent, false);
            return new UserHolder(v);
        } else {
            View v = inflater.inflate(R.layout.item_message_ai, parent, false);
            return new AiHolder(v);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull BaseHolder holder, int position) {
        holder.bind(messages.get(position));
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    abstract class BaseHolder extends RecyclerView.ViewHolder {
        BaseHolder(@NonNull View itemView) {
            super(itemView);
        }
        abstract void bind(ChatMessage msg);
    }

    class UserHolder extends BaseHolder {
        TextView content;

        UserHolder(@NonNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.text_content);
        }

        @Override
        void bind(ChatMessage msg) {
            content.setText(msg.getContent());
        }
    }

    class AiHolder extends BaseHolder {
        TextView content;
        LinearLayout actionsContainer;

        AiHolder(@NonNull View itemView) {
            super(itemView);
            content = itemView.findViewById(R.id.text_content);
            actionsContainer = itemView.findViewById(R.id.actions_container);
        }

        @Override
        void bind(ChatMessage msg) {
            // Markdown 渲染
            markwon.setMarkdown(content, msg.getContent());

            actionsContainer.removeAllViews();
            if (msg.hasActions()) {
                actionsContainer.setVisibility(View.VISIBLE);
                for (ActionCommand cmd : msg.getActions()) {
                    TextView tv = new TextView(itemView.getContext());
                    tv.setText(buildActionText(cmd));
                    tv.setPadding(24, 8, 24, 8);
                    tv.setTextSize(13);
                    tv.setTextColor(0xFF333333);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            LinearLayout.LayoutParams.WRAP_CONTENT);
                    lp.topMargin = 8;
                    tv.setLayoutParams(lp);
                    tv.setBackgroundResource(R.drawable.bubble_ai);
                    actionsContainer.addView(tv);
                }
            } else {
                actionsContainer.setVisibility(View.GONE);
            }
        }

        private String buildActionText(ActionCommand cmd) {
            switch (cmd.getAction()) {
                case "set_alarm": {
                    Map<String, String> p = cmd.getParams();
                    return "⏰ 闹钟: " + (p != null ? p.getOrDefault("time", "") : "");
                }
                case "send_notification": {
                    Map<String, String> p = cmd.getParams();
                    return "📢 通知: " + (p != null ? p.getOrDefault("title", "") : "");
                }
                case "open_app": {
                    Map<String, String> p = cmd.getParams();
                    return "📱 打开: " + (p != null ? p.getOrDefault("appName", "") : "");
                }
                default:
                    return "🔧 " + cmd.getAction();
            }
        }
    }
}

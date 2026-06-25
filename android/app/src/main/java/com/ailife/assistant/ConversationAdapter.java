package com.ailife.assistant;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.ailife.assistant.network.model.Conversation;

import java.util.ArrayList;
import java.util.List;

/**
 * 侧边栏对话列表适配器
 */
public class ConversationAdapter extends RecyclerView.Adapter<ConversationAdapter.Holder> {

    private final List<Conversation> items = new ArrayList<>();
    private OnConvClickListener listener;
    private long selectedConvId = -1;

    public interface OnConvClickListener {
        void onClick(Conversation conv);
        void onDelete(Conversation conv);
        void onRename(Conversation conv);
    }

    public void updateTitle(long convId, String newTitle) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == convId) {
                items.get(i).setTitle(newTitle);
                notifyItemChanged(i);
                return;
            }
        }
    }

    public void setListener(OnConvClickListener listener) {
        this.listener = listener;
    }

    public void setSelected(long convId) {
        long old = selectedConvId;
        selectedConvId = convId;
        // 刷新旧选中项
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == old) { notifyItemChanged(i); break; }
        }
        // 刷新新选中项
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId() == convId) { notifyItemChanged(i); break; }
        }
    }

    public void setItems(List<Conversation> list) {
        items.clear();
        items.addAll(list);
        notifyDataSetChanged();
    }

    public void addFirst(Conversation conv) {
        items.add(0, conv);
        notifyItemInserted(0);
    }

    public void remove(Conversation conv) {
        int idx = items.indexOf(conv);
        if (idx >= 0) {
            items.remove(idx);
            notifyItemRemoved(idx);
        }
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_conversation, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Conversation conv = items.get(position);
        holder.title.setText(conv.getTitle());
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(conv);
        });
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDelete(conv);
        });
        holder.itemView.setOnLongClickListener(v -> {
            if (listener != null) listener.onRename(conv);
            return true;
        });

        // 高亮选中项
        boolean selected = conv.getId() == selectedConvId;
        holder.itemView.setBackgroundColor(selected ? 0xFFE8EEFF : 0x00000000);
        holder.title.setTextColor(selected ? 0xFF4E6EF2 : 0xFF1F2937);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        TextView title;
        Button btnDelete;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_conv_title);
            btnDelete = itemView.findViewById(R.id.btn_delete_conv);
        }
    }
}

package com.zcshou.gogogo;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {
    private final List<HistoryItem> allItems = new ArrayList<>();
    private final List<HistoryItem> filteredItems = new ArrayList<>();
    private OnItemClickListener onItemClickListener;
    private OnItemLongClickListener onItemLongClickListener;

    public interface OnItemClickListener {
        void onItemClick(HistoryItem item);
    }

    public interface OnItemLongClickListener {
        void onItemLongClick(View anchorView, HistoryItem item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.onItemLongClickListener = listener;
    }

    // ==================== 数据操作 ====================

    /**
     * 设置全量数据（首次加载 / 搜索重置 / 删除全部后调用）
     */
    public void setData(List<HistoryItem> items) {
        allItems.clear();
        if (items != null) {
            allItems.addAll(items);
        }
        // 默认显示全部
        filteredItems.clear();
        filteredItems.addAll(allItems);
        notifyDataSetChanged();
    }

    /**
     * 更新单条 item 的标题（仅刷新该 item，不重建列表）
     */
    public void updateItemTitle(int id, String newTitle) {
        // 同步更新 allItems
        for (HistoryItem item : allItems) {
            if (item.getId() == id) {
                item.setLocation(newTitle);
                break;
            }
        }
        // 同步更新 filteredItems 并精准刷新
        for (int i = 0; i < filteredItems.size(); i++) {
            if (filteredItems.get(i).getId() == id) {
                filteredItems.get(i).setLocation(newTitle);
                notifyItemChanged(i);
                break;
            }
        }
    }

    /**
     * 删除单条 item（仅刷新删除项，不重建列表）
     */
    public boolean removeItemById(int id) {
        // 从 allItems 中移除
        for (int i = 0; i < allItems.size(); i++) {
            if (allItems.get(i).getId() == id) {
                allItems.remove(i);
                break;
            }
        }
        // 从 filteredItems 中移除
        for (int i = 0; i < filteredItems.size(); i++) {
            if (filteredItems.get(i).getId() == id) {
                filteredItems.remove(i);
                notifyItemRemoved(i);
                return true;
            }
        }
        return false;
    }

    /**
     * 搜索过滤
     */
    public void filter(String query) {
        filteredItems.clear();
        if (TextUtils.isEmpty(query)) {
            filteredItems.addAll(allItems);
        } else {
            for (HistoryItem item : allItems) {
                if (item.matches(query)) {
                    filteredItems.add(item);
                }
            }
        }
        notifyDataSetChanged();
    }

    /**
     * 当前过滤后的数据是否为空
     */
    public boolean isFilteredEmpty() {
        return filteredItems.isEmpty();
    }

    // ==================== RecyclerView.Adapter ====================

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.history_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = filteredItems.get(position);
        holder.locationText.setText(item.getLocation());
        holder.timeText.setText(item.getTime());
        holder.wgsLatLngText.setText(item.getWgsLatLngText());

        holder.itemView.setOnClickListener(v -> {
            if (onItemClickListener != null) {
                onItemClickListener.onItemClick(item);
            }
        });

        holder.itemView.setOnLongClickListener(v -> {
            if (onItemLongClickListener != null) {
                onItemLongClickListener.onItemLongClick(v, item);
            }
            return true; // 消费长按事件
        });
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    // ==================== ViewHolder ====================

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView locationText;
        TextView timeText;
        TextView wgsLatLngText;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            locationText = itemView.findViewById(R.id.LocationText);
            timeText = itemView.findViewById(R.id.TimeText);
            wgsLatLngText = itemView.findViewById(R.id.WGSLatLngText);
        }
    }
}

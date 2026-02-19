package com.example.emergencypreparednessmanager.UI.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.ItemSearchRow;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public class ItemSearchAdapter extends RecyclerView.Adapter<ItemSearchAdapter.VH> {

    public interface OnRowClickListener {
        void onRowClick(ItemSearchRow row);
    }

    private final LayoutInflater inflater;
    private final OnRowClickListener listener;
    private final List<ItemSearchRow> items = new ArrayList<>();

    public ItemSearchAdapter(Context context, OnRowClickListener listener) {
        this.inflater = LayoutInflater.from(context);
        this.listener = listener;
    }

    public void setItems(List<ItemSearchRow> newItems) {
        items.clear();
        if (newItems != null) items.addAll(newItems);
        notifyDataSetChanged();
    }

    public List<ItemSearchRow> getItems() {
        return items;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_item_search, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        ItemSearchRow row = items.get(position);

        holder.textItem.setText(safe(row.getItemName()));
        holder.textKit.setText(safe(row.getKitName()));
        holder.textCategory.setText(safe(row.getCategoryName()));
        holder.textQuantity.setText(String.valueOf(row.getQuantity()));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onRowClick(row);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        MaterialTextView textItem;
        MaterialTextView textKit;
        MaterialTextView textCategory;
        MaterialTextView textQuantity;

        VH(@NonNull View itemView) {
            super(itemView);
            textItem = itemView.findViewById(R.id.textItem);
            textKit = itemView.findViewById(R.id.textKit);
            textCategory = itemView.findViewById(R.id.textCategory);
            textQuantity = itemView.findViewById(R.id.textQuantity);
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s.trim();
    }
}

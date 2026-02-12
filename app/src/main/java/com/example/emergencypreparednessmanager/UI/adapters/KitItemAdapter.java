package com.example.emergencypreparednessmanager.UI.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.UI.activities.KitItemEditActivity;
import com.example.emergencypreparednessmanager.entities.KitItem;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying KitItems in a RecyclerView.
 * Handles clicks to open KitItemDetailsActivity.
 */
public class KitItemAdapter extends RecyclerView.Adapter<KitItemAdapter.KitItemViewHolder> {

    // ------------------- FIELDS -------------------

    private final Context context;
    private final LayoutInflater inflater;
    private List<KitItem> items = new ArrayList<>();

    // ------------------- CONSTRUCTOR -------------------

    public KitItemAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    // ------------------- RECYCLER -------------------

    @NonNull
    @Override
    public KitItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.kit_item_list_item, parent, false);
        return new KitItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull KitItemViewHolder holder, int position) {
        holder.bind(items.get(position));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // ------------------- DATA -------------------

    public List<KitItem> getItems() {
        return items;
    }

    public void setItems(List<KitItem> itemList) {
        this.items = itemList != null ? itemList : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ------------------- VIEW HOLDER -------------------

    class KitItemViewHolder extends RecyclerView.ViewHolder {

        private final MaterialTextView itemNameText;
        private final MaterialTextView itemMetaText;

        KitItemViewHolder(@NonNull View itemView) {
            super(itemView);

            itemNameText = itemView.findViewById(R.id.itemNameText);
            itemMetaText = itemView.findViewById(R.id.itemMetaText);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                KitItem current = items.get(position);

                Intent intent = new Intent(context, KitItemEditActivity.class);
                intent.putExtra(KitItemEditActivity.EXTRA_KIT_ID, current.getKitID());
                intent.putExtra(KitItemEditActivity.EXTRA_ITEM_ID, current.getItemID());
                context.startActivity(intent);
            });
        }

        void bind(KitItem item) {
            itemNameText.setText(item.getItemName());

            String qty = context.getString(R.string.qty_format, item.getQuantity());

            String exp = item.getExpirationDate();
            String expText = TextUtils.isEmpty(exp)
                    ? context.getString(R.string.no_expiration)
                    : context.getString(R.string.expires_format, exp.trim());

            itemMetaText.setText(qty + " • " + expText);
        }
    }
}

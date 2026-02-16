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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying KitItems in a RecyclerView.
 * Handles clicks to open KitItemDetailsActivity.
 */
public class KitItemAdapter extends RecyclerView.Adapter<KitItemAdapter.KitItemViewHolder> {

    public interface OnQuantityChangeListener {
        /**
         * Called after the adapter updates the in-memory item quantity.
         * Persist the change (Room update) in the Activity.
         */
        void onAdjustQuantity(int itemId, int delta);
    }

    // ------------------- FIELDS -------------------

    private final Context context;
    private final LayoutInflater inflater;
    private final OnQuantityChangeListener quantityChangeListener;
    private List<KitItem> items = new ArrayList<>();

    // ------------------- CONSTRUCTOR -------------------

    public KitItemAdapter(Context context, @NonNull OnQuantityChangeListener listener) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.quantityChangeListener = listener;
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

    public void replaceItem(@NonNull KitItem updated) {
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getItemID() == updated.getItemID()) {
                items.set(i, updated);
                notifyItemChanged(i);
                return;
            }
        }
    }

    // ------------------- VIEW HOLDER -------------------

    class KitItemViewHolder extends RecyclerView.ViewHolder {

        private final MaterialTextView itemNameText;
        private final MaterialTextView qtySummary;
        private final MaterialTextView itemMetaText;

        private final MaterialButton btnDecrement;
        private final MaterialTextView quantityText;
        private final MaterialButton btnIncrement;

        KitItemViewHolder(@NonNull View itemView) {
            super(itemView);

            itemNameText = itemView.findViewById(R.id.itemNameText);
            qtySummary = itemView.findViewById(R.id.qtySummary);
            itemMetaText = itemView.findViewById(R.id.itemMetaText);

            btnDecrement = itemView.findViewById(R.id.btnDecrement);
            quantityText = itemView.findViewById(R.id.quantityText);
            btnIncrement = itemView.findViewById(R.id.btnIncrement);

            itemView.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                KitItem current = items.get(position);

                Intent intent = new Intent(context, KitItemEditActivity.class);
                intent.putExtra(KitItemEditActivity.EXTRA_KIT_ID, current.getKitID());
                intent.putExtra(KitItemEditActivity.EXTRA_ITEM_ID, current.getItemID());
                context.startActivity(intent);
            });

            btnDecrement.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                KitItem current = items.get(position);
                if (current.getQuantity() <= 0) return;

                btnDecrement.setEnabled(false);
                btnIncrement.setEnabled(false);

                quantityChangeListener.onAdjustQuantity(current.getItemID(), -1);
            });

            btnIncrement.setOnClickListener(v -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                KitItem current = items.get(position);

                btnDecrement.setEnabled(false);
                btnIncrement.setEnabled(false);

                quantityChangeListener.onAdjustQuantity(current.getItemID(), +1);
            });
        }

        void bind(KitItem item) {
            itemNameText.setText(item.getItemName());

            int qty = item.getQuantity();

            // Right side summary
            qtySummary.setText("Qty: " + qty);

            // Number between the buttons
            quantityText.setText(String.valueOf(qty));

            // Expiration text (left, row 2)
            String exp = item.getExpirationDate();
            String expText = TextUtils.isEmpty(exp)
                    ? context.getString(R.string.no_expiration)
                    : context.getString(R.string.expires_format, exp.trim());

            itemMetaText.setText(expText);

            // Buttons state
            btnIncrement.setEnabled(true);
            btnDecrement.setEnabled(qty > 0);
        }
    }
}

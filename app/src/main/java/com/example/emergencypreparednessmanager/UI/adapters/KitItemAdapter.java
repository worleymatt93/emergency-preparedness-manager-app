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
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying KitItems in a RecyclerView.
 * Handles clicks to open KitItemDetailsActivity.
 */
public class KitItemAdapter extends RecyclerView.Adapter<KitItemAdapter.KitItemViewHolder> {

    public interface OnQuantityChangeListener {
        void onAdjustQuantity(int itemId, int delta);
    }

    // ------------------- FIELDS -------------------

    private final Context context;
    private final LayoutInflater inflater;
    private final OnQuantityChangeListener quantityChangeListener;
    private List<KitItem> items = new ArrayList<>();
    private int highlightedItemId = -1;

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
        KitItem item = items.get(position);
        holder.bind(item);

        boolean highlight = item.getItemID() == highlightedItemId;
        holder.applyHighlight(highlight);
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

    public void setHighlightedItemId(int itemId) {
        highlightedItemId = itemId;
        notifyDataSetChanged();
    }

    // ------------------- VIEW HOLDER -------------------

    public class KitItemViewHolder extends RecyclerView.ViewHolder {

        private final MaterialCardView card;

        private final MaterialTextView itemNameText;
        private final MaterialTextView qtySummary;
        private final MaterialTextView itemMetaText;

        private final MaterialButton btnDecrement;
        private final MaterialTextView quantityText;
        private final MaterialButton btnIncrement;

        KitItemViewHolder(@NonNull View itemView) {
            super(itemView);

            card = (MaterialCardView) itemView;

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

            // Summary at top-right
            qtySummary.setText("Qty: " + qty);

            // Quantity between buttons
            quantityText.setText(String.valueOf(qty));

            // Expiration text
            String exp = item.getExpirationDate();
            String expText = TextUtils.isEmpty(exp)
                    ? context.getString(R.string.no_expiration)
                    : context.getString(R.string.expires_format, exp.trim());

            itemMetaText.setText(expText);

            // Buttons state
            btnIncrement.setEnabled(true);
            btnDecrement.setEnabled(qty > 0);
        }

        void applyHighlight(boolean highlight) {
            if (highlight) {
                card.setStrokeWidth(dpToPx(2));
                card.setStrokeColor(MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary));
            } else {
                card.setStrokeWidth(dpToPx(1));
                card.setStrokeColor(MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutline));
            }
        }

        // ------------------- HELPERS -------------------

        private int dpToPx(int dp) {
            return Math.round(dp * context.getResources().getDisplayMetrics().density);
        }
    }
}

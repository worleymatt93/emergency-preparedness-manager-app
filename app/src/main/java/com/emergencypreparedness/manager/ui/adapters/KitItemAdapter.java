package com.emergencypreparedness.manager.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencypreparedness.manager.R;
import com.emergencypreparedness.manager.entities.KitItem;
import com.emergencypreparedness.manager.ui.activities.KitItemEditActivity;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * RecyclerView adapter for displaying kit items with quantity controls.
 * <p>
 * Uses {@link ListAdapter} + DiffUtil for efficient updates. Supports:
 * <ul>
 *   <li>Clicking an item to edit it</li>
 *   <li>Increment/decrement quantity (via listener)</li>
 *   <li>Temporary highlighting of a specific item</li>
 * </ul>
 */
public class KitItemAdapter extends ListAdapter<KitItem, KitItemAdapter.KitItemViewHolder> {

  //region DiffUtil
  private static final DiffUtil.ItemCallback<KitItem> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull KitItem oldItem, @NonNull KitItem newItem) {
          return oldItem.getItemID() == newItem.getItemID();
        }

        @Override
        public boolean areContentsTheSame(@NonNull KitItem oldItem, @NonNull KitItem newItem) {
          return safe(oldItem.getItemName()).equals(safe(newItem.getItemName()))
              && oldItem.getQuantity() == newItem.getQuantity()
              && safe(oldItem.getExpirationDate()).equals(safe(newItem.getExpirationDate()))
              && safe(oldItem.getNotes()).equals(safe(newItem.getNotes()))
              && oldItem.isExpirationRemindersEnabled() == newItem.isExpirationRemindersEnabled()
              && oldItem.getNotifyDaysBefore() == newItem.getNotifyDaysBefore()
              && oldItem.isNotifyOnZero() == newItem.isNotifyOnZero()
              && oldItem.getCategoryID() == newItem.getCategoryID()
              && oldItem.getKitID() == newItem.getKitID();
        }
      };
  //region Fields
  private final Context context;
  private final LayoutInflater inflater;
  private final OnQuantityChangeListener quantityChangeListener;
  //endregion
  private int highlightedItemId = -1;
  //endregion

  //region Constructor

  /**
   * Creates a ListAdapter-backed adapter for kit items.
   *
   * @param context  context for inflating views and launching edit activity
   * @param listener callback for quantity adjustments
   */
  public KitItemAdapter(Context context, @NonNull OnQuantityChangeListener listener) {
    super(DIFF_CALLBACK);
    this.context = context;
    this.inflater = LayoutInflater.from(context);
    this.quantityChangeListener = listener;
  }
  //endregion

  private static String safe(String s) {
    return (s == null) ? "" : s.trim();
  }

  //region RecyclerView Overrides
  @NonNull
  @Override
  public KitItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = inflater.inflate(R.layout.kit_item_list_item, parent, false);
    return new KitItemViewHolder(view);
  }
  //endregion

  //region Data Management

  @Override
  public void onBindViewHolder(@NonNull KitItemViewHolder holder, int position) {
    KitItem item = getItem(position);
    holder.bind(item);

    boolean highlight = item.getItemID() == highlightedItemId;
    holder.applyHighlight(highlight);
  }

  /**
   * Convenience wrapper that submits a defensive copy (mutable list).
   * <p>
   * This prevents accidental shared-list mutations from outside the adapter.
   *
   * @param itemList new list of items (can be null)
   */
  public void setItems(List<KitItem> itemList) {
    List<KitItem> safeCopy = (itemList == null) ? new ArrayList<>() : new ArrayList<>(itemList);
    submitList(safeCopy);
  }

  /**
   * Returns the item currently displayed at adapter position.
   *
   * @param position adapter position
   * @return item at position
   */
  public KitItem getItemAt(int position) {
    return getItem(position);
  }

  /**
   * Replaces a single item by ID and submits an updated list.
   *
   * @param updated updated kit item
   */
  public void replaceItem(@NonNull KitItem updated) {
    List<KitItem> copy = new ArrayList<>(getCurrentList());
    for (int i = 0; i < copy.size(); i++) {
      if (copy.get(i).getItemID() == updated.getItemID()) {
        copy.set(i, updated);
        submitList(copy);
        return;
      }
    }
  }

  /**
   * Highlights a single item by ID (applies visual stroke).
   *
   * @param itemId ID of item to highlight (-1 to clear highlight)
   */
  public void setHighlightedItemId(int itemId) {
    int old = highlightedItemId;
    highlightedItemId = itemId;

    // Update only the affected rows instead of refreshing everything
    int oldPos = findPositionByItemId(old);
    int newPos = findPositionByItemId(itemId);

    if (oldPos != RecyclerView.NO_POSITION) {
      notifyItemChanged(oldPos);
    }
    if (newPos != RecyclerView.NO_POSITION) {
      notifyItemChanged(newPos);
    }
  }
  //endregion

  /**
   * Finds the adapter position for a given item ID.
   *
   * @param itemId item ID
   * @return adapter position or {@link RecyclerView#NO_POSITION}
   */
  public int findPositionByItemId(int itemId) {
    if (itemId <= 0) {
      return RecyclerView.NO_POSITION;
    }

    List<KitItem> list = getCurrentList();
    for (int i = 0; i < list.size(); i++) {
      if (list.get(i).getItemID() == itemId) {
        return i;
      }
    }
    return RecyclerView.NO_POSITION;
  }
  //endregion

  //region Listener Interface
  public interface OnQuantityChangeListener {

    void onAdjustQuantity(int itemId, int delta);
  }

  //region ViewHolder
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

      // Click entire row to edit
      itemView.setOnClickListener(v -> {
        int position = getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
          return;
        }

        KitItem current = getItem(position);

        Intent intent = new Intent(context, KitItemEditActivity.class);
        intent.putExtra(KitItemEditActivity.EXTRA_KIT_ID, current.getKitID());
        intent.putExtra(KitItemEditActivity.EXTRA_ITEM_ID, current.getItemID());
        context.startActivity(intent);
      });

      // Decrement button
      btnDecrement.setOnClickListener(v -> {
        int position = getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
          return;
        }

        KitItem current = getItem(position);
        if (current.getQuantity() <= 0) {
          return;
        }

        disableButtons();
        quantityChangeListener.onAdjustQuantity(current.getItemID(), -1);
      });

      // Increment button
      btnIncrement.setOnClickListener(v -> {
        int position = getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
          return;
        }

        KitItem current = getItem(position);

        disableButtons();
        quantityChangeListener.onAdjustQuantity(current.getItemID(), +1);
      });
    }

    private void disableButtons() {
      btnDecrement.setEnabled(false);
      btnIncrement.setEnabled(false);
    }

    public void bind(KitItem item) {
      itemNameText.setText(item.getItemName());

      int qty = item.getQuantity();
      qtySummary.setText(context.getString(R.string.qty_prefix, qty));
      quantityText.setText(String.valueOf(qty));

      String exp = item.getExpirationDate();
      String expText = TextUtils.isEmpty(exp)
          ? context.getString(R.string.no_expiration)
          : context.getString(R.string.expires_format, exp.trim());

      itemMetaText.setText(expText);

      btnIncrement.setEnabled(true);
      btnDecrement.setEnabled(qty > 0);
    }

    public void applyHighlight(boolean highlight) {
      int targetWidth = highlight ? dpToPx(context, 2) : dpToPx(context, 1);
      int targetColor = highlight
          ? MaterialColors.getColor(card, androidx.appcompat.R.attr.colorPrimary)
          : MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutline);

      // Avoid unnecessary updates
      if (card.getStrokeWidth() == targetWidth
          && Objects.requireNonNull(card.getStrokeColorStateList()).getDefaultColor()
          == targetColor) {
        return;
      }

      card.setStrokeWidth(targetWidth);
      card.setStrokeColor(targetColor);
    }

    //region Helpers
    private int dpToPx(Context context, int dp) {
      return Math.round(dp * context.getResources().getDisplayMetrics().density);
    }
  }
  //endregion
}

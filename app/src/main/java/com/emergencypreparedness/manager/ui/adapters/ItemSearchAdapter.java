package com.emergencypreparedness.manager.ui.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.DiffUtil.ItemCallback;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.emergencypreparedness.manager.R;
import com.emergencypreparedness.manager.database.ItemSearchRow;
import com.emergencypreparedness.manager.ui.adapters.ItemSearchAdapter.VH;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * RecyclerView adapter for displaying global search results of kit items.
 * <p>
 * Uses {@link ListAdapter} + DiffUtil for efficient updates, instead of calling
 * {@code notifyDataSetChanged()} for all changes.
 */
public class ItemSearchAdapter extends ListAdapter<ItemSearchRow, VH> {

  //region DiffUtil
  private static final DiffUtil.ItemCallback<ItemSearchRow> DIFF_CALLBACK =
      new ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(
            @NonNull ItemSearchRow oldItem,
            @NonNull ItemSearchRow newItem
        ) {
          return oldItem.getItemID() == newItem.getItemID();
        }

        @Override
        public boolean areContentsTheSame(
            @NonNull ItemSearchRow oldItem,
            @NonNull ItemSearchRow newItem
        ) {
          return safe(oldItem.getItemName()).equals(safe(newItem.getItemName()))
              && safe(oldItem.getKitName()).equals(safe(newItem.getKitName()))
              && safe(oldItem.getCategoryName()).equals(safe(newItem.getCategoryName()))
              && oldItem.getQuantity() == newItem.getQuantity()
              && oldItem.getKitID() == newItem.getKitID();
        }
      };
  //region Fields
  private final LayoutInflater inflater;
  //endregion
  private final OnRowClickListener listener;
  //endregion

  //region Constructor

  /**
   * Creates a new adapter.
   *
   * @param context  context for inflating row views
   * @param listener click listener for rows (nullable)
   */
  public ItemSearchAdapter(@NonNull Context context, OnRowClickListener listener) {
    super(DIFF_CALLBACK);
    this.inflater = LayoutInflater.from(context);
    this.listener = listener;
  }
  //endregion

  //region Helpers
  private static String safe(String s) {
    return (s == null) ? "" : s.trim();
  }

  // region RecyclerView Overrides
  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = inflater.inflate(R.layout.row_item_search, parent, false);
    return new VH(v);
  }
  //endregion

  //region Data Management

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    ItemSearchRow row = getItem(position);

    holder.textItem.setText(safe(row.getItemName()));
    holder.textKit.setText(safe(row.getKitName()));
    holder.textCategory.setText(safe(row.getCategoryName()));
    holder.textQuantity.setText(String.valueOf(row.getQuantity()));

    holder.itemView.setOnClickListener(v -> {
      if (listener != null) {
        listener.onRowClick(row);
      }
    });
  }
  //endregion

  /**
   * Convenience wrapper to preserve existing calling style.
   * <p>
   * Internally uses {@link #submitList(List)} with a defensive copy so callers can safely reuse or
   * mutate their original list later.
   *
   * @param newItems new list of search rows (nullable)
   */
  public void setItems(List<ItemSearchRow> newItems) {
    if (newItems == null || newItems.isEmpty()) {
      submitList(Collections.emptyList());
    } else {
      submitList(new ArrayList<>(newItems));
    }
  }
  //endregion

  //region Listener Interface
  public interface OnRowClickListener {

    /**
     * Called when a search row is clicked.
     *
     * @param row clicked row
     */
    void onRowClick(ItemSearchRow row);
  }
  //endregion

  //region ViewHolder
  public static class VH extends RecyclerView.ViewHolder {

    //region Fields
    MaterialTextView textItem;
    MaterialTextView textKit;
    MaterialTextView textCategory;
    MaterialTextView textQuantity;
    //endregion

    //region Constructor
    VH(@NonNull View itemView) {
      super(itemView);

      textItem = itemView.findViewById(R.id.textItem);
      textKit = itemView.findViewById(R.id.textKit);
      textCategory = itemView.findViewById(R.id.textCategory);
      textQuantity = itemView.findViewById(R.id.textQuantity);
    }
    //endregion
  }
  //endregion
}

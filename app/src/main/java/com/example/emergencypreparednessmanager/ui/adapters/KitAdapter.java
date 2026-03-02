package com.example.emergencypreparednessmanager.ui.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.example.emergencypreparednessmanager.ui.activities.KitItemsActivity;
import com.google.android.material.textview.MaterialTextView;
import java.util.ArrayList;
import java.util.List;

/**
 * RecyclerView adapter for displaying a list of kits.
 * <p>
 * Uses ListAdapter + DiffUtil for efficient updates. Clicking a row opens the kit's item list
 * screen.
 */
public class KitAdapter extends ListAdapter<Kit, KitAdapter.KitViewHolder> {

  //region DiffUtil
  private static final DiffUtil.ItemCallback<Kit> DIFF_CALLBACK =
      new DiffUtil.ItemCallback<>() {
        @Override
        public boolean areItemsTheSame(@NonNull Kit oldItem, @NonNull Kit newItem) {
          return oldItem.getKitID() == newItem.getKitID();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Kit oldItem, @NonNull Kit newItem) {
          return safe(oldItem.getKitName()).equals(safe(newItem.getKitName()))
              && safe(oldItem.getLocation()).equals(safe(newItem.getLocation()))
              && safe(oldItem.getNotes()).equals(safe(newItem.getNotes()))
              && oldItem.isNotificationsEnabled() == newItem.isNotificationsEnabled()
              && safe(oldItem.getNotificationFrequency()).equals(
              safe(newItem.getNotificationFrequency()));
        }
      };
  //region Fields
  private final Context context;
  //endregion
  private final LayoutInflater inflater;
  //endregion

  //region Constructor
  public KitAdapter(@NonNull Context context) {
    super(DIFF_CALLBACK);
    this.context = context;
    this.inflater = LayoutInflater.from(context);
  }
  //endregion

  //region Helpers
  private static String safe(String s) {
    return (s == null) ? "" : s.trim();
  }

  //region RecyclerView Overrides
  @NonNull
  @Override
  public KitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View itemView = inflater.inflate(R.layout.kit_list_item, parent, false);
    return new KitViewHolder(itemView);
  }
  //endregion

  //region Data Management

  @Override
  public void onBindViewHolder(@NonNull KitViewHolder holder, int position) {
    holder.bind(getItem(position));
  }

  /**
   * Submits a new list of kits to the adapter.
   * <p>
   * Copies the input list to ensure the adapter always receives a new list instance, which prevents
   * subtle DiffUtil issues if the caller later mutates their list.
   *
   * @param kitList list of kits (nullable)
   */
  public void setKits(List<Kit> kitList) {
    List<Kit> safeCopy = (kitList == null) ? new ArrayList<>() : new ArrayList<>(kitList);
    submitList(safeCopy);
  }
  //endregion

  /**
   * Returns the kit currently displayed at adapter position.
   *
   * @param position adapter position
   * @return kit at position
   */
  public Kit getKitAt(int position) {
    return getItem(position);
  }
  //endregion

  //region ViewHolder
  public class KitViewHolder extends RecyclerView.ViewHolder {

    private final MaterialTextView kitNameText;
    private final MaterialTextView kitLocationText;

    public KitViewHolder(@NonNull View itemView) {
      super(itemView);

      kitNameText = itemView.findViewById(R.id.kitNameText);
      kitLocationText = itemView.findViewById(R.id.kitLocationText);

      itemView.setOnClickListener(view -> {
        int position = getBindingAdapterPosition();
        if (position == RecyclerView.NO_POSITION) {
          return;
        }

        Kit current = getItem(position);

        Intent intent = new Intent(context, KitItemsActivity.class);
        intent.putExtra(KitItemsActivity.EXTRA_KIT_ID, current.getKitID());
        intent.putExtra(KitItemsActivity.EXTRA_KIT_NAME, current.getKitName());
        context.startActivity(intent);
      });
    }

    public void bind(Kit kit) {
      kitNameText.setText(kit.getKitName());

      String location = kit.getLocation();
      if (location == null || location.trim().isEmpty()) {
        kitLocationText.setVisibility(View.GONE);
      } else {
        kitLocationText.setVisibility(View.VISIBLE);
        kitLocationText.setText(location.trim());
      }
    }
  }
  //endregion
}

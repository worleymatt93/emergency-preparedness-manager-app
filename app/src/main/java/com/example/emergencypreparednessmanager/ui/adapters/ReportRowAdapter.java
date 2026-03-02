package com.example.emergencypreparednessmanager.ui.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.ItemSearchRow;
import com.example.emergencypreparednessmanager.util.AppConstants;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * RecyclerView adapter for report rows (inventory or time-sensitive items).
 * <p>
 * Displays denormalized item data with conditional styling based on expiration status. Supports two
 * modes: full inventory or time-sensitive only.
 */
public class ReportRowAdapter extends RecyclerView.Adapter<ReportRowAdapter.VH> {

  //region Fields
  private final List<ItemSearchRow> rows;
  private final boolean timeSensitiveMode;
  private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);
  //endregion

  //region Constructor
  public ReportRowAdapter(List<ItemSearchRow> rows, boolean timeSensitiveMode) {
    this.rows = (rows == null) ? Collections.emptyList() : rows;
    this.timeSensitiveMode = timeSensitiveMode;
  }
  //endregion

  //region RecyclerView Overrides
  @NonNull
  @Override
  public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.row_report_item, parent, false);
    return new VH(v);
  }

  @Override
  public void onBindViewHolder(@NonNull VH holder, int position) {
    ItemSearchRow r = rows.get(position);

    String itemName = nullToEmpty(r.getItemName());
    String kitName = nullToEmpty(r.getKitName());
    String location = nullToEmpty(r.getLocation());
    String category = nullToEmpty(r.getCategoryName());
    int qty = r.getQuantity();
    String exp = nullToEmpty(r.getExpirationDate());

    String status = computeStatus(exp);

    holder.tvItemName.setText(itemName);
    holder.tvKit.setText(holder.itemView.getContext().getString(R.string.kit_prefix, kitName));

    if (TextUtils.isEmpty(location.trim())) {
      holder.tvLocation.setVisibility(View.GONE);
    } else {
      holder.tvLocation.setVisibility(View.VISIBLE);
      holder.tvLocation.setText(
          holder.itemView.getContext().getString(R.string.location_prefix, location.trim()));
    }

    holder.tvCategory.setText(
        holder.itemView.getContext().getString(R.string.category_prefix, category));
    holder.tvQty.setText(holder.itemView.getContext().getString(R.string.qty_prefix, qty));
    holder.tvStatus.setText(holder.itemView.getContext().getString(R.string.status_prefix, status));

    if (TextUtils.isEmpty(exp.trim())) {
      holder.tvExp.setVisibility(View.GONE);
    } else {
      holder.tvExp.setVisibility(View.VISIBLE);
      holder.tvExp.setText(holder.itemView.getContext().getString(R.string.exp_prefix, exp.trim()));
    }

    applyCardStyling(holder, status);
  }

  @Override
  public int getItemCount() {
    return rows.size();
  }
  //endregion

  //region Helpers
  private void applyCardStyling(VH holder, String status) {
    MaterialCardView card = holder.card;

    // Reset to defaults
    card.setStrokeWidth(holder.defaultStrokeWidth);
    card.setStrokeColor(holder.defaultStrokeColor);
    card.setCardBackgroundColor(holder.defaultCardBackground);

    if ("OK".equals(status)) {
      return;
    }

    boolean expired = "Expired".equals(status);
    boolean expiringSoon = "Expiring Soon".equals(status);

    if (!expired && !expiringSoon) {
      return;
    }

    int strokeColorAttr = expired
        ? androidx.appcompat.R.attr.colorError
        : com.google.android.material.R.attr.colorTertiary;

    int containerColorAttr = expired
        ? com.google.android.material.R.attr.colorErrorContainer
        : com.google.android.material.R.attr.colorTertiaryContainer;

    int strokeColor = MaterialColors.getColor(card, strokeColorAttr);
    int containerColor = MaterialColors.getColor(card, containerColorAttr);

    card.setStrokeWidth(Math.round(2 * card.getResources().getDisplayMetrics().density));
    card.setStrokeColor(strokeColor);

    // Apply background tint only in time-sensitive mode
    if (timeSensitiveMode) {
      card.setCardBackgroundColor(containerColor);
    }
  }
  //endregion

  private String computeStatus(String exp) {
    Date expDate = parseDate(exp);
    if (expDate == null) {
      return "OK";
    }

    Date today = startOfDay(new Date());
    Date in30 = addDays(today, AppConstants.DAYS_BEFORE_EXPIRATION_FOR_WARNING);

    if (expDate.before(today)) {
      return "Expired";
    }
    if (!expDate.after(in30)) {
      return "Expiring Soon";
    }
    return "OK";
  }

  private Date parseDate(String s) {
    if (TextUtils.isEmpty(s)) {
      return null;
    }
    try {
      return sdf.parse(s.trim());
    } catch (ParseException e) {
      return null;
    }
  }

  private Date startOfDay(Date d) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.set(Calendar.HOUR_OF_DAY, 0);
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);
    return cal.getTime();
  }

  private Date addDays(Date d, int days) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(d);
    cal.add(Calendar.DAY_OF_YEAR, days);
    return cal.getTime();
  }

  private String nullToEmpty(String s) {
    return s == null ? "" : s;
  }

  //region ViewHolder
  public static class VH extends RecyclerView.ViewHolder {

    final MaterialCardView card;

    final MaterialTextView tvItemName;
    final MaterialTextView tvKit;
    final MaterialTextView tvLocation;
    final MaterialTextView tvCategory;
    final MaterialTextView tvQty;
    final MaterialTextView tvStatus;
    final MaterialTextView tvExp;

    // Capture defaults once to restore "OK" items to normal appearance
    final int defaultStrokeWidth;
    final int defaultStrokeColor;
    final int defaultCardBackground;

    VH(@NonNull View itemView) {
      super(itemView);

      card = (MaterialCardView) itemView;

      tvItemName = itemView.findViewById(R.id.tvItemName);
      tvKit = itemView.findViewById(R.id.tvKit);
      tvLocation = itemView.findViewById(R.id.tvLocation);
      tvCategory = itemView.findViewById(R.id.tvCategory);
      tvQty = itemView.findViewById(R.id.tvQty);
      tvStatus = itemView.findViewById(R.id.tvStatus);
      tvExp = itemView.findViewById(R.id.tvExp);

      // Capture defaults to reset "OK" items to normal appearance
      defaultStrokeWidth = card.getStrokeWidth();
      defaultStrokeColor = Objects.requireNonNull(card.getStrokeColorStateList()).getDefaultColor();
      defaultCardBackground = card.getCardBackgroundColor().getDefaultColor();
    }
  }
  //endregion
}

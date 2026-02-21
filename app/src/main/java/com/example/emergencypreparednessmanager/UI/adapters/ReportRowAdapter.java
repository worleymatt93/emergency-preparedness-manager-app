package com.example.emergencypreparednessmanager.UI.adapters;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.emergencypreparednessmanager.R;
import com.example.emergencypreparednessmanager.database.ItemSearchRow;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.color.MaterialColors;
import com.google.android.material.textview.MaterialTextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportRowAdapter extends RecyclerView.Adapter<ReportRowAdapter.VH> {

    private final List<ItemSearchRow> rows;
    private final boolean timeSensitiveMode;
    private final SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy", Locale.US);

    public ReportRowAdapter(List<ItemSearchRow> rows, boolean timeSensitiveMode) {
        this.rows = rows;
        this.timeSensitiveMode = timeSensitiveMode;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.row_report_item, parent, false);
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

        holder.tvKit.setText("Kit: " + kitName);

        if (TextUtils.isEmpty(location.trim())) {
            holder.tvLocation.setVisibility(View.GONE);
        } else {
            holder.tvLocation.setVisibility(View.VISIBLE);
            holder.tvLocation.setText("Location: " + location.trim());
        }

        holder.tvCategory.setText("Category: " + category);
        holder.tvQty.setText("Qty: " + qty);

        holder.tvStatus.setText("Status: " + status);

        if (TextUtils.isEmpty(exp.trim())) {
            holder.tvExp.setVisibility(View.GONE);
        } else {
            holder.tvExp.setVisibility(View.VISIBLE);
            holder.tvExp.setText("Exp: " + exp.trim());
        }

        applyCardStyling(holder.card, status);
    }

    @Override
    public int getItemCount() {
        return rows == null ? 0 : rows.size();
    }

    public static class VH extends RecyclerView.ViewHolder {
        final MaterialCardView card;

        final MaterialTextView tvItemName;
        final MaterialTextView tvKit;
        final MaterialTextView tvLocation;
        final MaterialTextView tvCategory;
        final MaterialTextView tvQty;
        final MaterialTextView tvStatus;
        final MaterialTextView tvExp;

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

            // Capture defaults so ok items keep the same look
            defaultStrokeWidth = card.getStrokeWidth();
            defaultStrokeColor = card.getStrokeColor();
            defaultCardBackground = card.getCardBackgroundColor().getDefaultColor();
        }
    }

    private void applyCardStyling(MaterialCardView card, String status) {
        // Always reset to avoid bugs
        int outline = MaterialColors.getColor(card, com.google.android.material.R.attr.colorOutline);
        int surface = MaterialColors.getColor(card, com.google.android.material.R.attr.colorSurface);

        card.setStrokeWidth(Math.round(card.getResources().getDisplayMetrics().density));
        card.setStrokeColor(outline);
        card.setCardBackgroundColor(surface);


        if ("OK".equals(status)) return;

        boolean expired = "Expired".equals(status);
        boolean expiringSoon = "Expiring Soon".equals(status);
        if (!expired && !expiringSoon) return;

        int strokeColorAttr = expired
                ? androidx.appcompat.R.attr.colorError
                : com.google.android.material.R.attr.colorTertiary;

        int containerColorAttr = expired
                ? com.google.android.material.R.attr.colorErrorContainer
                : com.google.android.material.R.attr.colorTertiaryContainer;

        int strokeColor = MaterialColors.getColor(card, strokeColorAttr);
        int containerColor = MaterialColors.getColor(card, containerColorAttr);

        // Border for not OK
        card.setStrokeWidth(Math.round(2 * card.getResources().getDisplayMetrics().density));
        card.setStrokeColor(strokeColor);

        // Background only in Time-Sensitive Items section
        if (timeSensitiveMode) card.setCardBackgroundColor(containerColor);
    }

    private String computeStatus(String exp) {
        Date expDate = parse(exp);
        if (expDate == null) return "OK";

        Date today = startOfDay(new Date());
        Date in30 = addDays(today);

        if (expDate.before(today)) return "Expired";
        if (!expDate.after(in30)) return "Expiring Soon";
        return "OK";
    }

    private Date parse(String s) {
        if (TextUtils.isEmpty(s)) return null;
        try {
            return sdf.parse(s.trim());
        } catch (ParseException e) {
            return null;
        }
    }

    private Date startOfDay(Date d) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTime();
    }

    private Date addDays(Date d) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(d);
        calendar.add(Calendar.DAY_OF_YEAR, 30);
        return calendar.getTime();
    }

    private String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}

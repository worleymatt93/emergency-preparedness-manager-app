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
import com.example.emergencypreparednessmanager.UI.activities.KitDetailsActivity;
import com.example.emergencypreparednessmanager.entities.Kit;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for displaying a list of kits in a RecyclerView.
 * Handles item clicks to open KitDetailsActivity.
 */
public class KitAdapter extends RecyclerView.Adapter<KitAdapter.KitViewHolder> {

    // ------------------- FIELDS -------------------

    private final Context context;
    private final LayoutInflater inflater;
    private List<Kit> kits = new ArrayList<>();

    // ------------------- CONSTRUCTOR -------------------

    public KitAdapter(Context context) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
    }

    // ------------------- RECYCLER -------------------

    @NonNull
    @Override
    public KitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View itemView = inflater.inflate(R.layout.kit_list_item, parent, false);
        return new KitViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(@NonNull KitViewHolder holder, int position) {
        holder.bind(kits.get(position));
    }

    @Override
    public int getItemCount() {
        return kits.size();
    }

    // ------------------- DATA -------------------

    public List<Kit> getKits() {
        return kits;
    }

    public void setKits(List<Kit> kitList) {
        this.kits = kitList != null ? kitList : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ------------------- VIEW HOLDER -------------------

    public class KitViewHolder extends RecyclerView.ViewHolder {

        private final MaterialTextView kitNameText;
        private final MaterialTextView kitLocationText;

        public KitViewHolder(@NonNull View itemView) {
            super(itemView);

            kitNameText = itemView.findViewById(R.id.kitNameText);
            kitLocationText = itemView.findViewById(R.id.kitLocationText);

            itemView.setOnClickListener(view -> {
                int position = getBindingAdapterPosition();
                if (position == RecyclerView.NO_POSITION) return;

                Kit current = kits.get(position);

                Intent intent = new Intent(context, KitDetailsActivity.class);
                intent.putExtra("kitID", current.getKitID());
                context.startActivity(intent);
            });
        }

        public void bind(Kit kit) {

            // Primary title
            kitNameText.setText(kit.getKitName());

            // Secondary subtitle
            String location = kit.getLocation();

            if (TextUtils.isEmpty(location)) {
                kitLocationText.setVisibility(View.GONE);
            } else {
                kitLocationText.setVisibility(View.VISIBLE);
                kitLocationText.setText(location.trim());
            }
        }
    }
}

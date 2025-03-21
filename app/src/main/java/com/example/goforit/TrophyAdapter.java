package com.example.goforit;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

/**
 * Adaptateur pour afficher les trophées dans un RecyclerView
 */
public class TrophyAdapter extends RecyclerView.Adapter<TrophyAdapter.TrophyViewHolder> {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
    private final List<Trophy> trophies;

    public TrophyAdapter(List<Trophy> trophies) {
        this.trophies = trophies;
    }

    @NonNull
    @Override
    public TrophyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_trophy, parent, false);
        return new TrophyViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TrophyViewHolder holder, int position) {
        Trophy trophy = trophies.get(position);
        
        // Définir l'icône du trophée en fonction du type
        switch (trophy.getType()) {
            case GOLD:
                holder.trophyIcon.setImageResource(R.drawable.trophy_gold);
                break;
            case SILVER:
                holder.trophyIcon.setImageResource(R.drawable.trophy_silver);
                break;
            case BRONZE:
                holder.trophyIcon.setImageResource(R.drawable.trophy_bronze);
                break;
        }
        
        // Définir les textes
        holder.trophyLevel.setText(String.valueOf(trophy.getLevel()));
        holder.trophyTitle.setText(trophy.getTitle());
        holder.trophyDescription.setText(trophy.getDescription());
        holder.trophyDate.setText("Obtenu le " + DATE_FORMAT.format(trophy.getDateObtained()));
    }

    @Override
    public int getItemCount() {
        return trophies.size();
    }

    static class TrophyViewHolder extends RecyclerView.ViewHolder {
        ImageView trophyIcon;
        TextView trophyLevel;
        TextView trophyTitle;
        TextView trophyDescription;
        TextView trophyDate;

        public TrophyViewHolder(@NonNull View itemView) {
            super(itemView);
            trophyIcon = itemView.findViewById(R.id.trophyIcon);
            trophyLevel = itemView.findViewById(R.id.trophyLevel);
            trophyTitle = itemView.findViewById(R.id.trophyTitle);
            trophyDescription = itemView.findViewById(R.id.trophyDescription);
            trophyDate = itemView.findViewById(R.id.trophyDate);
        }
    }
} 
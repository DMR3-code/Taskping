package com.s23010301.taskping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Random;

public class PriorityTaskAdapter extends RecyclerView.Adapter<PriorityTaskAdapter.ViewHolder> {

    private final List<PriorityTask> taskList;
    private final int[] colors = {
            R.color.cardColor1,
            R.color.cardColor2,
            R.color.cardColor3,
            R.color.cardColor4,
            R.color.cardColor5
    };

    public PriorityTaskAdapter(List<PriorityTask> taskList) {
        this.taskList = taskList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title, progressText;
        ProgressBar progressBar;
        ImageView icon;
        CardView cardView;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.taskTitle);
            progressText = view.findViewById(R.id.taskProgressText);
            progressBar = view.findViewById(R.id.taskProgress);
            icon = view.findViewById(R.id.taskIcon);
            cardView = view.findViewById(R.id.cardContainer); // ✅ proper reference
        }
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_priority_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        PriorityTask task = taskList.get(position);
        holder.title.setText(task.getTitle());
        holder.progressBar.setProgress(task.getProgress());
        holder.progressText.setText(task.getProgress() + "%");
        holder.icon.setImageResource(task.getIconRes());

        // 🔁 Assign consistent random color based on position
        Random random = new Random(holder.getLayoutPosition());
        int colorResId = colors[random.nextInt(colors.length)];
        int color = ContextCompat.getColor(holder.itemView.getContext(), colorResId);

        holder.cardView.setCardBackgroundColor(color);
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
}

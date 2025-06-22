package com.s23010301.taskping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class DailyTaskAdapter extends RecyclerView.Adapter<DailyTaskAdapter.TaskViewHolder> {

    private final List<DailyTask> taskList;

    public DailyTaskAdapter(List<DailyTask> taskList) {
        this.taskList = taskList;
    }

    @NonNull
    @Override
    public TaskViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_daily_task, parent, false);
        return new TaskViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TaskViewHolder holder, int position) {
        DailyTask task = taskList.get(position);

        holder.taskTitle.setText(task.getTitle());

        // Optional: toggle visibility if task supports location
        if (task.hasLocation()) {
            holder.locationIcon.setVisibility(View.VISIBLE);
        } else {
            holder.locationIcon.setVisibility(View.GONE);
        }

        // Prevent checkbox recycling issues
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(task.isDone());

        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            task.setDone(isChecked);
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;
        CheckBox checkBox;
        ImageView locationIcon;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            checkBox = itemView.findViewById(R.id.checkboxComplete);
            locationIcon = itemView.findViewById(R.id.locationIcon);
        }
    }
}

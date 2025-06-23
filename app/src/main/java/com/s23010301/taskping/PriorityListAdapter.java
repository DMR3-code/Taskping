package com.s23010301.taskping;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class PriorityListAdapter extends RecyclerView.Adapter<PriorityListAdapter.ViewHolder> {

    private final List<PriorityTask> taskList;

    public PriorityListAdapter(List<PriorityTask> taskList) {
        this.taskList = taskList;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView taskTitle;
        CheckBox checkBox;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            checkBox = itemView.findViewById(R.id.checkboxComplete);
        }
    }

    @NonNull
    @Override
    public PriorityListAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_priority_list_task, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PriorityListAdapter.ViewHolder holder, int position) {
        PriorityTask task = taskList.get(position);
        holder.taskTitle.setText(task.getTitle());
        holder.checkBox.setChecked(false); // purely visual
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
}

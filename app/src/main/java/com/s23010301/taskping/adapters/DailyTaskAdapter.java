package com.s23010301.taskping.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.s23010301.taskping.R;
import com.s23010301.taskping.activities.TaskDetailsActivity;
import com.s23010301.taskping.helpers.LocalCacheHelper;
import com.s23010301.taskping.models.DailyTask;
import com.s23010301.taskping.helpers.FirestoreHelper;
import com.s23010301.taskping.models.TaskReminderReceiver;
import com.s23010301.taskping.models.TaskViewModel;

import java.util.List;

public class DailyTaskAdapter extends RecyclerView.Adapter<DailyTaskAdapter.TaskViewHolder> {
    private final Context context;
    private final List<DailyTask> taskList;
    private TaskViewModel viewModel;

    public void setViewModel(TaskViewModel viewModel) {
        this.viewModel = viewModel;
    }

    public DailyTaskAdapter(Context context, List<DailyTask> taskList) {
        this.context = context;
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
        holder.bind(task);
        holder.itemView.setOnLongClickListener(v -> {
            showDeleteDialog(task, position);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (viewModel != null) {
                viewModel.onTaskClicked(task.getId());
            }
        });

    }

    private void showDeleteDialog(DailyTask task, int position) {
        new android.app.AlertDialog.Builder(context)
                .setTitle("Delete Task")
                .setMessage("Are you sure you want to delete this task?")
                .setPositiveButton("Delete", (dialog, which) -> deleteTask(task, position))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteTask(DailyTask task, int position) {

        TaskReminderReceiver.cancelReminder(context, task.getId());

        FirestoreHelper.deleteTask(task.getId(), // ✅ Use the document ID
                unused -> {
                    // 1️⃣ Remove from RecyclerView list
                    taskList.remove(position);
                    notifyItemRemoved(position);

                    // 2️⃣ Remove from Room immediately
                    LocalCacheHelper cache = LocalCacheHelper.getInstance((FragmentActivity) context);
                    cache.deleteTaskById(task.getId());

                    // 3️⃣ Show toast
                    Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }



    @Override
    public int getItemCount() {
        return taskList.size();
    }

    static class TaskViewHolder extends RecyclerView.ViewHolder {
        private final TextView taskTitle;
        private final CheckBox checkBox;
        private final ImageView locationIcon;

        TaskViewHolder(@NonNull View itemView) {
            super(itemView);
            taskTitle = itemView.findViewById(R.id.taskTitle);
            checkBox = itemView.findViewById(R.id.checkboxComplete);
            locationIcon = itemView.findViewById(R.id.locationIcon);
        }

        void bind(DailyTask task) {
            taskTitle.setText(task.getTitle());
            locationIcon.setVisibility(task.hasLocation() ? View.VISIBLE : View.GONE);
            checkBox.setChecked(task.isDone());
        }
    }
}
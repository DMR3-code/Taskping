package com.s23010301.taskping.adapters;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import com.s23010301.taskping.R;
import com.s23010301.taskping.activities.TaskDetailsActivity;
import com.s23010301.taskping.helpers.LocalCacheHelper;
import com.s23010301.taskping.models.PriorityTask;
import com.s23010301.taskping.models.TaskReminderReceiver;
import com.s23010301.taskping.utils.ColorUtils;
import com.s23010301.taskping.helpers.FirestoreHelper;
import java.util.List;

public class PriorityTaskAdapter extends RecyclerView.Adapter<PriorityTaskAdapter.ViewHolder> {
    private final List<PriorityTask> taskList;
    private final Context context;
    private int lastColorIndex = -1;

    public PriorityTaskAdapter(Context context, List<PriorityTask> taskList) {
        this.context = context;
        this.taskList = taskList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView title;
        ImageView icon;
        CardView cardView;

        public ViewHolder(View view) {
            super(view);
            title = view.findViewById(R.id.taskTitle);
            icon = view.findViewById(R.id.taskIcon);
            cardView = view.findViewById(R.id.cardContainer);
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

        int color = ColorUtils.getStableColorAvoidRepeat(
                holder.itemView.getContext(),
                task.getId(), // stable per task
                position,
                R.array.task_colors,
                lastColorIndex
        );

        lastColorIndex = Math.abs(task.getId().hashCode()) % holder.itemView.getContext().getResources().getIntArray(R.array.task_colors).length;

        holder.cardView.setCardBackgroundColor(color);

        setupLongClickDelete(holder, task, position);

        holder.itemView.setOnClickListener(v -> {
            LocalCacheHelper cache = LocalCacheHelper.getInstance((FragmentActivity) context);
            cache.getTaskById(task.getId()).observe((FragmentActivity) context, fullTask -> {
                if (fullTask != null) {
                    Intent intent = new Intent(context, TaskDetailsActivity.class);
                    intent.putExtra("title", fullTask.getTitle());
                    intent.putExtra("description", fullTask.getDescription());
                    intent.putExtra("endDate", fullTask.getEndDate());
                    intent.putExtra("hasLocation", fullTask.hasLocation());
                    intent.putExtra("location", fullTask.getLocation());
                    context.startActivity(intent);
                } else {
                    Toast.makeText(context, "Task data not found", Toast.LENGTH_SHORT).show();
                }
            });
        });

    }

    private void setupLongClickDelete(ViewHolder holder, PriorityTask task, int position) {
        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Yes", (dialog, which) -> deleteTask(task, position))
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    private void deleteTask(PriorityTask task, int position) {

        TaskReminderReceiver.cancelReminder(context, task.getId());

        FirestoreHelper.deleteTask(task.getId(),
                unused -> {
                    // 1️⃣ Remove from RecyclerView list
                    taskList.remove(position);
                    notifyItemRemoved(position);

                    // 2️⃣ Remove from Room cache immediately
                    LocalCacheHelper cache = LocalCacheHelper.getInstance((FragmentActivity) context);
                    cache.deleteTaskById(task.getId()); // LiveData observer will auto-refresh fragments

                    // 3️⃣ Notify user
                    Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
                },
                e -> Toast.makeText(context, "Delete failed: " + e.getMessage(), Toast.LENGTH_SHORT).show()
        );
    }


    @Override
    public int getItemCount() {
        return taskList.size();
    }
}
package com.s23010301.taskping;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.List;
import java.util.Random;

public class PriorityTaskAdapter extends RecyclerView.Adapter<PriorityTaskAdapter.ViewHolder> {
    private final List<PriorityTask> taskList;
    private final Context context;
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    public PriorityTaskAdapter(Context context, List<PriorityTask> taskList) {
        this.context = context;
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
        holder.progressBar.setProgress(task.getProgress());
        holder.progressText.setText(task.getProgress() + "%");
        holder.icon.setImageResource(task.getIconRes());

        // Random card color
        Random random = new Random(holder.getLayoutPosition());
        int[] colors = {
                R.color.cardColor1, R.color.cardColor2,
                R.color.cardColor3, R.color.cardColor4, R.color.cardColor5
        };
        int colorResId = colors[random.nextInt(colors.length)];
        int color = ContextCompat.getColor(holder.itemView.getContext(), colorResId);
        holder.cardView.setCardBackgroundColor(color);


        holder.itemView.setOnLongClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Delete Task")
                    .setMessage("Are you sure you want to delete this task?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        String title = task.getTitle(); // assuming title is unique
                        db.collection("tasks")
                                .whereEqualTo("title", title)
                                .whereEqualTo("type", "priority")
                                .get()
                                .addOnSuccessListener(querySnapshot -> {
                                    for (QueryDocumentSnapshot doc : querySnapshot) {
                                        db.collection("tasks").document(doc.getId()).delete();
                                    }
                                    taskList.remove(holder.getAdapterPosition());
                                    notifyItemRemoved(holder.getAdapterPosition());
                                    Toast.makeText(context, "Task deleted", Toast.LENGTH_SHORT).show();
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(context, "Failed to delete: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                });
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return taskList.size();
    }
}

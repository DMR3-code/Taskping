package com.s23010301.taskping.db;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.s23010301.taskping.helpers.LocalCacheHelper;
import com.s23010301.taskping.models.Task;

import java.util.ArrayList;
import java.util.List;

public class TaskRepository {
    private final LocalCacheHelper cache;
    private final FirebaseFirestore db;

    public TaskRepository(FragmentActivity activity) {
        this.cache = LocalCacheHelper.getInstance(activity);
        this.db = FirebaseFirestore.getInstance();
    }

    public LiveData<List<Task>> getTasksByDate(String date) {
        return cache.getTasksByDate(date);
    }

    public void refreshTasksFromFirestore(String type, String date) {
        db.collection("tasks")
                .whereEqualTo("type", type)
                .whereEqualTo("date", date)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(query -> {
                    List<Task> freshTasks = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : query) {
                        freshTasks.add(new Task(
                                doc.getId(),
                                doc.getString("title"),
                                doc.getString("type"),
                                doc.getString("date"),
                                Boolean.TRUE.equals(doc.getBoolean("done")),
                                doc.getString("description"),
                                doc.getString("endDate"),
                                Boolean.TRUE.equals(doc.getBoolean("hasLocation")),
                                doc.getString("location")
                        ));
                    }
                    cache.insertOrUpdateTasks(freshTasks);
                });
    }
}

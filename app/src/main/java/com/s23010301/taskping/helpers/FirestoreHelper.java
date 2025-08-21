package com.s23010301.taskping.helpers;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Map;

public class FirestoreHelper {
    private static final FirebaseFirestore db = FirebaseFirestore.getInstance();

    // For saving tasks
    public static void saveTask(Map<String, Object> taskData,
                                OnSuccessListener<Void> onSuccess,
                                OnFailureListener onFailure) {
        String taskId = (String) taskData.get("id");
        if (taskId == null) {
            onFailure.onFailure(new Exception("Task ID is missing"));
            return;
        }

        db.collection("tasks").document(taskId)
                .set(taskData)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
    // For deleting tasks by ID
    public static void deleteTask(String taskId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection("tasks").document(taskId).delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
    public static void saveUser(String userId, Map<String, Object> userData,
                                OnSuccessListener<Void> onSuccess,
                                OnFailureListener onFailure) {
        db.collection("users").document(userId)
                .set(userData)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
    public static void updateTask(String taskId, Map<String, Object> updates,
                                  OnSuccessListener<Void> onSuccess,
                                  OnFailureListener onFailure) {
        db.collection("tasks").document(taskId)
                .update(updates)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }
}
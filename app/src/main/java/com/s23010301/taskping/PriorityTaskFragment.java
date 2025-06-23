package com.s23010301.taskping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class PriorityTaskFragment extends Fragment {
    private RecyclerView priorityRecyclerView;
    private PriorityListAdapter adapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_priority_task, container, false);
        priorityRecyclerView = view.findViewById(R.id.priorityRecyclerView);
        priorityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        fetchPriorityTasks();

        return view;
    }

    private void fetchPriorityTasks() {
        db.collection("tasks")
                .whereEqualTo("type", "priority")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<PriorityTask> priorityList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        priorityList.add(new PriorityTask(title, 0, 0, R.drawable.ic_location));
                    }
                    adapter = new PriorityListAdapter(priorityList);
                    priorityRecyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load priority tasks", Toast.LENGTH_SHORT).show());
    }
}

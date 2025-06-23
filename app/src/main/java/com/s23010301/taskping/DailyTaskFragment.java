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

public class DailyTaskFragment extends Fragment {
    private RecyclerView dailyRecyclerView;
    private DailyTaskAdapter adapter;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_task, container, false);
        dailyRecyclerView = view.findViewById(R.id.dailyRecyclerView);
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();
        fetchDailyTasks();
        return view;
    }

    private void fetchDailyTasks() {
        db.collection("tasks")
                .whereEqualTo("type", "daily")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<DailyTask> dailyList = new ArrayList<>();
                    for (QueryDocumentSnapshot doc : querySnapshot) {
                        String title = doc.getString("title");
                        boolean done = Boolean.TRUE.equals(doc.getBoolean("done"));
                        boolean hasLocation = doc.contains("location"); // optional
                        dailyList.add(new DailyTask(title, done, hasLocation));
                    }
                    adapter = new DailyTaskAdapter(requireContext(),dailyList);
                    dailyRecyclerView.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(getContext(), "Failed to load daily tasks Frag", Toast.LENGTH_SHORT).show());
    }

}
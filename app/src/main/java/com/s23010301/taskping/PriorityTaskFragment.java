package com.s23010301.taskping;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class PriorityTaskFragment extends Fragment {
    private RecyclerView recyclerView;
    private PriorityTaskAdapter adapter;
    private List<PriorityTask> taskList;

    public PriorityTaskFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_priority_task, container, false);
        recyclerView = view.findViewById(R.id.priorityTaskRecyclerView);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        taskList = new ArrayList<>();

        // Sample data
        taskList.add(new PriorityTask("Client Presentation", 40, 2, R.drawable.ic_location));
        taskList.add(new PriorityTask("App Redesign", 60, 7, R.drawable.ic_location));
        taskList.add(new PriorityTask("Prepare Report", 80, 1, R.drawable.ic_location));

        adapter = new PriorityTaskAdapter(taskList);
        recyclerView.setAdapter(adapter);

        return view;
    }

}
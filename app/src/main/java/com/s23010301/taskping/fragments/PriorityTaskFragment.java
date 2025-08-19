package com.s23010301.taskping.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.s23010301.taskping.R;
import com.s23010301.taskping.adapters.PriorityTaskAdapter;
import com.s23010301.taskping.db.TaskRepository;
import com.s23010301.taskping.models.PriorityTask;
import com.s23010301.taskping.models.Task;
import com.s23010301.taskping.models.DateViewModel;

import java.util.ArrayList;
import java.util.List;

public class PriorityTaskFragment extends Fragment {

    private final List<PriorityTask> priorityTasks = new ArrayList<>();
    private PriorityTaskAdapter adapter;
    private TaskRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_priority_task, container, false);

        RecyclerView priorityRecyclerView = view.findViewById(R.id.priorityRecyclerView);
        priorityRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new PriorityTaskAdapter(getContext(), priorityTasks);
        priorityRecyclerView.setAdapter(adapter);

        repository = new TaskRepository(requireActivity());

        DateViewModel dateViewModel = new ViewModelProvider(requireActivity()).get(DateViewModel.class);
        dateViewModel.getDate().observe(getViewLifecycleOwner(), this::loadTasksForDate);

        return view;
    }

    private void loadTasksForDate(String date) {
        if (date == null) return;

        repository.getTasksByDate(date).observe(getViewLifecycleOwner(), tasks -> {
            priorityTasks.clear();
            for (Task task : tasks) {
                if ("priority".equals(task.getType())) {
                    priorityTasks.add(new PriorityTask(task.getId(), task.getTitle(), 0));
                }
            }
            adapter.notifyDataSetChanged();
        });

        repository.refreshTasksFromFirestore("priority", date);
    }
}

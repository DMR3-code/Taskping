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
import com.s23010301.taskping.adapters.DailyTaskAdapter;
import com.s23010301.taskping.db.TaskRepository;
import com.s23010301.taskping.models.DailyTask;
import com.s23010301.taskping.models.Task;
import com.s23010301.taskping.models.DateViewModel;

import java.util.ArrayList;
import java.util.List;

public class DailyTaskFragment extends Fragment {

    private final List<DailyTask> dailyTasks = new ArrayList<>();
    private DailyTaskAdapter adapter;
    private TaskRepository repository;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_task, container, false);

        RecyclerView dailyRecyclerView = view.findViewById(R.id.dailyRecyclerView);
        dailyRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new DailyTaskAdapter(requireContext(), dailyTasks);
        dailyRecyclerView.setAdapter(adapter);

        repository = new TaskRepository(requireActivity());

        DateViewModel dateViewModel = new ViewModelProvider(requireActivity()).get(DateViewModel.class);
        dateViewModel.getDate().observe(getViewLifecycleOwner(), this::loadTasksForDate);

        return view;
    }

    private void loadTasksForDate(String date) {
        if (date == null) return;

        repository.getTasksByDate(date).observe(getViewLifecycleOwner(), tasks -> {
            dailyTasks.clear();
            for (Task task : tasks) {
                if ("daily".equals(task.getType())) {
                    dailyTasks.add(new DailyTask(task.getId(), task.getTitle(), task.isDone(), false));
                }
            }
            adapter.notifyDataSetChanged();
        });

        repository.refreshTasksFromFirestore("daily", date);
    }
}

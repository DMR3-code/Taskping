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

public class DailyTaskFragment extends Fragment {
    private RecyclerView dailyTaskRecyclerView;
    private DailyTaskAdapter dailyTaskAdapter;
    private List<DailyTask> dailyTaskList;

    public DailyTaskFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_daily_task, container, false);

        dailyTaskRecyclerView = view.findViewById(R.id.dailyTaskRecyclerView);
        dailyTaskRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        // Sample Data
        dailyTaskList = new ArrayList<>();
        dailyTaskList.add(new DailyTask("Work Out", false, false));
        dailyTaskList.add(new DailyTask("Daily Meeting", true, true));
        dailyTaskList.add(new DailyTask("Reading Book", false, false));
        dailyTaskList.add(new DailyTask("Learn Coding", false, false));
        dailyTaskList.add(new DailyTask("Sleep soon", false, false));

        dailyTaskAdapter = new DailyTaskAdapter(dailyTaskList);
        dailyTaskRecyclerView.setAdapter(dailyTaskAdapter);

        return view;
    }

}
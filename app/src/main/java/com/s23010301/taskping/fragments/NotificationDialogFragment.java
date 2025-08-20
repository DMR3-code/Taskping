package com.s23010301.taskping.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.s23010301.taskping.R;
import com.s23010301.taskping.adapters.NotificationAdapter;
import com.s23010301.taskping.db.NotificationRepository;
import com.s23010301.taskping.models.Notification;

public class NotificationDialogFragment extends DialogFragment {
    private NotificationRepository notificationRepository;
    private NotificationAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_notifications, container, false);

        notificationRepository = new NotificationRepository(requireContext());

        RecyclerView recyclerView = view.findViewById(R.id.notificationsRecyclerView);
        MaterialButton btnClearAll = view.findViewById(R.id.btnClearAll);
        MaterialButton btnMarkAllRead = view.findViewById(R.id.btnMarkAllRead);

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotificationAdapter();
        recyclerView.setAdapter(adapter);

        notificationRepository.getAllNotifications().observe(getViewLifecycleOwner(),
                notifications -> adapter.setNotifications(notifications));

        btnClearAll.setOnClickListener(v -> clearAllNotifications());
        btnMarkAllRead.setOnClickListener(v -> markAllAsRead());

        return view;
    }

    private void clearAllNotifications() {
        // Implement clear logic
    }

    private void markAllAsRead() {
        notificationRepository.markAllAsRead();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null) {
            getDialog().getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
            );
        }
    }
}
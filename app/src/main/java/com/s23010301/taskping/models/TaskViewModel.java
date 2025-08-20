package com.s23010301.taskping.models;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.s23010301.taskping.db.TaskRepository;
import com.s23010301.taskping.models.Event;

import java.util.List;

public class TaskViewModel extends AndroidViewModel {
    private final TaskRepository repository;

    // LiveData to trigger navigation
    private final MutableLiveData<Event<Task>> _navigateToTaskDetails = new MutableLiveData<>();
    public LiveData<Event<Task>> navigateToTaskDetails() {
        return _navigateToTaskDetails;
    }

    public TaskViewModel(@NonNull Application application) {
        super(application);
        repository = new TaskRepository(application);
    }

    public void onTaskClicked(String taskId) {
        // Fetch data on a background thread
        new Thread(() -> {
            Task task = repository.getTaskByIdSync(taskId); // Re-use the sync method
            if (task != null) {
                // Post the navigation event to the main thread
                _navigateToTaskDetails.postValue(new Event<>(task));
            }
        }).start();
    }

    public LiveData<List<Task>> getTasksByDate(String date) {
        return repository.getTasksByDate(date);
    }

    public void refreshTasksFromFirestore(String type, String date) {
        repository.refreshTasksFromFirestore(type, date);
    }
}
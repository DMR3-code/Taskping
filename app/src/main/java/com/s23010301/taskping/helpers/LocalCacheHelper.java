package com.s23010301.taskping.helpers;

import android.content.Context;

import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.room.Room;

import com.s23010301.taskping.db.AppDatabase;
import com.s23010301.taskping.models.Task;

import java.util.List;

public class LocalCacheHelper {
    private static LocalCacheHelper instance;
    private final AppDatabase db;

    private LocalCacheHelper(Context context) {
        db = Room.databaseBuilder(context, AppDatabase.class, "taskping-db")
                .fallbackToDestructiveMigration()
                .build();
    }

    public static synchronized LocalCacheHelper getInstance(Context context) {
        if (instance == null) {
            // This line is already correct, as it uses the application context
            instance = new LocalCacheHelper(context.getApplicationContext());
        }
        return instance;
    }

    public LiveData<List<Task>> getTasksByDate(String date) {
        return db.taskDao().getTasksByDate(date);
    }

    public void insertOrUpdateTasks(List<Task> tasks) {
        new Thread(() -> db.taskDao().insertAll(tasks)).start();
    }
    public void deleteTaskById(String id) {
        new Thread(() -> db.taskDao().deleteTaskById(id)).start();
    }
    public LiveData<Task> getTaskById(String id) {
        return db.taskDao().getTaskById(id);
    }
    public Task getTaskByIdSync(String id) {
        return db.taskDao().getTaskByIdSync(id);
    }

}

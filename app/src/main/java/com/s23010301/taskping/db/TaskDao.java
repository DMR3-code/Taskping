package com.s23010301.taskping.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.s23010301.taskping.models.Task;

import java.util.List;

@Dao
public interface TaskDao {

    @Query("SELECT * FROM tasks WHERE date = :date")
    LiveData<List<Task>> getTasksByDate(String date);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Task> tasks);

    @Query("DELETE FROM tasks WHERE id = :id")
    void deleteTaskById(String id);

    @Query("DELETE FROM tasks")
    void clearAll();

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    LiveData<Task> getTaskById(String id);

    @Query("SELECT * FROM tasks WHERE id = :id LIMIT 1")
    Task getTaskByIdSync(String id);


}

package com.s23010301.taskping.db;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.s23010301.taskping.models.Notification;
import com.s23010301.taskping.models.Task;

@Database(entities = {Task.class, Notification.class}, version = 3, exportSchema = false)
@TypeConverters({Converters.class})
public abstract class AppDatabase extends RoomDatabase {
    public abstract TaskDao taskDao();
    public abstract NotificationDao notificationDao();
    private static volatile AppDatabase INSTANCE;

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE tasks ADD COLUMN description TEXT");
            database.execSQL("ALTER TABLE tasks ADD COLUMN endDate TEXT");
            database.execSQL("ALTER TABLE tasks ADD COLUMN hasLocation INTEGER NOT NULL DEFAULT 0");
            database.execSQL("ALTER TABLE tasks ADD COLUMN location TEXT");
        }
    };
    public static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE notifications (" +
                    "id TEXT PRIMARY KEY NOT NULL, " +
                    "title TEXT, " +
                    "message TEXT, " +
                    "type TEXT, " +
                    "isRead INTEGER NOT NULL, " +
                    "timestamp INTEGER, " +
                    "taskId TEXT)");
        }
    };


    // ✅ Singleton instance initializer
    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "taskping-db")
                            .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}


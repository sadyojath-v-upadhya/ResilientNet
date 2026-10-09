package com.resilientnet.data.local.database;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.resilientnet.data.local.dao.UserDao;
import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.data.local.entity.User;
import com.resilientnet.data.local.dao.SosDao;
import com.resilientnet.data.local.entity.MessageEntity;
import com.resilientnet.data.local.dao.MessageDao;
@Database(
        entities = {
                User.class,
                SosEntity.class,
                MessageEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static final String DATABASE_NAME = "resilientnet_db";

    private static volatile AppDatabase INSTANCE;

    public abstract UserDao userDao();

    public abstract SosDao sosDao();
    public abstract MessageDao messageDao();
    public static AppDatabase getInstance(Context context) {

        if (INSTANCE == null) {

            synchronized (AppDatabase.class) {

                if (INSTANCE == null) {

                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    DATABASE_NAME
                            )
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }

        return INSTANCE;
    }
}
package com.resilientnet.data.repository;

import android.content.Context;

import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.User;

public class HomeRepository {

    private final AppDatabase database;

    public HomeRepository(Context context) {
        database = AppDatabase.getInstance(context);
    }

    public User getUserByEmail(String email) {
        return database.userDao().getUserByEmail(email);
    }
}
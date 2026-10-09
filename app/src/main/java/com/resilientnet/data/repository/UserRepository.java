package com.resilientnet.data.repository;

import android.content.Context;

import com.resilientnet.data.local.dao.UserDao;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.User;

public class UserRepository {

    private final UserDao userDao;

    public UserRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        userDao = database.userDao();
    }

    public boolean registerUser(User user) {
        return userDao.insertUser(user) > 0;
    }

    public User getUserByEmail(String email) {
        return userDao.getUserByEmail(email);
    }

    public boolean isEmailExists(String email) {
        return userDao.isEmailExists(email);
    }

    public boolean isPhoneExists(String phone) {
        return userDao.isPhoneExists(phone);
    }

    public boolean updateUser(User user) {
        return userDao.updateUser(user) > 0;
    }
}
package com.resilientnet.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.resilientnet.data.local.entity.User;
import androidx.room.Update;
@Dao
public interface UserDao {

    @Insert(onConflict = OnConflictStrategy.ABORT)
    long insertUser(User user);

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    User getUserByEmail(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE email = :email)")
    boolean isEmailExists(String email);

    @Query("SELECT EXISTS(SELECT 1 FROM users WHERE phone_number = :phone)")
    boolean isPhoneExists(String phone);

    @Update
    int updateUser(User user);
}
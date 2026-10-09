package com.resilientnet.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.resilientnet.data.local.entity.User;
import com.resilientnet.data.repository.UserRepository;

import java.util.concurrent.Executors;

public class ProfileViewModel extends AndroidViewModel {

    private final UserRepository repository;
    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
    }

    public LiveData<User> getUser() {
        return userLiveData;
    }

    public void loadUser(String email) {

        Executors.newSingleThreadExecutor().execute(() -> {

            User user = repository.getUserByEmail(email);

            userLiveData.postValue(user);

        });
    }
}
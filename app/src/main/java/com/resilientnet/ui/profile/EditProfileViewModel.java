package com.resilientnet.ui.profile;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.resilientnet.data.local.entity.User;
import com.resilientnet.data.repository.UserRepository;

public class EditProfileViewModel extends AndroidViewModel {

    private final UserRepository repository;

    private final MutableLiveData<User> userLiveData = new MutableLiveData<>();

    public EditProfileViewModel(@NonNull Application application) {
        super(application);
        repository = new UserRepository(application);
    }

    public LiveData<User> getUser() {
        return userLiveData;
    }

    public void loadUser(String email) {

        User user = repository.getUserByEmail(email);

        userLiveData.setValue(user);

    }

    public boolean updateUser(User user) {

        boolean updated = repository.updateUser(user);

        if (updated) {
            userLiveData.setValue(user);
        }

        return updated;
    }
}
package com.resilientnet.ui.auth.register.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.resilientnet.data.local.entity.User;
import com.resilientnet.data.repository.UserRepository;
import com.resilientnet.utils.security.PasswordUtils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RegisterViewModel extends AndroidViewModel {

    private final UserRepository repository;
    private final ExecutorService executorService;

    private final MutableLiveData<Boolean> registrationSuccess =
            new MutableLiveData<>();

    private final MutableLiveData<String> registrationError =
            new MutableLiveData<>();

    public RegisterViewModel(@NonNull Application application) {
        super(application);

        repository = new UserRepository(application);
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<Boolean> getRegistrationSuccess() {
        return registrationSuccess;
    }

    public LiveData<String> getRegistrationError() {
        return registrationError;
    }

    public void registerUser(
            String fullName,
            String email,
            String phone,
            String password,
            String emergencyContact
    ) {


        executorService.execute(() -> {

            if (repository.isEmailExists(email)) {
                registrationError.postValue("Email already registered.");
                return;
            }
            if (repository.isPhoneExists(phone)) {
                registrationError.postValue("Phone number is already registered.");
                return;
            }

            User user = new User(
                    fullName,
                    email,
                    phone,
                    PasswordUtils.hashPassword(password),
                    emergencyContact,
                    System.currentTimeMillis()
            );

            boolean success = repository.registerUser(user);

            if (success) {
                registrationSuccess.postValue(true);
            } else {
                registrationError.postValue("Unable to register user.");
            }

        });


    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executorService.shutdown();
    }
}
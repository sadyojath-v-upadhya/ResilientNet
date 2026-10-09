package com.resilientnet.ui.auth.login.viewmodel;

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

public class LoginViewModel extends AndroidViewModel {

    private final UserRepository repository;
    private final ExecutorService executor;

    private final MutableLiveData<User> loginSuccess = new MutableLiveData<>();
    private final MutableLiveData<String> loginError = new MutableLiveData<>();

    public LoginViewModel(@NonNull Application application) {
        super(application);

        repository = new UserRepository(application);
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<User> getLoginSuccess() {
        return loginSuccess;
    }

    public LiveData<String> getLoginError() {
        return loginError;
    }

    public void login(String email, String password) {

        executor.execute(() -> {

            User user = repository.getUserByEmail(email);

            if (user == null) {
                loginError.postValue("No account found with this email.");
                return;
            }

            boolean valid = PasswordUtils.verifyPassword(
                    password,
                    user.getPassword()
            );

            if (!valid) {
                loginError.postValue("Incorrect password.");
                return;
            }

            loginSuccess.postValue(user);

        });

    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
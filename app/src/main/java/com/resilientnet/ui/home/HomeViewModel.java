package com.resilientnet.ui.home;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;

import androidx.lifecycle.LiveData;

import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.dao.SosDao;
import com.resilientnet.data.local.entity.User;
import com.resilientnet.data.repository.HomeRepository;

import java.util.concurrent.Executors;

public class HomeViewModel extends AndroidViewModel {

    private final HomeRepository repository;
    private final LiveData<Integer> unsyncedSosCount;
    private final LiveData<Integer> syncedSosCount;

    private final MutableLiveData<User> userLiveData =
            new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        repository = new HomeRepository(application);

        SosDao sosDao = AppDatabase.getInstance(application).sosDao();
        unsyncedSosCount = sosDao.getUnsyncedCountLiveData();
        syncedSosCount = sosDao.getSyncedCountLiveData();
    }

    public LiveData<Integer> getUnsyncedSosCount() {
        return unsyncedSosCount;
    }

    public LiveData<Integer> getSyncedSosCount() {
        return syncedSosCount;
    }

    public MutableLiveData<User> getUser() {
        return userLiveData;
    }

    public void loadUser(String email) {

        Executors.newSingleThreadExecutor().execute(() -> {

            User user = repository.getUserByEmail(email);

            userLiveData.postValue(user);

        });

    }
}
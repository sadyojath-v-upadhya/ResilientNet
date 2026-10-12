package com.resilientnet.data.repository;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.resilientnet.data.local.dao.SosDao;
import com.resilientnet.data.local.database.AppDatabase;
import com.resilientnet.data.local.entity.SosEntity;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SosRepository {

    private final SosDao sosDao;
    private final LiveData<List<SosEntity>> allSosRequests;
    private final ExecutorService executorService;

    public SosRepository(Application application) {

        AppDatabase database = AppDatabase.getInstance(application);

        sosDao = database.sosDao();
        allSosRequests = sosDao.getAllSosRequests();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<List<SosEntity>> getAllSosRequests() {
        return allSosRequests;
    }

    public void insert(SosEntity sosEntity) {
        insert(sosEntity, null);
    }

    public void insert(SosEntity sosEntity, Runnable onInserted) {
        executorService.execute(() -> {
            sosDao.insertSos(sosEntity);
            if (onInserted != null) {
                onInserted.run();
            }
        });
    }

    public void update(SosEntity sosEntity) {
        executorService.execute(() -> sosDao.updateSos(sosEntity));
    }

    public void deleteAll() {
        executorService.execute(sosDao::deleteAll);
    }

    public void deleteById(int id) {
        executorService.execute(() -> sosDao.deleteById(id));
    }

    public LiveData<Integer> getUnsyncedCountLiveData() {
        return sosDao.getUnsyncedCountLiveData();
    }

    public LiveData<Integer> getSyncedCountLiveData() {
        return sosDao.getSyncedCountLiveData();
    }

    public LiveData<Integer> getTotalSosCountLiveData() {
        return sosDao.getTotalSosCountLiveData();
    }
}
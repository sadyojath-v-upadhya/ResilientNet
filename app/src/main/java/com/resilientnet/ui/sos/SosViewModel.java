package com.resilientnet.ui.sos;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.resilientnet.data.local.entity.SosEntity;
import com.resilientnet.data.repository.SosRepository;

import java.util.List;

public class SosViewModel extends AndroidViewModel {

    private final SosRepository repository;
    private final LiveData<List<SosEntity>> allSosRequests;

    public SosViewModel(@NonNull Application application) {
        super(application);

        repository = new SosRepository(application);
        allSosRequests = repository.getAllSosRequests();
    }

    public LiveData<List<SosEntity>> getAllSosRequests() {
        return allSosRequests;
    }

    public void insert(SosEntity sosEntity) {
        repository.insert(sosEntity);
    }

    public void update(SosEntity sosEntity) {
        repository.update(sosEntity);
    }

    public void deleteAll() {
        repository.deleteAll();
    }

    public void deleteById(int id) {
        repository.deleteById(id);
    }
}
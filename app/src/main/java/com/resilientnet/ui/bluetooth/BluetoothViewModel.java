package com.resilientnet.ui.bluetooth;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.resilientnet.bluetooth.BluetoothState;
import com.resilientnet.data.repository.BluetoothRepository;

public class BluetoothViewModel extends AndroidViewModel {

    private final BluetoothRepository repository;

    private final MutableLiveData<BluetoothState> bluetoothState =
            new MutableLiveData<>();

    public BluetoothViewModel(@NonNull Application application) {
        super(application);
        repository = new BluetoothRepository(application);
    }

    public LiveData<BluetoothState> getBluetoothState() {
        return bluetoothState;
    }

    public void checkBluetoothState() {
        bluetoothState.setValue(
                repository.getBluetoothState(getApplication())
        );
    }

    public void refreshBluetoothState() {

        bluetoothState.setValue(
                repository.getBluetoothState(getApplication())
        );

    }

    public BluetoothRepository getRepository() {
        return repository;
    }


}
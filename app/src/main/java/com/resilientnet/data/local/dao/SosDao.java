package com.resilientnet.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.resilientnet.data.local.entity.SosEntity;

import java.util.List;

@Dao
public interface SosDao {

    @Insert
    long insertSos(SosEntity sosEntity);

    @Update
    void updateSos(SosEntity sosEntity);

    @Query("SELECT * FROM sos_requests ORDER BY id DESC")
    LiveData<List<SosEntity>> getAllSosRequests();

    @Query("SELECT * FROM sos_requests WHERE latitude != 0.0 AND longitude != 0.0 ORDER BY id DESC")
    LiveData<List<SosEntity>> getSosWithLocation();

    @Query("SELECT * FROM sos_requests WHERE status = 'PENDING' ORDER BY id ASC")
    List<SosEntity> getPendingSosRequests();

    @Query("SELECT * FROM sos_requests WHERE id = :id LIMIT 1")
    SosEntity getSosById(int id);

    @Query("SELECT * FROM sos_requests WHERE is_synced = 0 ORDER BY id ASC")
    List<SosEntity> getUnsyncedSosRequests();

    @Query("SELECT COUNT(*) FROM sos_requests WHERE is_synced = 0")
    LiveData<Integer> getUnsyncedCountLiveData();

    @Query("SELECT COUNT(*) FROM sos_requests WHERE is_synced = 1")
    LiveData<Integer> getSyncedCountLiveData();

    @Query("SELECT COUNT(*) FROM sos_requests")
    LiveData<Integer> getTotalSosCountLiveData();

    @Query("DELETE FROM sos_requests")
    void deleteAll();

    @Query("DELETE FROM sos_requests WHERE id = :id")
    void deleteById(int id);
}
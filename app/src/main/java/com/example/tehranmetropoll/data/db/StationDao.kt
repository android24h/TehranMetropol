package com.example.tehranmetropoll.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.tehranmetropoll.data.model.StationDataModel

@Dao
interface StationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStation(station:List<StationDataModel>)

    @Query("SELECT * FROM stations")
    suspend fun getAllStation():List<StationDataModel>

    @Query("SELECT * FROM stations WHERE name LIKE :query")
    suspend fun searchStation(query: String):List<StationDataModel>

    @Query("SELECT * FROM stations WHERE id = :stationId LIMIT 1")
    suspend fun getSearchById(stationId:Int): StationDataModel
}
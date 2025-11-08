package com.example.tehranmetropoll.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.tehranmetropoll.data.model.Convertor
import com.example.tehranmetropoll.data.model.StationDataModel

@Database(entities = [StationDataModel::class], version = 3)
@TypeConverters(Convertor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun stationDao(): StationDao
}
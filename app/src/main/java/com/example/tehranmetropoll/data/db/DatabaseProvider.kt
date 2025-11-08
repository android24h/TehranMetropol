package com.example.tehranmetropoll.data.db

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase

object DatabaseProvider {
    private var INSTANCE : AppDatabase?=null

    fun getInstance(context: Context):AppDatabase{
        return INSTANCE ?: synchronized(this){
            val instance=Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "Tehran MetrooPool"
            )
                .fallbackToDestructiveMigration()

                .build()
            INSTANCE=instance
            instance

        }


    }


}
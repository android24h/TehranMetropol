package com.example.tehranmetropoll.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stations")
data class StationDataModel(
    @PrimaryKey
    val id: Int,
    val name: String,
    val nameFa:String="",
    val line:Int,
    val x: Float,
    val y: Float,
    val neighbors: List<Int>
)
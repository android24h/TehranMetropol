package com.example.tehranmetropoll.data.model

import androidx.room.TypeConverter

class Convertor {
    @TypeConverter
    fun fromList(value: List<Int>):
            String = value.joinToString(",")

    @TypeConverter
    fun toList(value: String):
            List<Int> = if (value.isEmpty()) emptyList() else value.split(",").map { it.toInt() }
}
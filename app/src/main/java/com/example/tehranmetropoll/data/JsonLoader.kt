package com.example.tehranmetropoll.data

import android.content.Context
import com.example.tehranmetropoll.data.model.StationDataModel
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

fun loadStationFromJson(context: Context): List<StationDataModel> {
    val josn = context.assets.open("stations.json").bufferedReader().use { it.readText() }
    val listType = object : TypeToken<List<StationDataModel>>() {}.type
    return Gson().fromJson(josn, listType)
}
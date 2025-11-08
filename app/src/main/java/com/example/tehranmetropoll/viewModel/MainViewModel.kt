package com.example.tehranmetropoll.viewModel

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.tehranmetropoll.data.db.DatabaseProvider
import com.example.tehranmetropoll.data.loadStationFromJson
import com.example.tehranmetropoll.data.model.StationDataModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * ViewModel اصلی برنامه مترو تهران
 * مدیریت جستجو، پیشنهاد، و مسیر‌یابی بین ایستگاه‌ها
 * نوشته‌شده برای رزومه و نمونه‌کار
 */
class MainViewModel(private val context: Context) : ViewModel() {

    // مسیر نهایی انتخاب‌شده بین دو ایستگاه
    var route by mutableStateOf<List<StationDataModel>>(emptyList())

    // اتصال به دیتابیس Room
    private val dao = DatabaseProvider.getInstance(context).stationDao()

    // متغیرهای مبدا و مقصد
    var origin by mutableStateOf("")
        private set
    var destination by mutableStateOf("")
        private set

    // لیست تمام ایستگاه‌ها (کش داخلی در حافظه)
    var allStation = mutableStateListOf<StationDataModel>()
        private set

    // پیشنهادهای autocomplete
    var originSuggestions by mutableStateOf<List<StationDataModel>>(emptyList())
        private set
    var destinationSuggestions by mutableStateOf<List<StationDataModel>>(emptyList())
        private set

    // کنترل debounce برای ورودی‌ها
    private var originDebounceJob: Job? = null
    private var destinationDebounceJob: Job? = null

    init {
        // بارگذاری اولیه ایستگاه‌ها از دیتابیس یا فایل JSON
        viewModelScope.launch(Dispatchers.IO) {
            val existing = dao.getAllStation()
            if (existing.isEmpty()) {
                val stations = loadStationFromJson(context)
                dao.insertStation(stations)
                Log.d("MetroDB", "Inserted ${stations.size} stations into DB")
                withContext(Dispatchers.Main) {
                    allStation.addAll(stations)
                }
            } else {
                Log.d("MetroDB", "Loaded ${existing.size} stations from DB")
                withContext(Dispatchers.Main) {
                    allStation.addAll(existing)
                }
            }
        }
    }

    // --- 🔍 جستجو و پیشنهاد مبدا ---
    fun updateOrigin(value: String, useDebounce: Boolean = true) {
        origin = value
        originDebounceJob?.cancel()

        if (value.isBlank()) {
            originSuggestions = emptyList()
            return
        }

        if (useDebounce) {
            originDebounceJob = viewModelScope.launch {
                delay(300)
                originSuggestions = allStation.filter {
                    it.name.contains(value, ignoreCase = true) ||
                            it.nameFa.contains(value)
                }
            }
        } else {
            originSuggestions = allStation.filter {
                it.name.contains(value, ignoreCase = true) ||
                        it.nameFa.contains(value)
            }
        }
    }

    fun selectOrigin(station: StationDataModel) {
        origin = station.nameFa.ifBlank { station.name }
        originSuggestions = emptyList()
    }

    // --- 🎯 جستجو و پیشنهاد مقصد ---
    fun updateDestination(value: String, useDebounce: Boolean = true) {
        destination = value
        destinationDebounceJob?.cancel()

        if (value.isBlank()) {
            destinationSuggestions = emptyList()
            return
        }

        if (useDebounce) {
            destinationDebounceJob = viewModelScope.launch {
                delay(300)
                destinationSuggestions = allStation.filter {
                    it.name.contains(value, ignoreCase = true) ||
                            it.nameFa.contains(value)
                }
            }
        } else {
            destinationSuggestions = allStation.filter {
                it.name.contains(value, ignoreCase = true) ||
                        it.nameFa.contains(value)
            }
        }
    }

    fun selectDestination(station: StationDataModel) {
        destination = station.nameFa.ifBlank { station.name }
        destinationSuggestions = emptyList()
    }

    // --- 🚇 پیدا کردن مسیر بین مبدا و مقصد ---
    fun findRoute() {
        viewModelScope.launch(Dispatchers.IO) {
            val allStation = dao.getAllStation()
            val start = allStation.find {
                it.name.contains(origin, ignoreCase = true) ||
                        it.nameFa.contains(origin)
            }
            val end = allStation.find {
                it.name.contains(destination, ignoreCase = true) ||
                        it.nameFa.contains(destination)
            }

            if (start != null && end != null) {
                val startIndex = allStation.indexOf(start)
                val endIndex = allStation.indexOf(end)
                val result = if (startIndex <= endIndex)
                    allStation.subList(startIndex, endIndex + 1)
                else
                    allStation.subList(endIndex, startIndex + 1).reversed()

                withContext(Dispatchers.Main) { route = result }
                Log.d("MetroRoute", "Found route with ${result.size} stations")
            } else {
                withContext(Dispatchers.Main) { route = emptyList() }
                Log.e("MetroRoute", "Invalid stations: start=$start, end=$end")
            }
        }
    }

    // --- 🔄 مسیر‌یابی با امکان تعویض خط ---
    fun findRouteWithTransfer() {
        viewModelScope.launch(Dispatchers.IO) {
            val stations = dao.getAllStation()
            val start = stations.find {
                it.name.contains(origin, ignoreCase = true) ||
                        it.nameFa.contains(origin)
            }
            val end = stations.find {
                it.name.contains(destination, ignoreCase = true) ||
                        it.nameFa.contains(destination)
            }

            if (start == null || end == null) {
                withContext(Dispatchers.Main) { route = emptyList() }
                return@launch
            }

            val queue = ArrayDeque<List<StationDataModel>>()
            val visited = mutableSetOf<Int>()
            queue.add(listOf(start))
            visited.add(start.id)

            var foundRoute: List<StationDataModel>? = null

            while (queue.isNotEmpty()) {
                val path = queue.removeFirst()
                val current = path.last()

                if (current.id == end.id) {
                    foundRoute = path
                    break
                }

                val directNeighbors = current.neighbors.mapNotNull { id ->
                    stations.find { it.id == id }
                }
                val transferStations = stations.filter {
                    it.name == current.name && it.line != current.line
                }
                val neighbors = directNeighbors + transferStations

                for (neighbor in neighbors) {
                    if (visited.add(neighbor.id)) {
                        queue.add(path + neighbor)
                    }
                }
            }

            withContext(Dispatchers.Main) {
                route = foundRoute ?: emptyList()
                Log.i("MetroRoute", "Route found with ${route.size} stations")
            }
        }
    }
}

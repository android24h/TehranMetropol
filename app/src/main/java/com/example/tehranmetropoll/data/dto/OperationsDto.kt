package com.example.tehranmetropoll.data.dto

/** Mirrors assets/metro_operations.json. Keys starting with "_" are documentation and ignored. */
data class OperationsDto(
    val version: Int = 1,
    val timeZone: String? = null,
    val serviceDayStartsAt: String? = null,
    val stationEntryMinutes: Double? = null,
    val lastTrainWarningMinutes: Int? = null,
    val holidays: HolidaysDto? = null,
    val lines: List<LineOperationsDto>? = null,
)

data class HolidaysDto(
    val verified: Boolean = false,
    val dates: List<String>? = null,
)

data class LineOperationsDto(
    val lineId: String,
    val directions: List<DirectionDto>? = null,
)

data class DirectionDto(
    val toward: String,
    val lastTrain: LastTrainDto? = null,
    val womenCars: WomenCarsDto? = null,
)

data class LastTrainDto(
    val verified: Boolean = false,
    val weekday: String? = null,
    val friday: String? = null,
    val holiday: String? = null,
)

data class WomenCarsDto(
    val verified: Boolean = false,
    val carsPerTrain: Int = 0,
    val womenOnlyCars: List<Int>? = null,
)

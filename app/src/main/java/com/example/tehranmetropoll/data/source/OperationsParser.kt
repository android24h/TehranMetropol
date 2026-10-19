package com.example.tehranmetropoll.data.source

import com.example.tehranmetropoll.data.dto.OperationsDto
import com.example.tehranmetropoll.domain.model.DirectionOperations
import com.example.tehranmetropoll.domain.model.LastTrainTimes
import com.example.tehranmetropoll.domain.model.MetroOperations
import com.example.tehranmetropoll.domain.model.WomenCars
import com.google.gson.Gson

/** Converts assets/metro_operations.json into domain objects. Kept Android-free for unit tests. */
object OperationsParser {

    fun parse(json: String): MetroOperations {
        val dto = Gson().fromJson(json, OperationsDto::class.java)
        val defaults = MetroOperations.EMPTY
        val directions = dto.lines.orEmpty().flatMap { line ->
            line.directions.orEmpty().map { dir ->
                DirectionOperations(
                    lineId = line.lineId,
                    towardStationId = dir.toward,
                    lastTrain = dir.lastTrain?.let {
                        LastTrainTimes(
                            weekday = parseTime(it.weekday),
                            friday = parseTime(it.friday),
                            holiday = parseTime(it.holiday),
                            verified = it.verified,
                        )
                    },
                    womenCars = dir.womenCars
                        ?.takeIf { it.carsPerTrain > 0 }
                        ?.let { cars ->
                            WomenCars(
                                carsPerTrain = cars.carsPerTrain,
                                womenOnlyCars = cars.womenOnlyCars.orEmpty().filter { it in 1..cars.carsPerTrain }.toSet(),
                                verified = cars.verified,
                            )
                        },
                )
            }
        }
        return MetroOperations(
            directions = directions,
            holidays = dto.holidays?.dates.orEmpty().map { it.trim() }.toSet(),
            holidaysVerified = dto.holidays?.verified ?: false,
            serviceDayStartMinute = parseTime(dto.serviceDayStartsAt) ?: defaults.serviceDayStartMinute,
            stationEntryMinutes = dto.stationEntryMinutes ?: defaults.stationEntryMinutes,
            warningMinutes = dto.lastTrainWarningMinutes ?: defaults.warningMinutes,
            timeZoneId = dto.timeZone ?: defaults.timeZoneId,
        )
    }

    /** "HH:mm" → minutes after midnight. Hours may exceed 23 for trains after midnight. */
    fun parseTime(text: String?): Int? {
        val parts = text?.trim()?.split(':') ?: return null
        if (parts.size != 2) return null
        val hours = parts[0].toIntOrNull() ?: return null
        val minutes = parts[1].toIntOrNull() ?: return null
        if (hours !in 0..30 || minutes !in 0..59) return null
        return hours * 60 + minutes
    }
}

package com.example.tehranmetropoll.data.dto

/** Mirrors assets/metro_network.json. */
data class MetroNetworkDto(
    val version: Int = 1,
    val defaultTransferWalkMinutes: Double = 3.0,
    val transferWalkMinutes: Map<String, Double> = emptyMap(),
    val lines: List<LineDto> = emptyList(),
    val stations: List<StationDto> = emptyList(),
)

data class LineDto(
    val id: String,
    val number: Int,
    val branch: Boolean = false,
    val nameEn: String,
    val nameFa: String,
    val color: String,
    val onColor: String,
    val headwayMinutes: Double,
    val speedKmh: Double,
    val stations: List<String>,
)

data class StationDto(
    val id: String,
    val en: String,
    val fa: String,
    val lat: Double,
    val lng: Double,
)

package com.example.tehranmetropoll.data.dto

/** Stored form of a favourite route (JSON inside DataStore). */
data class FavoriteDto(
    val id: String?,
    val label: String?,
    val originId: String?,
    val destinationId: String?,
    val preference: String?,
)

package ir.halvaei.metropol.data.dto

/** Subset of the Neshan Direction API (v4) response that the app reads. */
data class NeshanDirectionDto(val routes: List<NeshanRouteDto>?)

data class NeshanRouteDto(val legs: List<NeshanLegDto>?)

data class NeshanLegDto(val duration: NeshanValueDto?)

/** `value` is in seconds for durations. */
data class NeshanValueDto(val value: Double?)

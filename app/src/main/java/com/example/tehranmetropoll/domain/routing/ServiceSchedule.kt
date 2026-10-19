package com.example.tehranmetropoll.domain.routing

import com.example.tehranmetropoll.domain.model.ServiceStatus

/** Approximate Tehran Metro service pattern used to scale headways by time of day. */
object ServiceSchedule {
    private const val OPEN = 5 * 60 + 30
    private const val CLOSE = 22 * 60 + 30
    private val MORNING_PEAK = (7 * 60)..(9 * 60 + 30)
    private val EVENING_PEAK = (16 * 60)..(19 * 60 + 30)
    private val LATE = (21 * 60)..CLOSE

    fun status(minuteOfDay: Int): ServiceStatus = when {
        minuteOfDay < OPEN || minuteOfDay > CLOSE -> ServiceStatus.CLOSED
        minuteOfDay in MORNING_PEAK || minuteOfDay in EVENING_PEAK -> ServiceStatus.PEAK
        minuteOfDay in LATE -> ServiceStatus.LATE_NIGHT
        else -> ServiceStatus.NORMAL
    }

    fun headwayMultiplier(status: ServiceStatus): Double = when (status) {
        ServiceStatus.PEAK -> 0.75
        ServiceStatus.NORMAL -> 1.0
        ServiceStatus.LATE_NIGHT -> 1.5
        // When closed we estimate for the first trains of the morning.
        ServiceStatus.CLOSED -> 1.25
    }
}

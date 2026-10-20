package ir.halvaei.metropol.data.time

import ir.halvaei.metropol.domain.repository.Clock
import java.util.Calendar
import java.util.TimeZone

/** Wall clock in Tehran local time, whatever time zone the phone is set to. */
object TehranClock : Clock {
    private val zone: TimeZone = TimeZone.getTimeZone("Asia/Tehran")

    override fun minuteOfDay(): Int =
        Calendar.getInstance(zone).let { it.get(Calendar.HOUR_OF_DAY) * 60 + it.get(Calendar.MINUTE) }
}

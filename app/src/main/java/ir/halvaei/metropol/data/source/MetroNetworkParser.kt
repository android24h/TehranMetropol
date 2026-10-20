package ir.halvaei.metropol.data.source

import ir.halvaei.metropol.data.dto.MetroNetworkDto
import ir.halvaei.metropol.domain.model.LatLng
import ir.halvaei.metropol.domain.model.MetroLine
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.Station
import com.google.gson.Gson

/** Converts the raw JSON network into validated domain objects. Kept Android-free for unit tests. */
object MetroNetworkParser {

    fun parse(json: String): MetroNetwork {
        val dto = Gson().fromJson(json, MetroNetworkDto::class.java)
        val knownIds = dto.stations.map { it.id }.toSet()
        dto.lines.forEach { line ->
            val unknown = line.stations.filterNot(knownIds::contains)
            require(unknown.isEmpty()) { "Line ${line.id} references unknown stations: $unknown" }
        }

        val linesByStation = dto.lines.flatMap { line -> line.stations.map { it to line.number } }
            .groupBy({ it.first }, { it.second })

        val stations = dto.stations.map {
            Station(
                id = it.id,
                nameEn = it.en,
                nameFa = it.fa,
                location = LatLng(it.lat, it.lng),
                lineNumbers = linesByStation[it.id].orEmpty().distinct().sorted(),
            )
        }.filter { it.lineNumbers.isNotEmpty() }

        val lines = dto.lines.map {
            MetroLine(
                id = it.id,
                number = it.number,
                nameEn = it.nameEn,
                nameFa = it.nameFa,
                colorArgb = parseColor(it.color),
                onColorArgb = parseColor(it.onColor),
                isBranch = it.branch,
                headwayMinutes = it.headwayMinutes,
                speedKmh = it.speedKmh,
                stationIds = it.stations,
            )
        }
        return MetroNetwork(stations, lines, dto.transferWalkMinutes, dto.defaultTransferWalkMinutes)
    }

    private fun parseColor(hex: String): Long {
        val clean = hex.removePrefix("#")
        val argb = if (clean.length == 6) "FF$clean" else clean
        return argb.toLong(16)
    }
}

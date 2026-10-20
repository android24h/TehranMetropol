package ir.halvaei.metropol.domain.model

data class LatLng(val lat: Double, val lng: Double)

data class Station(
    val id: String,
    val nameEn: String,
    val nameFa: String,
    val location: LatLng,
    /** Distinct public line numbers serving this station, ascending. */
    val lineNumbers: List<Int>,
) {
    val isInterchange: Boolean get() = lineNumbers.size > 1

    fun name(persian: Boolean): String = if (persian) nameFa else nameEn
    fun alternateName(persian: Boolean): String = if (persian) nameEn else nameFa
}

/**
 * A single train service. Branches (e.g. the airport shuttle of Line 1) are modelled as their
 * own service that shares a public [number] and colour with the trunk line.
 */
data class MetroLine(
    val id: String,
    val number: Int,
    val nameEn: String,
    val nameFa: String,
    val colorArgb: Long,
    val onColorArgb: Long,
    val isBranch: Boolean,
    val headwayMinutes: Double,
    val speedKmh: Double,
    val stationIds: List<String>,
) {
    fun name(persian: Boolean): String = if (persian) nameFa else nameEn
    val firstStationId: String get() = stationIds.first()
    val lastStationId: String get() = stationIds.last()
}

class MetroNetwork(
    val stations: List<Station>,
    val lines: List<MetroLine>,
    private val transferWalkMinutes: Map<String, Double>,
    private val defaultTransferWalkMinutes: Double,
) {
    private val stationById: Map<String, Station> = stations.associateBy { it.id }
    private val lineById: Map<String, MetroLine> = lines.associateBy { it.id }
    private val linesByStation: Map<String, List<MetroLine>> =
        lines.flatMap { line -> line.stationIds.map { it to line } }
            .groupBy({ it.first }, { it.second })

    /** Public lines (trunks only) – what users think of as "the 7 lines". */
    val publicLines: List<MetroLine> get() = lines.filter { !it.isBranch }.sortedBy { it.number }

    fun station(id: String): Station = stationById.getValue(id)
    fun stationOrNull(id: String?): Station? = id?.let(stationById::get)
    fun line(id: String): MetroLine = lineById.getValue(id)
    fun linesAt(stationId: String): List<MetroLine> = linesByStation[stationId].orEmpty()

    fun transferWalkAt(stationId: String): Double =
        transferWalkMinutes[stationId] ?: defaultTransferWalkMinutes
}

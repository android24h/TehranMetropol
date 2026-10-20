package ir.halvaei.metropol.domain.routing

import ir.halvaei.metropol.domain.model.Station

/**
 * Forgiving bilingual station search: ignores case, spacing, punctuation, zero-width
 * non-joiners, Arabic vs Persian letter variants and Persian/Arabic digits.
 */
object StationMatcher {

    fun normalize(text: String): String {
        val sb = StringBuilder(text.length)
        for (raw in text.lowercase()) {
            val c = when (raw) {
                'ي', 'ى' -> 'ی'
                'ك' -> 'ک'
                'ة' -> 'ه'
                'أ', 'إ', 'آ' -> 'ا'
                'ؤ' -> 'و'
                in '۰'..'۹' -> '0' + (raw - '۰')
                in '٠'..'٩' -> '0' + (raw - '٠')
                else -> raw
            }
            if (c.isLetterOrDigit()) sb.append(c)
        }
        return sb.toString()
    }

    /** Lower is better; null means no match. */
    fun score(station: Station, query: String): Int? {
        val q = normalize(query)
        if (q.isEmpty()) return null
        return listOf(station.nameEn, station.nameFa, station.id).mapNotNull { name ->
            val n = normalize(name)
            val words = name.lowercase().split(' ', '-', '–', '(', ')').map(::normalize)
            when {
                n == q -> 0
                n.startsWith(q) -> 1
                words.any { it.startsWith(q) } -> 2
                n.contains(q) -> 3
                else -> null
            }
        }.minOrNull()
    }

    fun search(stations: List<Station>, query: String, limit: Int = 8): List<Station> =
        stations.mapNotNull { s -> score(s, query)?.let { s to it } }
            .sortedWith(compareBy({ it.second }, { it.first.nameEn.length }))
            .take(limit)
            .map { it.first }

    /** An exact (normalised) name match, used when the user typed but never picked a suggestion. */
    fun exact(stations: List<Station>, query: String): Station? =
        stations.firstOrNull { score(it, query) == 0 }
}

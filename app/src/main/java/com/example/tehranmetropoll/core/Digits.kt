package com.example.tehranmetropoll.core

/** Replaces ASCII digits with Persian (Extended Arabic-Indic) digits when [persian] is true. */
fun String.localizeDigits(persian: Boolean): String {
    if (!persian) return this
    val sb = StringBuilder(length)
    for (c in this) sb.append(if (c in '0'..'9') '۰' + (c - '0') else c)
    return sb.toString()
}

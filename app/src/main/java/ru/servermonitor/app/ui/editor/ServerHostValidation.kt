package ru.servermonitor.app.ui.editor

import android.net.InetAddresses
import java.net.IDN

internal fun isValidServerHost(value: String): Boolean {
    val host = value.trim()
    if (host.isEmpty() || host.any { it.isWhitespace() } || '[' in host || ']' in host) return false
    if (':' in host) return InetAddresses.isNumericAddress(host)

    val asciiHost = try {
        IDN.toASCII(host, IDN.USE_STD3_ASCII_RULES).removeSuffix(".")
    } catch (_: IllegalArgumentException) {
        return false
    }
    if (asciiHost.isEmpty() || asciiHost.length > 253) return false

    if ('.' in asciiHost && asciiHost.all { it in '0'..'9' || it == '.' }) {
        return InetAddresses.isNumericAddress(host)
    }
    return asciiHost.split('.').all { HOST_LABEL.matches(it) }
}

private val HOST_LABEL = Regex("[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?")

package ru.servermonitor.app.ui.navigation

import android.net.Uri
import ru.servermonitor.app.ui.components.HsvColor

internal object ServerDestinations {
    const val LIST = "servers"
    const val EDIT = "server/edit"
    const val SERVER_ID = "serverId"
    const val EDIT_PATTERN = "$EDIT?$SERVER_ID={$SERVER_ID}"
    private const val COLOR = "server/color-picker"
    const val HUE = "hue"
    const val SATURATION = "saturation"
    const val VALUE = "value"
    const val COLOR_PATTERN = "$COLOR?$HUE={$HUE}&$SATURATION={$SATURATION}&$VALUE={$VALUE}"
    const val COLOR_RESULT = "selected_color"

    fun edit(serverId: String): String = "$EDIT?$SERVER_ID=${Uri.encode(serverId)}"

    fun colorPicker(color: HsvColor): String =
        "$COLOR?$HUE=${color.hue}&$SATURATION=${color.saturation}&$VALUE=${color.value}"
}

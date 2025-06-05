package me.arcator.onfimLib.format


import java.util.Locale.getDefault
import me.arcator.onfimLib.utils.hostname

class ServerMessage(
    val text: String,
    val server: String = "Velocity${
        hostname.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                getDefault(),
            ) else it.toString()
        }
    }"
) :
    SerializedEvent(type = "Server Message")

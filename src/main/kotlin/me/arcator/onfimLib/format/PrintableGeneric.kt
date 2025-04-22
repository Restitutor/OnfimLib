package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import me.arcator.onfimLib.utils.hostname
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor

@JsonIgnoreProperties(ignoreUnknown = true)
sealed interface PrintableGeneric {
    // Use methods to avoid serializing json
    fun colour(): NamedTextColor

    fun printString(): String

    fun getComponent() = Component.text(printString(), colour())
}

class JoinQuit(
    // Pass to Generic
    name: String,
    type: String,
    server: String,
    platform: String = "In-Game",
    fromMC: Boolean = true,
    fromBot: Boolean = false,
    roomID: String = "#arcatorirc",
    isArcator: Boolean = true,
    nodeType: String = "BG",
    nodeHost: String = hostname,
    nodeName: String = nodeNameS,
    evtId: Int = randomEvtId(),
) :
    GenericChat(
        name,
        server,
        platform,
        fromMC,
        fromBot,
        roomID,
        isArcator,
        type,
        nodeType,
        nodeHost,
        nodeName,
        evtId,
    ),
    PrintableGeneric {

    override fun colour(): NamedTextColor =
        if (type == "Join") NamedTextColor.GREEN else NamedTextColor.RED

    override fun printString(): String {
        val vc = if (fromMC == true) "" else " [VC]"
        val verb = if (type == "Join") " joined " else " left "
        return name + vc + verb + server
    }
}

class Switch(
    // Unique
    val fromServer: String,

    // Pass to Generic
    name: String,
    server: String,
    platform: String = "In-Game",
    fromMC: Boolean = true,
    fromBot: Boolean = false,
    roomID: String = "#arcatorirc",
    isArcator: Boolean = true,
    nodeType: String = "BG",
    nodeHost: String = hostname,
    nodeName: String = nodeNameS,
    evtId: Int = randomEvtId(),
) :
    GenericChat(
        name,
        server,
        platform,
        fromMC,
        fromBot,
        roomID,
        isArcator,
        "Switch",
        nodeType,
        nodeHost,
        nodeName,
        evtId,
    ),
    PrintableGeneric {

    override fun colour(): NamedTextColor = NamedTextColor.YELLOW

    override fun printString(): String {
        val vc = if (fromMC == true) "" else " [VC]"
        return "$name$vc moved from $fromServer to $server"
    }
}

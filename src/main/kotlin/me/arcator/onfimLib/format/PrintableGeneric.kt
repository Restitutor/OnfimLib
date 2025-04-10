package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import me.arcator.onfimLib.utils.hostname
import net.kyori.adventure.text.format.NamedTextColor

sealed interface PrintableGeneric {
    val colour: NamedTextColor
    val printString: String
}

@JsonIgnoreProperties(ignoreUnknown = true)
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
        name, server, platform, fromMC, fromBot, roomID,
        isArcator, type, nodeType, nodeHost, nodeName, evtId,
    ), PrintableGeneric {
    override val colour: NamedTextColor =
        if (type == "Join") NamedTextColor.GREEN else NamedTextColor.RED
    private val vc = if (fromMC) "" else " [VC]"
    private val verb = if (type == "Join") " joined " else " left "
    override val printString = name + vc + verb + server
}

@JsonIgnoreProperties(ignoreUnknown = true)
class Switch(
    // Unique
    @Suppress("unused")
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
        name, server, platform, fromMC, fromBot, roomID,
        isArcator, "Switch", nodeType, nodeHost, nodeName, evtId,
    ), PrintableGeneric {
    override val colour: NamedTextColor = NamedTextColor.YELLOW
    private val vc = if (fromMC) "" else " [VC]"
    override val printString = "$name$vc moved from $fromServer to $server"
}

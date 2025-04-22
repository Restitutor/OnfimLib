package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import me.arcator.onfimLib.utils.hostname

@JsonIgnoreProperties(ignoreUnknown = true)
open class GenericChat(
    val name: String? = null,
    val server: String? = null,
    val platform: String = "In-Game",
    val fromMC: Boolean? = true,
    val fromBot: Boolean? = false,
    val roomID: String? = "#arcatorirc",
    val isArcator: Boolean? = true,
    type: String,
    nodeType: String = "BG",
    nodeHost: String = hostname,
    nodeName: String = nodeNameS,
    evtId: Int = randomEvtId(),
) : Generic(type, nodeType, nodeHost, nodeName, evtId) {

    @JsonIgnore open fun getHover() = if (platform == "In-Game") server else platform

    fun shouldRelay() = nodeNameS != nodeName
}

@Suppress("unused")
fun SJoin(name: String, server: String) = GenericChat(name, type = "SJoin", server = server)

@Suppress("unused")
fun SQuit(name: String, server: String) = GenericChat(name, type = "SQuit", server = server)

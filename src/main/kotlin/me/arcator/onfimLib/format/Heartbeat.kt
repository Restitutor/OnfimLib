package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import me.arcator.onfimLib.utils.hostname

@JsonIgnoreProperties(ignoreUnknown = true)
class Heartbeat(
    val udp: Int? = null,
    val sctp: Int? = null,

    nodeType: String = "BG",
    nodeHost: String = hostname,
    nodeName: String = nodeNameS,
    evtId: Int = randomEvtId(),
) :
    Generic(type = "Heartbeat", nodeType, nodeHost, nodeName, evtId)

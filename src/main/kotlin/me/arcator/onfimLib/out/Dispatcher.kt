package me.arcator.onfimLib.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import me.arcator.onfimLib.format.GenericChat
import me.arcator.onfimLib.format.Heartbeat
import me.arcator.onfimLib.utils.SELF_PORT
import org.msgpack.jackson.dataformat.MessagePackFactory

var dispatcher: Dispatcher? = null

class Dispatcher(private val logger: ((String) -> Unit)) {
    private val objectMapper = ObjectMapper(MessagePackFactory()).registerKotlinModule()
    private val uOut = SocketManager(UDPInterface(), logger)
    private val sOut = SocketManager(SCTPInterface(), logger)
    private val hm = HeartbeatManager(
        { h -> uOut.setMulticastHosts(h) },
        { h -> sOut.setMulticastHosts(h) },
    )

    init {
        dispatcher = this
    }

    fun broadcast(evt: GenericChat) {
        val bytes = objectMapper.writeValueAsBytes(evt)
        logger("[Onfim Send] ${evt.type}")
        uOut.multicast(bytes, "JS")
        sOut.multicast(bytes)
    }

    fun getHeartbeat(h: Heartbeat) {
        hm.uIn(h.udp, h.sctp, h.nodeHost, h.nodeType)
    }

    // Repeatable
    fun pingAll() {
        uOut.broadcast(
            objectMapper.writeValueAsBytes(Heartbeat(udp = SELF_PORT, sctp = SELF_PORT)),
        )
    }

    fun disable() {
        uOut.disable()
        sOut.disable()
    }
}

fun getHeartbeat(h: Heartbeat) {
    dispatcher?.getHeartbeat(h)
}

package me.arcator.onfimLib.out

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import me.arcator.onfimLib.format.GenericChat
import me.arcator.onfimLib.format.Heartbeat
import org.msgpack.jackson.dataformat.MessagePackFactory

class Dispatcher(
    private val logger: ((String) -> Unit),
    private val getUdpInPort: () -> Port,
    private val getSctpInPort: () -> Port,
) {
    private val objectMapper = ObjectMapper(MessagePackFactory()).registerKotlinModule()
    private val uOut = SocketManager(UDPOut(), logger)
    private val sOut = SocketManager(SCTPOut(), logger)
    private val hm = HeartbeatManager(uOut::setMulticastHosts, sOut::setMulticastHosts)

    fun broadcast(evt: GenericChat) {
        val bytes = objectMapper.writeValueAsBytes(evt)
        logger("[Onfim] Send ${evt.type}")
        uOut.multicast(bytes, "JS")
        sOut.multicast(bytes)
    }

    fun getHeartbeat(h: Heartbeat) {
        if (h.udp == null && h.sctp == null) return

        hm.addHeartbeat(h.nodeHost, h.nodeType, h.udp, h.sctp)
    }

    // Repeatable
    fun pingAll() {
        hm.associateHosts()
        val h = Heartbeat(udp = getUdpInPort(), sctp = getSctpInPort())
        if (h.udp != null || h.sctp != null) {
            uOut.broadcast(objectMapper.writeValueAsBytes(h))
        }
    }

    fun disable() {
        uOut.disable()
        sOut.disable()
    }
}

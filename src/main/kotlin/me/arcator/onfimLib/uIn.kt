package me.arcator.onfimLib

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import me.arcator.onfimLib.interfaces.ChatSenderInterface
import me.arcator.onfimLib.utils.SELF_PORT
import me.arcator.onfimLib.utils.Unpacker

@Suppress("unused")
class uIn(private val chatSender: ChatSenderInterface) : Runnable {
    private var active = true
    private val length = 4096
    private val ds = DatagramSocket(InetSocketAddress(SELF_PORT))
    override fun run() {
        while (active) {
            try {
                val buf = ByteArray(length)
                val packet = DatagramPacket(buf, buf.size)
                ds.receive(packet)
                Unpacker.read("UDP", chatSender, packet.data)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        println("Shutdown uIn")
        ds.close()
    }

    fun disable() {
        active = false
        ds.close()
    }
}

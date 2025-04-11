package me.arcator.onfimLib

import java.net.BindException
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

    private val ds = DatagramSocket(null)
    override fun run() {
        while (active) {
            try {
                ds.bind(InetSocketAddress(SELF_PORT))
            } catch (e: BindException) {
                Thread.sleep(30000)
                continue
            }
            break
        }

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
        ds.close()
        println("Shutdown uIn")
    }

    fun disable() {
        active = false
        ds.close()
    }
}

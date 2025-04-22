package me.arcator.onfimLib.out

import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress

internal class UDPOut : UnicastInterface {
    // Operate in blocking mode such that a socket only sends data one at a time
    private val socket = DatagramSocket()

    override val type = "UDP"

    override fun disable() = socket.close()

    override fun send(message: ByteArray, host: InetSocketAddress) {
        synchronized(socket) { socket.send(DatagramPacket(message, message.size, host)) }
    }
}

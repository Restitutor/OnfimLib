package me.arcator.onfimLib.out


import com.sun.nio.sctp.MessageInfo
import com.sun.nio.sctp.SctpMultiChannel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.UnresolvedAddressException

internal class SCTPInterface : UnicastInterface {
    private val socket = SctpMultiChannel.open()

    init {
        socket.configureBlocking(false)
    }

    override val type = "SCTP"

    override fun disable() = socket.close()

    override fun send(message: ByteArray, host: InetSocketAddress) {
        synchronized(socket) {
            try {
                socket.send(
                    ByteBuffer.wrap(message),
                    MessageInfo.createOutgoing(host, 0),
                )
            } catch (e: UnresolvedAddressException) {
                System.err.println("Could not find ${host.hostString}")
                e.printStackTrace()
            }
        }
    }
}


package me.arcator.onfimLib

import com.sun.nio.sctp.AbstractNotificationHandler
import com.sun.nio.sctp.SctpMultiChannel
import java.net.BindException
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.ClosedChannelException
import me.arcator.onfimLib.interfaces.ChatSenderInterface
import me.arcator.onfimLib.utils.SELF_PORT
import me.arcator.onfimLib.utils.Unpacker

@Suppress("unused")
class sIn(private val chatSender: ChatSenderInterface) : Runnable {
    private var active = true
    private val length = 30000
    private val assocHandler = object : AbstractNotificationHandler<java.io.PrintStream>() {}
    private val ds = SctpMultiChannel.open()

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

        var closedCount = 0
        while (active && closedCount < 10) {
            try {
                val buf = ByteBuffer.allocateDirect(length)
                val arr = ByteArray(length)
                ds.receive(buf, System.out, assocHandler)

                buf.rewind()
                buf.get(arr)  // .array doesn't work for ds
                Unpacker.read("SCTP", chatSender, arr)
            } catch (e: ClosedChannelException) {
                closedCount += 1
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        ds.close()
        println("Shutdown sIn")
    }

    fun disable() {
        active = false
    }
}

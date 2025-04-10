package me.arcator.onfimLib.out


import java.net.InetSocketAddress

internal sealed interface UnicastInterface {
    val type: String
    fun disable()
    fun send(message: ByteArray, host: InetSocketAddress)
}

package me.arcator.onfimLib.out

enum class Protocols { UDP, SCTP }

data class HeartbeatStore(
    private val udp: Int?,
    private val sctp: Int?,
    val nodeHost: String,
    val nodeType: String
) {

    private val lastPing: Int = nowSeconds()

    companion object {
        fun nowSeconds() = (System.currentTimeMillis() / 1000).toInt()
    }

    fun isMatch(hs: HeartbeatStore) = nodeHost == hs.nodeHost && nodeType == hs.nodeType
    fun isOld() = lastPing < nowSeconds() - 120 // Two minutes
    fun getPort(prop: Protocols): Int? {
        if (prop == Protocols.UDP) return udp
        if (prop == Protocols.SCTP) return sctp
        return 0
    }
}

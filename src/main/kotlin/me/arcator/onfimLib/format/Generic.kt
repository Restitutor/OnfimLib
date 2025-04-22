package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import me.arcator.onfimLib.utils.hostname
import me.arcator.onfimLib.utils.s_id

@JsonIgnoreProperties(ignoreUnknown = true)
open class Generic(
    val type: String,
    val nodeType: String = "BG",
    val nodeHost: String = hostname,
    val nodeName: String = nodeNameS,
    val evtId: Int = randomEvtId(),
) {
    companion object {
        // Cannot serialize static fields
        @JvmStatic val nodeNameS = "BG mcsa@$hostname"

        init {
            println("[Onfim] Set nodeHost as $hostname")
        }

        fun randomEvtId(): Int {
            // Random 7 digit code
            return (0..999999).random() * 10 + s_id - 1
        }
    }
}

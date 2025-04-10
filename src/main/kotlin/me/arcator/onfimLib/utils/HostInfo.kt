package me.arcator.onfimLib.utils

import java.net.InetAddress
import java.net.UnknownHostException


internal val rawHostname: String =
    try {
        InetAddress.getLocalHost().hostName
    } catch (e: UnknownHostException) {
        println("[Onfim] Hostname not found.")
        "styx"
    }

internal val hostname: String =
    when (rawHostname) {
        "pc00" -> "icarus"
        "pc01" -> "styx"
        else -> rawHostname
    }

internal val s_id: Int =
    when (hostname) {
        "jylina" -> 1
        "thoth" -> 2
        "icarus" -> 3
        "styx" -> 4
        "suse" -> 5
        "juno" -> 6
        "apollo" -> 7
        "sputnik" -> 9
        "vulcan" -> 10
        else -> {
            println("Unexpected hostname. $hostname")
            8
        }
    }

internal const val SELF_PORT = 2504

package me.arcator.onfimLib.utils

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.util.*
import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.Generic
import me.arcator.onfimLib.format.Heartbeat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.JoinQuit
import me.arcator.onfimLib.format.Switch
import me.arcator.onfimLib.interfaces.ChatSenderInterface
import me.arcator.onfimLib.out.getHeartbeat
import org.msgpack.jackson.dataformat.MessagePackFactory

object Unpacker {
    private val objectMapper = ObjectMapper(MessagePackFactory()).registerKotlinModule()
    private val seenUuids = LinkedList<Int>()

    // This method is designed for one thread
    fun read(protocol: String, chatSender: ChatSenderInterface, serialized: ByteArray) {
        val meta = objectMapper.readValue(serialized, Generic::class.java)
        val evtType = meta.type

        if (evtType != "Heartbeat") {
            // println("[Onfim Z] Received ${meta.type} at " + System.currentTimeMillis() + " from $protocol ${meta.nodeName}")
        }

        synchronized(this) {
            if (seenUuids.contains(meta.evtId)) return
            if (evtType == "Chat") {
                val evt: Chat = objectMapper.readValue(serialized, Chat::class.java)
                if (evt.shouldShow()) chatSender.say(evt)
            } else if (evtType in arrayOf("Join", "Quit")) {
                val evt: JoinQuit = objectMapper.readValue(serialized, JoinQuit::class.java)
                chatSender.say(evt)
            } else if (evtType == "Switch") {
                val evt: Switch = objectMapper.readValue(serialized, Switch::class.java)
                chatSender.say(evt)
            } else if (evtType == "Heartbeat") {
                getHeartbeat(
                    objectMapper.readValue(serialized, Heartbeat::class.java),
                )
            } else if (evtType == "Image") {
                chatSender.say(objectMapper.readValue(serialized, ImageEvt::class.java))
            }

            seenUuids.add(meta.evtId)
            while (seenUuids.size > 50) seenUuids.removeFirst()
        }
    }
}

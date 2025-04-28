package me.arcator.onfimLib.interfaces

import me.arcator.onfimLib.format.Chat
import me.arcator.onfimLib.format.ImageEvt
import me.arcator.onfimLib.format.PlayerMoveInterface

interface ChatSenderInterface {
    fun say(evt: Chat)

    fun say(evt: ImageEvt)

    fun say(evt: PlayerMoveInterface)
}

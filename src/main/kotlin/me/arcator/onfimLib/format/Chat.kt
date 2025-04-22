package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.vdurmont.emoji.EmojiParser
import de.themoep.minedown.adventure.MineDown
import java.util.*
import me.arcator.onfimLib.utils.hostname
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

val RES_ID: UUID = UUID.fromString("864cff60-0b54-4757-a1f3-f7d4828b7d29")

@JsonIgnoreProperties(ignoreUnknown = true)
class Chat(
    val plaintext: String,
    val rawtext: String = plaintext,
    val replyColour: String? = null,
    val replyUser: String? = null,
    val replyText: String? = null,
    val userColour: String? = null,
    uuid: UUID? = null,

    // Pass to Generic
    name: String,
    server: String,
    platform: String = "In-Game",
    fromMC: Boolean = true,
    fromBot: Boolean = false,
    roomID: String = "#arcatorirc",
    isArcator: Boolean = true,
    nodeType: String = "BG",
    nodeHost: String = hostname,
    nodeName: String = nodeNameS,
    evtId: Int = randomEvtId(),

    // Computed values
    val mentioned: Boolean = plaintext.lowercase().split(" ").any { it.endsWith("fim") },
    val person: String? = if (uuid == RES_ID) "RestitutorOrbis" else name,
) :
    GenericChat(
        name,
        server,
        platform,
        fromMC,
        fromBot,
        roomID,
        isArcator,
        "Chat",
        nodeType,
        nodeHost,
        nodeName,
        evtId,
    ) {

    @Suppress("unused")
    @JsonIgnore
    fun getMinecraftMessage(): String = "&f" + EmojiParser.parseToAliases(plaintext)

    @Suppress("unused")
    @JsonIgnore
    fun getChatMessage(): Component {
        val hover =
            if (replyUser == null) {
                Component.text(getHover()!!)
            } else {
                var comp = Component.text("${replyUser}\n")
                if (replyColour != null) {
                    comp = comp.color(TextColor.fromCSSHexString(replyColour!!))
                }

                comp
                    .append(Component.text(": ", NamedTextColor.WHITE))
                    .append(MineDown.parse(replyText!!))
            }

        var prefix =
            Component.text(name!!, getColour())
                .clickEvent(ClickEvent.openUrl("https://discord.gg/GwArgw2"))
                .hoverEvent(HoverEvent.showText(hover))

        if (replyUser != null) {
            prefix = prefix.append(Component.text(" ⏎").decoration(TextDecoration.BOLD, true))
        }

        return prefix
            .append(Component.text(": ", NamedTextColor.WHITE))
            .append(MineDown.parse(getMinecraftMessage()))
    }

    private fun inGame() =
        ((platform == "Discord" && roomID == "148831815984087041") ||
            (platform in setOf("IRC", "Onfim") && roomID == "#arcatorirc") ||
            (platform == "Matrix" && roomID == "!DNtAUptbNdsOOjGXVI:chat.arcator.co.uk") ||
            platform == "In-Game")

    fun shouldShow() = inGame()

    @Suppress("unused")
    @JsonIgnore
    private fun getColour(): TextColor {
        if ((userColour is String) && userColour.startsWith("#"))
            return TextColor.fromCSSHexString(userColour)!!
        return when (platform) {
            "In-Game" -> NamedTextColor.GOLD
            "IRC" -> if (fromMC == true) NamedTextColor.RED else NamedTextColor.DARK_RED
            "Onfim" -> NamedTextColor.YELLOW
            "Discord" ->
                if (fromBot == true) NamedTextColor.BLUE
                else TextColor.fromCSSHexString("#5865F2")!!
            "Matrix" -> NamedTextColor.LIGHT_PURPLE
            else -> {
                println("[Onfim Listen] Did not expect platform: $platform")
                NamedTextColor.DARK_RED
            }
        }
    }

    companion object {
        @Suppress("unused")
        fun fromMessage(rawMsg: String): String {
            if (rawMsg.startsWith("/")) {
                var matchedCmd = false
                val cmds = listOf("me", "eme", "broadcast", "bc", "say", "alert")
                for (cmd in cmds) {
                    val fullCmd = "/$cmd "
                    if (rawMsg.startsWith(fullCmd)) {
                        matchedCmd = true
                        break
                    }
                }

                // Ignore all other commands
                if (!matchedCmd) return ""
            }
            val msg =
                if (rawMsg.startsWith("/me ")) {
                    val suffix: String = rawMsg.split("/me ", ignoreCase = false, limit = 2).last()
                    "* $suffix"
                } else if (rawMsg.startsWith("/eme ")) {
                    val suffix: String = rawMsg.split("/eme ", ignoreCase = false, limit = 2).last()
                    "* $suffix"
                } else rawMsg
            return EmojiParser.parseToUnicode(msg)
        }
    }
}

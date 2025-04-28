package me.arcator.onfimLib.format

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.vdurmont.emoji.EmojiParser
import de.themoep.minedown.adventure.MineDown
import java.util.*
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.ClickEvent
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration

val RES_ID: UUID = UUID.fromString("864cff60-0b54-4757-a1f3-f7d4828b7d29")
val RELAY_CMDS = hashSetOf("me", "eme", "broadcast", "bc", "say", "alert")
// Main and testing channel
val DISCORD_CHANNELS = hashSetOf("148831815984087041", "1364820197801988099")

@JsonIgnoreProperties(ignoreUnknown = true)
class Chat(
    val plaintext: String,
    val user: ChatUser,
    val server: EventLocation,
    val rawtext: String = plaintext,
    val platform: String = "In-Game",
    val context: DiscordContext? = null,
    val mentioned: Boolean = plaintext.lowercase().split(" ").any { it.endsWith("fim") },
    val language: String? = null,
    val dm: Boolean = false,
    val mc: Boolean = true,
    val perms: Int = 0,
    val room: ChatRoom = ChatRoom(),
) : SerializedEvent(type = "Chat") {

    @Suppress("unused")
    @JsonIgnore
    fun getMinecraftMessage(): String = "&f" + EmojiParser.parseToAliases(plaintext)

    @Suppress("unused")
    @JsonIgnore
    fun getChatMessage(): Component {
        val hover =
            if (context?.replyUser == null) {
                Component.text(getHover())
            } else {
                var comp = Component.text("${context.replyUser}\n")
                if (context.replyColour != null) {
                    comp = comp.color(TextColor.fromCSSHexString(context.replyColour))
                }

                comp
                    .append(Component.text(": ", NamedTextColor.WHITE))
                    .append(MineDown.parse(context.replyText))
            }

        var prefix =
            Component.text(user.name, getColour())
                .clickEvent(ClickEvent.openUrl("https://discord.gg/GwArgw2"))
                .hoverEvent(HoverEvent.showText(hover))

        if (context?.replyUser != null) {
            prefix = prefix.append(Component.text(" ⏎").decoration(TextDecoration.BOLD, true))
        }

        return prefix
            .append(Component.text(": ", NamedTextColor.WHITE))
            .append(MineDown.parse(getMinecraftMessage()))
    }

    private fun inGame() =
        ((platform == "Discord" && room.id in DISCORD_CHANNELS) ||
            (platform in setOf("IRC", "Onfim") && room.id == "#arcatorirc") ||
            (platform == "Matrix" && room.id == "!DNtAUptbNdsOOjGXVI:chat.arcator.co.uk") ||
            platform == "In-Game")

    fun shouldShow() = inGame()

    @Suppress("unused")
    @JsonIgnore
    private fun getColour(): TextColor {
        val userColour = user.colour
        if ((userColour is String) && userColour.startsWith("#"))
            return TextColor.fromCSSHexString(userColour)!!

        return when (platform) {
            "In-Game" -> NamedTextColor.GOLD
            "IRC" -> if (mc) NamedTextColor.RED else NamedTextColor.DARK_RED
            "Onfim" -> NamedTextColor.YELLOW
            "Discord" ->
                if (user.bot) NamedTextColor.BLUE else TextColor.fromCSSHexString("#5865F2")!!
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

                for (cmd in RELAY_CMDS) {
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

    @JsonIgnore fun getHover() = if (platform == "In-Game") server.name else platform
}

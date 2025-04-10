"""Discord bridge implementation using pycord.
Handles message receiving from input channels and sending to output channel.
"""

import logging

import discord

import config
from message_buffer import AddMessageType

logger = logging.getLogger(__name__)


class DiscordBridge:
    """Discord bridge for relaying messages."""

    def __init__(
        self,
        add_message: AddMessageType,
    ) -> None:
        """Initialize the Discord bridge.

        Args:
            add_message: Callback when a new message is received

        """
        self.add_message = add_message
        self.bot: discord.Bot | None = None
        self._output_channel: discord.TextChannel | None = None
        self.emojis = {}

    async def start(self) -> None:
        """Start the Discord bot."""
        if (
            not config.DISCORD_TOKEN
            or config.DISCORD_TOKEN == "your_discord_bot_token_here"
        ):
            logger.warning("Discord token not configured, skipping Discord bridge")
            return

        intents = discord.Intents.default()
        intents.message_content = True

        self.bot = discord.Bot(
            allowed_mentions=discord.AllowedMentions(
                everyone=False,
                users=True,
                roles=False,
            ),
            intents=intents,
        )

        @self.bot.event
        async def on_ready() -> None:
            logger.info(f"Discord bot logged in as {self.bot.user}")
            await self._setup_output_channel()
            if self.bot is not None:
                self.emojis = await self.get_emojis(self.bot)

        @self.bot.event
        async def on_message(message: discord.Message) -> None:
            await self._handle_message(message)

        @self.bot.event
        async def on_message_edit(
            before: discord.Message, after: discord.Message
        ) -> None:
            # Resend edits as new messages
            await self._handle_message(after, is_edit=True)

        try:
            await self.bot.start(config.DISCORD_TOKEN)
        except Exception as e:
            logger.exception(f"Failed to start Discord bot: {e}")

    async def _setup_output_channel(self) -> None:
        """Setup the output channel for posting messages."""
        try:
            channel = self.bot.get_channel(int(config.DISCORD_OUTPUT_CHANNEL))
            if channel and isinstance(channel, discord.TextChannel):
                self._output_channel = channel
                logger.info(f"Discord output channel set: {channel.name}")
            else:
                logger.error(
                    f"Invalid Discord output channel: {config.DISCORD_OUTPUT_CHANNEL}",
                )
        except Exception as e:
            logger.exception(f"Failed to setup Discord output channel: {e}")

    async def _handle_message(
        self,
        message: discord.Message,
        is_edit: bool = False,
    ) -> None:
        """Handle incoming Discord messages."""
        # Ignore messages from the bot itself
        if message.author == self.bot.user:
            return

        # Only process messages from configured input channels
        if str(message.channel.id) not in config.DISCORD_INPUT_CHANNELS:
            return

        # Ignore attachments and embeds for now
        if not message.clean_content.strip():
            return

        content = message.clean_content

        # Add to buffer via rewrite content
        processed_content = self.rewrite_content(content)
        await self.add_message(
            message.author.display_name,
            "discord",
            processed_content,
        )

    def rewrite_content(self, message: str) -> str:
        """Rewrite message for formatting.

        To be modified for adding caching emojis.
        """
        return message

    @staticmethod
    async def get_emojis(bot: discord.Bot) -> dict[str, str]:
        # Fetch guild by ID
        try:
            guild = await bot.fetch_guild(config.DISCORD_GUILD)
        except discord.NotFound:
            logger.exception(f"Guild with ID {config.DISCORD_GUILD} not found.")
            return {}
        except discord.Forbidden:
            logger.exception(f"No access to guild with ID {config.DISCORD_GUILD}.")
            return {}

        # Get all emojis from the guild
        emojis = guild.emojis

        if not emojis:
            logger.warning(f"No custom emojis found in {guild.name}")
            return {}

        # Format emoji list
        mapping: dict[str, str] = {}
        for emoji in emojis:
            if emoji.is_usable():
                mapping[f":{emoji.name}:"] = str(emoji)

        return mapping

    async def post_message(self, sender: str, platform: str, content: str) -> bool:
        """Post a message to Discord output channel."""
        if not self._output_channel:
            logger.warning("Discord output channel not configured")
            return False

        # Rewrite content with emojis
        words = content.split(" ")
        rep = False
        for i, word in enumerate(words):
            if word in self.emojis:
                words[i] = self.emojis[word]
                rep = True

        if rep:
            content = " ".join(words)

        try:
            formatted_message = f"**[{platform}]** {sender}: {content}"
            await self._output_channel.send(formatted_message)
            return True
        except discord.HTTPException as e:
            logger.exception(f"Failed to send Discord message: {e}")
            return False
        except Exception as e:
            logger.exception(f"Unexpected error sending Discord message: {e}")
            return False

    async def stop(self) -> None:
        """Stop the Discord bot."""
        if self.bot:
            await self.bot.close()

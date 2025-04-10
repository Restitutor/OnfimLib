"""Main chat relay application.
Orchestrates all bridges and handles message relaying between platforms.
"""

import asyncio
import logging
import signal
import sys

import uvicorn

import config
from discord_bridge import DiscordBridge
from http_bridge import HTTPBridge
from matrix_bridge import MatrixBridge
from message_buffer import Message, message_buffer
from sctp_bridge import SCTPBridge

# Configure logging
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


def rewrite_content(content: str) -> str:
    return content


class ChatRelay:
    """Main chat relay orchestrator.
    Can be used as a library with add_message.
    """

    def __init__(self) -> None:
        """Initialize the chat relay."""
        self.discord_bridge = DiscordBridge(self.add_message)
        self.matrix_bridge = MatrixBridge(self.add_message)
        self.http_bridge = HTTPBridge(self.add_message)
        self.sctp_bridge = SCTPBridge(self.add_message)

    async def start(self) -> None:
        """Start all bridges and the relay system."""
        # Wait for all tasks
        # Create tasks for each component
        tasks = [asyncio.create_task(self.sctp_bridge.listen())]
        tasks.append(asyncio.create_task(self._start_discord()))
        tasks.append(asyncio.create_task(self._start_matrix()))
        tasks.append(asyncio.create_task(self._start_http()))

        logger.info("Starting Chat Relay System")
        try:
            await asyncio.gather(*tasks, return_exceptions=True)
        except Exception as e:
            logger.exception(f"Error in main tasks: {e}")
        finally:
            for t in tasks:
                t.cancel()
            await self.stop()

    async def _start_discord(self) -> None:
        """Start Discord bridge with error handling."""
        try:
            await self.discord_bridge.start()
        except Exception as e:
            logger.exception(f"Discord bridge failed: {e}")

    async def _start_matrix(self) -> None:
        """Start Matrix bridge with error handling."""
        try:
            await self.matrix_bridge.start()
        except Exception as e:
            logger.exception(f"Matrix bridge failed: {e}")

    async def _start_http(self) -> None:
        """Start API bridge with error handling."""
        try:
            # Run uvicorn server
            config_obj = uvicorn.Config(
                self.http_bridge.app,
                host=config.API_HOST,
                port=config.API_PORT,
                log_level="info",
            )
            server = uvicorn.Server(config_obj)
            await server.serve()
        except Exception as e:
            logger.exception(f"API bridge failed: {e}")

    async def add_message(self, sender: str, platform: str, content: str) -> None:
        """Main message relay loop. Runs when a new message is received."""
        logger.info("Running message relay loop")
        # Add to buffer
        msg = message_buffer.add_message(
            sender=sender,
            platform=platform,
            content=rewrite_content(content),
        )

        try:
            await self._relay_game_message(msg)
        except Exception as e:
            logger.exception(f"Error in message relay loop: {e}")

    async def _relay_game_message(self, message: Message) -> None:
        """Relay game messages to discord and matrix."""
        sender = message["sender"]
        platform = message["platform"]
        content = message["content"]

        if platform in {"discord", "matrix"}:
            return

        logger.info(f"Relaying message from {sender} ({platform}): {content}")

        # Relay API messages to Discord
        for bridge, label in (
            (self.discord_bridge, "Discord"),
            (self.matrix_bridge, "Matrix"),
        ):
            try:
                success = await bridge.post_message(
                    sender,
                    platform,
                    content,
                )
                if success:
                    logger.debug("Successfully relayed to %s", label)
                else:
                    logger.warning("Failed to relay message to %s", label)
            except Exception as e:
                logger.exception(f"Error relaying to %s: {e}", label)

    async def stop(self) -> None:
        """Stop all bridges and the relay system."""
        logger.info("Stopping Chat Relay System")

        # Stop all bridges
        for bridge in (self.discord_bridge, self.matrix_bridge, self.http_bridge):
            await bridge.stop()

        self.sctp_bridge.stop()
        logger.info("Chat Relay System stopped")


async def main() -> None:
    """Main entry point."""
    relay = ChatRelay()

    # Setup signal handlers for graceful shutdown
    def signal_handler() -> None:
        logger.info("Received shutdown signal")
        asyncio.create_task(relay.stop())

    # Register signal handlers
    for sig in [signal.SIGTERM, signal.SIGINT]:
        signal.signal(sig, lambda s, f: signal_handler())

    try:
        await relay.start()
    except KeyboardInterrupt:
        logger.info("Received keyboard interrupt")
    except Exception as e:
        logger.exception(f"Unexpected error: {e}")
    finally:
        await relay.stop()


if __name__ == "__main__":
    try:
        asyncio.run(main())
    except KeyboardInterrupt:
        logger.info("Application terminated by user")
    except Exception as e:
        logger.exception(f"Application crashed: {e}")
        sys.exit(1)

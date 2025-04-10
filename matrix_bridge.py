"""Matrix bridge implementation using matrix-nio.
Modern Matrix integration with support for posting and receiving messages.
"""

import asyncio
import contextlib
import logging
import time
from typing import Any

from nio import (
    AsyncClient,
    JoinError,
    MatrixRoom,
    RoomMessageText,
    RoomSendError,
)
from nio.responses import SyncResponse

import config
from message_buffer import AddMessageType

logger = logging.getLogger(__name__)


class MatrixBridge:
    """Matrix bridge for relaying messages using matrix-nio."""

    def __init__(self, add_message: AddMessageType) -> None:
        """Initialize the Matrix bridge.

        Args:
            add_message: Callback when a new message is received

        """
        self.add_message = add_message
        self._running = False
        self.client: AsyncClient | None = None
        self._sync_task: asyncio.Task | None = None
        self._joined_rooms: set[str] = set()
        self._startup_time = time.time()

    async def start(self) -> None:
        """Start the Matrix bridge."""
        if not config.MATRIX_TOKEN or config.MATRIX_TOKEN == "your_matrix_token_here":
            logger.warning("Matrix token not configured, skipping Matrix bridge")
            return

        if not config.MATRIX_USER_ID or not config.MATRIX_HOMESERVER:
            logger.warning("Matrix user ID or homeserver not configured")
            return

        logger.info("Starting Matrix bridge...")

        # Initialize the Matrix client
        self.client = AsyncClient(
            homeserver=config.MATRIX_HOMESERVER,
            user=config.MATRIX_USER_ID,
            device_id=config.MATRIX_DEVICE_ID,
            store_path=config.MATRIX_STORE_PATH,
        )

        # Set the access token directly
        self.client.access_token = config.MATRIX_TOKEN

        # Add event callbacks
        self.client.add_event_callback(self._handle_room_message, RoomMessageText)

        try:
            # Start syncing
            logger.info("Starting Matrix sync...")
            self._running = True

            # Join configured input channels
            await self._join_channels()

            # Start the sync loop in the background
            self._sync_task = asyncio.create_task(self._sync_loop())

            logger.info("Matrix bridge started successfully")

        except Exception as e:
            logger.exception(f"Failed to start Matrix bridge: {e}")
            await self.stop()
            raise

    async def _join_channels(self) -> None:
        """Join all configured input channels."""
        channels_to_join = set(config.MATRIX_INPUT_CHANNELS)

        # Also join output channel if not already in input channels
        if config.MATRIX_OUTPUT_CHANNEL not in channels_to_join:
            channels_to_join.add(config.MATRIX_OUTPUT_CHANNEL)

        for room_id in channels_to_join:
            try:
                response = await self.client.join(room_id)
                if isinstance(response, JoinError):
                    logger.warning(f"Failed to join room {room_id}: {response.message}")
                else:
                    self._joined_rooms.add(room_id)
                    logger.info(f"Joined Matrix room: {room_id}")
            except Exception as e:
                logger.exception(f"Error joining room {room_id}: {e}")

    async def _sync_loop(self) -> None:
        """Main sync loop for receiving Matrix events."""
        try:
            # Initial sync to get current state
            logger.info("Performing initial Matrix sync...")
            sync_response = await self.client.sync(timeout=10000, full_state=True)

            if not isinstance(sync_response, SyncResponse):
                logger.error(f"Initial sync failed: {sync_response}")
                return

            logger.info("Initial sync completed, starting continuous sync...")

            # Continuous sync loop
            while self._running:
                try:
                    sync_response = await self.client.sync(timeout=10000)

                    if not isinstance(sync_response, SyncResponse):
                        logger.warning(f"Sync error: {sync_response}")
                        await asyncio.sleep(5)
                        continue

                except asyncio.CancelledError:
                    logger.info("Matrix sync loop cancelled")
                    break
                except Exception as e:
                    logger.exception(f"Error in sync loop: {e}")
                    await asyncio.sleep(10)  # Wait before retrying

        except Exception as e:
            logger.exception(f"Fatal error in sync loop: {e}")
        finally:
            logger.info("Matrix sync loop ended")

    async def _handle_room_message(
        self, room: MatrixRoom, event: RoomMessageText,
    ) -> None:
        """Handle incoming Matrix room messages."""
        try:
            # Ignore prior messages
            if event.server_timestamp < (self._startup_time * 1000):
                return

            # Ignore messages from ourselves and matrix relay
            if event.sender == config.MATRIX_USER_ID or event.sender.startswith(
                "@_ooye_",
            ):
                return

            # Only process messages from input channels
            if room.room_id not in config.MATRIX_INPUT_CHANNELS:
                return

            # Extract sender display name or use user ID
            sender_name = room.user_name(event.sender) or event.sender

            # Clean up sender name (remove homeserver part if present)
            if sender_name.startswith("@") and ":" in sender_name:
                sender_name = sender_name.split(":")[0][
                    1:
                ]  # Remove @ and everything after :

            message_content = event.body.strip()

            if message_content:
                logger.info(f"Matrix message from {sender_name}: {message_content}")
                await self.add_message(sender_name, "matrix", message_content)

        except Exception as e:
            logger.exception(f"Error handling Matrix message: {e}")

    async def post_message(self, sender: str, platform: str, content: str) -> bool:
        """Post a message to Matrix output channel.

        Args:
            sender: The original sender of the message
            platform: The platform the message came from
            content: The message content

        Returns:
            True if message was sent successfully, False otherwise

        """
        if not self._running or not self.client:
            logger.warning("Matrix bridge not running")
            return False

        try:
            # Format the message to show origin
            formatted_message = f"**[{platform}]** {sender}: {content}"

            # Send to output channel
            response = await self.client.room_send(
                room_id=config.MATRIX_OUTPUT_CHANNEL,
                message_type="m.room.message",
                content={
                    "msgtype": "m.text",
                    "body": formatted_message,
                },
            )

            if isinstance(response, RoomSendError):
                logger.error(f"Failed to send Matrix message: {response.message}")
                return False

            logger.debug(f"Sent Matrix message: {formatted_message}")
            return True

        except Exception as e:
            logger.exception(f"Error sending Matrix message: {e}")
            return False

    async def stop(self) -> None:
        """Stop the Matrix bridge."""
        logger.info("Stopping Matrix bridge...")
        self._running = False

        # Cancel sync task
        if self._sync_task and not self._sync_task.done():
            self._sync_task.cancel()
            with contextlib.suppress(asyncio.CancelledError):
                await self._sync_task

        # Close the client
        if self.client:
            try:
                await self.client.close()
            except Exception as e:
                logger.warning(f"Error closing Matrix client: {e}")

        logger.info("Matrix bridge stopped")

    async def get_room_info(self) -> dict[str, Any]:
        """Get information about joined rooms (for debugging)."""
        if not self.client:
            return {}

        room_info = {}
        for room_id in self._joined_rooms:
            room = self.client.rooms.get(room_id)
            if room:
                room_info[room_id] = {
                    "display_name": room.display_name,
                    "member_count": room.member_count,
                    "encrypted": room.encrypted,
                }

        return room_info

    async def get_user_display_name(self, user_id: str) -> str:
        """Get the display name for a user ID."""
        if not self.client:
            return user_id

        try:
            response = await self.client.get_displayname(user_id)
            if hasattr(response, "displayname") and response.displayname:
                return response.displayname
        except Exception as e:
            logger.debug(f"Failed to get display name for {user_id}: {e}")

        # Fallback: extract username from user ID
        if user_id.startswith("@") and ":" in user_id:
            return user_id.split(":")[0][1:]
        return user_id

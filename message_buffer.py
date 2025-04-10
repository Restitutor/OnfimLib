"""Rolling message buffer implementation.
Maintains a FIFO buffer of the last N messages with timestamp filtering.
"""

import time
from collections import deque
from collections.abc import Callable
from types import CoroutineType
from typing import Any, TypedDict

import config

AddMessageType = Callable[[str, str, str], CoroutineType[Any, Any, None]]


class Message(TypedDict):
    """Represents a message in the buffer."""

    timestamp: float
    sender: str
    platform: str
    content: str


class MessageBuffer:
    """Thread-safe rolling buffer for messages."""

    def __init__(self, max_size: int = config.BUFFER_SIZE) -> None:
        self._buffer: deque[Message] = deque(maxlen=max_size)

    def add_message(self, sender: str, platform: str, content: str) -> Message:
        """Add a new message to the buffer."""
        message = Message(
            timestamp=time.time(),
            sender=sender,
            platform=platform,
            content=content,
        )
        self._buffer.append(message)

        return message

    def get_messages_since(self, timestamp: float) -> list[Message]:
        """Get all messages since the given timestamp."""
        return [msg for msg in self._buffer if msg["timestamp"] > timestamp]


# Global buffer instance
message_buffer = MessageBuffer()

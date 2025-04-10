"""FastAPI bridge implementation.
Provides HTTP endpoints for posting and retrieving messages.
Messages are only received by Mindustry.
"""

import logging
from collections.abc import Callable

from fastapi import FastAPI, HTTPException
from pydantic import BaseModel

import config
from message_buffer import AddMessageType, Message, message_buffer

logger = logging.getLogger(__name__)


class IncomingMessage(BaseModel):
    """Model for incoming HTTP messages."""

    user: str
    message: str
    server: str


class HTTPBridge:
    """FastAPI bridge for HTTP endpoints."""

    def __init__(
        self,
        add_message: AddMessageType,
        preprocess_function: Callable[[dict[str, str]], dict[str, str]] | None = None,
    ) -> None:
        """Initialize the HTTP bridge.

        Args:
            preprocess_function: Function to preprocess incoming messages

        """
        self.app = FastAPI(title="Chat Relay API")
        self.add_message = add_message
        self.preprocess_function = preprocess_function or (lambda x: x)
        self._setup_routes()

    def _setup_routes(self) -> None:
        """Setup FastAPI routes."""

        @self.app.post("/messages")
        async def post_message(message: IncomingMessage) -> dict[str, str]:
            """Endpoint to receive incoming messages via JSON POST."""
            try:
                # Preprocess the message
                preprocessed = self.preprocess_function(
                    {"user": message.user, "message": message.message},
                )

                # Extract user and message from preprocessed data
                user = preprocessed.get("user", message.user)
                content = preprocessed.get("message", message.message)
                server = message.server

                # Add to buffer
                await self.add_message(user, server, content)

                logger.info(f"Received HTTP message from {user}: {content}")
                return {"status": "success", "message": "Message received"}

            except Exception as e:
                logger.exception(f"Error processing HTTP message: {e}")
                raise HTTPException(status_code=500, detail="Internal server error")

        @self.app.get("/messages")
        async def get_messages(timestamp: float = 0.0) -> list[Message]:
            """Endpoint to fetch messages since a given timestamp."""
            # Common bug is user giving millisecond timestamps
            if timestamp > 1000000000000:
                logger.warning("Client passed millisecond timestamp!")
                timestamp /= 1000

            try:
                return message_buffer.get_messages_since(timestamp)
            except Exception as e:
                logger.exception(f"Error retrieving messages: {e}")
                raise HTTPException(status_code=500, detail="Internal server error")

    async def start(self) -> None:
        """Start the FastAPI server."""
        logger.info(f"Starting HTTP server on {config.API_HOST}:{config.API_PORT}")

    async def stop(self) -> None:
        """Stop the HTTP server."""
        logger.info("HTTP server stopped")

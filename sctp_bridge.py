"""SCTP API implementation using asyncio and pysctp.
Receives messages from SCTP socket and calls back with add_message.
"""

import asyncio
import json
import logging
import socket

import sctp

import config
from message_buffer import AddMessageType

logger = logging.getLogger(__name__)


class SCTPBridge:
    """SCTP bridge for receiving messages and forwarding them via callback."""

    def __init__(self, add_message: AddMessageType) -> None:
        """Initialize the SCTP bridge.

        Args:
            add_message: Callback when a new message is received

        """
        self.add_message = add_message
        # Create SCTP UDP-style socket
        self.sock = sctp.sctpsocket_udp(socket.AF_INET)
        self.sock.bind((config.API_HOST, config.API_PORT))
        self.sock.listen(10)
        self.sock.setblocking(False)

    async def listen(self) -> None:
        """Handle incoming SCTP messages."""
        logger.info(f"SCTP bridge listening on {config.API_HOST}:{config.API_PORT}")

        while True:
            try:
                many = self.sock.sctp_recv(5000)
                logger.info(str(many))

                _, _, data, _ = many
                recv = data.decode("utf-8")

                # Parse and process message
                d = json.loads(recv)
                logger.info(recv)
                await self.add_message(d["user"], d["platform"], d["message"])
            except BlockingIOError:
                await asyncio.sleep(0.1)

            except (ConnectionResetError, ConnectionAbortedError):
                # Network/socket errors - usually safe to continue
                logger.exception("SCTP Connection error.")
                continue

            except (UnicodeDecodeError, json.JSONDecodeError):
                # Data parsing errors - skip bad messages
                logger.exception("Data parsing error.")
                continue

            except (asyncio.CancelledError, OSError):
                # Proper asyncio cleanup
                break

            except KeyboardInterrupt:
                # User interruption
                break

    def stop(self) -> None:
        """Stop the SCTP bridge."""
        self.sock.close()

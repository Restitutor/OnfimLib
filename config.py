"""Configuration constants for the chat relay program.
Edit these values according to your setup.
"""

# Discord Configuration
DISCORD_GUILD = 0

DISCORD_TOKEN: str = ""
DISCORD_INPUT_CHANNELS: list[str] = [
    "",
    "",  # Channel snowflake IDs
]
DISCORD_OUTPUT_CHANNEL: str = ""

MATRIX_INPUT_CHANNELS: list[str] = [
    "",
    "",
    "",
]
MATRIX_OUTPUT_CHANNEL: str = ""

# Matrix homeserver URL
MATRIX_HOMESERVER = "http://matrix:8008"  # or your homeserver

# Full Matrix user ID (must include homeserver)
MATRIX_USER_ID = ""

# Access token (get this from Element -> Settings -> Help & About -> Advanced)
MATRIX_TOKEN = ""

# Device ID for this client instance
MATRIX_DEVICE_ID = ""

# API Configuration
API_HOST: str = "10.0.0.1"
API_PORT: int = 8000

# Buffer Configuration
BUFFER_SIZE: int = 10

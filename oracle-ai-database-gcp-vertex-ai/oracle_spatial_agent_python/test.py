import asyncio
import os
from pathlib import Path

from dotenv import load_dotenv
from a2a.client.client_factory import ClientFactory
from a2a.client.helpers import create_text_message_object

REPO_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(REPO_ROOT / ".env")

async def test_spatial_agent():
    url = os.environ.get("SPATIAL_AGENT_URL") or os.environ.get("A2A_URL") or (
        f"{os.environ.get('PUBLIC_PROTOCOL') or 'http'}://"
        f"{os.environ.get('PUBLIC_HOST') or 'localhost'}:"
        f"{os.environ.get('SPATIAL_AGENT_PORT') or os.environ.get('PORT') or '8080'}"
    )
    print(f"Connecting to {url}...")

    client = await ClientFactory.connect(url)

    card = await client.get_card()
    print(f"Connected to: {card.name}")
    print(f"Output modes: {card.default_output_modes}")

    print("\nSending map request...")
    message = create_text_message_object(
        content="Show me a map for warehouses WH-101 and WH-202"
    )

    async for response in client.send_message(message):
        print(f"Task state: {response.result.status.state}")
        for artifact in response.result.artifacts or []:
            print(f"Artifact: {artifact.name or artifact.artifact_id}")
            for part in artifact.parts:
                root = part.root
                if hasattr(root, "file"):
                    file_info = root.file
                    print(
                        "  file:",
                        getattr(file_info, "mime_type", None)
                        or getattr(file_info, "mimeType", None),
                        getattr(file_info, "name", None),
                    )
        status_message = response.result.status.message
        if status_message:
            for part in status_message.parts:
                root = part.root
                if hasattr(root, "text"):
                    print(f"Message: {root.text}")

if __name__ == "__main__":
    asyncio.run(test_spatial_agent())

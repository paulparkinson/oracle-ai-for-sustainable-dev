import base64
import binascii
import os
import re
import struct
import zlib
from pathlib import Path

import uvicorn
from a2a.server.agent_execution import AgentExecutor
from a2a.server.apps.jsonrpc.starlette_app import A2AStarletteApplication
from a2a.server.request_handlers.default_request_handler import DefaultRequestHandler
from a2a.server.tasks.inmemory_push_notification_config_store import (
    InMemoryPushNotificationConfigStore,
)
from a2a.server.tasks.inmemory_task_store import InMemoryTaskStore
from a2a.server.tasks.task_updater import TaskUpdater
from a2a.types import (
    AgentCapabilities,
    AgentCard,
    AgentSkill,
    FilePart,
    FileWithBytes,
    JSONRPCError,
    Part,
    TaskNotCancelableError,
    TextPart,
)
from dotenv import load_dotenv

REPO_ROOT = Path(__file__).resolve().parent.parent
load_dotenv(REPO_ROOT / ".env")

WAREHOUSE_PATTERN = re.compile(r"\bWH-\d+\b", re.IGNORECASE)

BIND_HOST = os.environ.get("BIND_HOST") or "0.0.0.0"
PUBLIC_HOST = os.environ.get("PUBLIC_HOST") or "localhost"
PUBLIC_PROTOCOL = os.environ.get("PUBLIC_PROTOCOL") or "http"
PORT = int(os.environ.get("SPATIAL_AGENT_PORT") or os.environ.get("PORT") or "8080")


FONT_5X7 = {
    " ": ["00000", "00000", "00000", "00000", "00000", "00000", "00000"],
    "-": ["00000", "00000", "00000", "11111", "00000", "00000", "00000"],
    ".": ["00000", "00000", "00000", "00000", "00000", "01100", "01100"],
    ",": ["00000", "00000", "00000", "00000", "00110", "00110", "01100"],
    ":": ["00000", "01100", "01100", "00000", "01100", "01100", "00000"],
    "/": ["00001", "00010", "00100", "01000", "10000", "00000", "00000"],
    "0": ["01110", "10001", "10011", "10101", "11001", "10001", "01110"],
    "1": ["00100", "01100", "00100", "00100", "00100", "00100", "01110"],
    "2": ["01110", "10001", "00001", "00010", "00100", "01000", "11111"],
    "3": ["11110", "00001", "00001", "01110", "00001", "00001", "11110"],
    "4": ["00010", "00110", "01010", "10010", "11111", "00010", "00010"],
    "5": ["11111", "10000", "10000", "11110", "00001", "00001", "11110"],
    "6": ["01110", "10000", "10000", "11110", "10001", "10001", "01110"],
    "7": ["11111", "00001", "00010", "00100", "01000", "01000", "01000"],
    "8": ["01110", "10001", "10001", "01110", "10001", "10001", "01110"],
    "9": ["01110", "10001", "10001", "01111", "00001", "00001", "01110"],
    "A": ["01110", "10001", "10001", "11111", "10001", "10001", "10001"],
    "B": ["11110", "10001", "10001", "11110", "10001", "10001", "11110"],
    "C": ["01111", "10000", "10000", "10000", "10000", "10000", "01111"],
    "D": ["11110", "10001", "10001", "10001", "10001", "10001", "11110"],
    "E": ["11111", "10000", "10000", "11110", "10000", "10000", "11111"],
    "F": ["11111", "10000", "10000", "11110", "10000", "10000", "10000"],
    "G": ["01111", "10000", "10000", "10111", "10001", "10001", "01110"],
    "H": ["10001", "10001", "10001", "11111", "10001", "10001", "10001"],
    "I": ["01110", "00100", "00100", "00100", "00100", "00100", "01110"],
    "J": ["00001", "00001", "00001", "00001", "10001", "10001", "01110"],
    "K": ["10001", "10010", "10100", "11000", "10100", "10010", "10001"],
    "L": ["10000", "10000", "10000", "10000", "10000", "10000", "11111"],
    "M": ["10001", "11011", "10101", "10101", "10001", "10001", "10001"],
    "N": ["10001", "11001", "10101", "10011", "10001", "10001", "10001"],
    "O": ["01110", "10001", "10001", "10001", "10001", "10001", "01110"],
    "P": ["11110", "10001", "10001", "11110", "10000", "10000", "10000"],
    "Q": ["01110", "10001", "10001", "10001", "10101", "10010", "01101"],
    "R": ["11110", "10001", "10001", "11110", "10100", "10010", "10001"],
    "S": ["01111", "10000", "10000", "01110", "00001", "00001", "11110"],
    "T": ["11111", "00100", "00100", "00100", "00100", "00100", "00100"],
    "U": ["10001", "10001", "10001", "10001", "10001", "10001", "01110"],
    "V": ["10001", "10001", "10001", "10001", "10001", "01010", "00100"],
    "W": ["10001", "10001", "10001", "10101", "10101", "10101", "01010"],
    "X": ["10001", "10001", "01010", "00100", "01010", "10001", "10001"],
    "Y": ["10001", "10001", "01010", "00100", "00100", "00100", "00100"],
    "Z": ["11111", "00001", "00010", "00100", "01000", "10000", "11111"],
}


class PixelCanvas:
    def __init__(self, width: int, height: int, background: tuple[int, int, int]):
        self.width = width
        self.height = height
        self.pixels = [list(background) for _ in range(width * height)]

    def set_pixel(self, x: int, y: int, color: tuple[int, int, int]) -> None:
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[(y * self.width) + x] = [color[0], color[1], color[2]]

    def fill_rect(self, x0: int, y0: int, x1: int, y1: int, color: tuple[int, int, int]) -> None:
        x0 = max(0, x0)
        y0 = max(0, y0)
        x1 = min(self.width, x1)
        y1 = min(self.height, y1)
        for y in range(y0, y1):
            row_offset = y * self.width
            for x in range(x0, x1):
                self.pixels[row_offset + x] = [color[0], color[1], color[2]]

    def draw_rect_outline(
        self,
        x0: int,
        y0: int,
        x1: int,
        y1: int,
        color: tuple[int, int, int],
        thickness: int = 1,
    ) -> None:
        for offset in range(thickness):
            self.fill_rect(x0 + offset, y0 + offset, x1 - offset, y0 + offset + 1, color)
            self.fill_rect(x0 + offset, y1 - offset - 1, x1 - offset, y1 - offset, color)
            self.fill_rect(x0 + offset, y0 + offset, x0 + offset + 1, y1 - offset, color)
            self.fill_rect(x1 - offset - 1, y0 + offset, x1 - offset, y1 - offset, color)

    def draw_line(
        self,
        x0: int,
        y0: int,
        x1: int,
        y1: int,
        color: tuple[int, int, int],
        thickness: int = 1,
    ) -> None:
        dx = abs(x1 - x0)
        sx = 1 if x0 < x1 else -1
        dy = -abs(y1 - y0)
        sy = 1 if y0 < y1 else -1
        err = dx + dy
        while True:
            half = thickness // 2
            self.fill_rect(x0 - half, y0 - half, x0 + half + 1, y0 + half + 1, color)
            if x0 == x1 and y0 == y1:
                break
            e2 = 2 * err
            if e2 >= dy:
                err += dy
                x0 += sx
            if e2 <= dx:
                err += dx
                y0 += sy

    def draw_circle(
        self,
        center_x: int,
        center_y: int,
        radius: int,
        color: tuple[int, int, int],
        fill: bool = True,
    ) -> None:
        radius_sq = radius * radius
        inner_sq = (radius - 2) * (radius - 2)
        for y in range(center_y - radius, center_y + radius + 1):
            for x in range(center_x - radius, center_x + radius + 1):
                dx = x - center_x
                dy = y - center_y
                distance_sq = (dx * dx) + (dy * dy)
                if fill:
                    if distance_sq <= radius_sq:
                        self.set_pixel(x, y, color)
                elif inner_sq <= distance_sq <= radius_sq:
                    self.set_pixel(x, y, color)

    def draw_text(
        self,
        x: int,
        y: int,
        text: str,
        color: tuple[int, int, int],
        scale: int = 2,
    ) -> None:
        cursor_x = x
        for character in text.upper():
            glyph = FONT_5X7.get(character, FONT_5X7[" "])
            for row_index, row in enumerate(glyph):
                for column_index, bit in enumerate(row):
                    if bit == "1":
                        self.fill_rect(
                            cursor_x + (column_index * scale),
                            y + (row_index * scale),
                            cursor_x + ((column_index + 1) * scale),
                            y + ((row_index + 1) * scale),
                            color,
                        )
            cursor_x += (6 * scale)

    def to_png_bytes(self) -> bytes:
        raw_rows = bytearray()
        for y in range(self.height):
            raw_rows.append(0)
            for x in range(self.width):
                raw_rows.extend(self.pixels[(y * self.width) + x])

        compressed = zlib.compress(bytes(raw_rows), level=9)
        png = bytearray(b"\x89PNG\r\n\x1a\n")
        png.extend(_png_chunk(
            b"IHDR",
            struct.pack("!2I5B", self.width, self.height, 8, 2, 0, 0, 0),
        ))
        png.extend(_png_chunk(b"IDAT", compressed))
        png.extend(_png_chunk(b"IEND", b""))
        return bytes(png)


def _png_chunk(chunk_type: bytes, data: bytes) -> bytes:
    chunk = bytearray(struct.pack("!I", len(data)))
    chunk.extend(chunk_type)
    chunk.extend(data)
    checksum = binascii.crc32(chunk_type + data) & 0xFFFFFFFF
    chunk.extend(struct.pack("!I", checksum))
    return bytes(chunk)


def extract_warehouse_ids(user_input: str) -> list[str]:
    warehouse_ids = [match.group(0).upper() for match in WAREHOUSE_PATTERN.finditer(user_input)]
    if not warehouse_ids:
        return ["WH-101", "WH-202"]

    # Preserve the first-seen order while removing duplicates.
    return list(dict.fromkeys(warehouse_ids))


def fetch_warehouse_map_data(warehouse_ids: list[str]) -> list[dict[str, object]]:
    """Mock spatial lookup; replace with Oracle Spatial queries in production."""
    seed_points = [
        (0.18, 0.28),
        (0.46, 0.22),
        (0.72, 0.34),
        (0.28, 0.58),
        (0.56, 0.54),
        (0.78, 0.68),
    ]
    warehouses = []
    for index, warehouse_id in enumerate(warehouse_ids):
        x_ratio, y_ratio = seed_points[index % len(seed_points)]
        offset = index // len(seed_points)
        warehouses.append(
            {
                "warehouse_id": warehouse_id,
                "x_ratio": min(x_ratio + (offset * 0.05), 0.88),
                "y_ratio": min(y_ratio + (offset * 0.05), 0.82),
            }
        )
    return warehouses


def render_warehouse_map_png(warehouse_ids: list[str]) -> str:
    width, height = 1100, 720
    canvas = PixelCanvas(width, height, (243, 239, 231))

    title_color = (249, 250, 251)
    subtitle_color = (203, 213, 225)
    ink_color = (15, 23, 42)
    muted_color = (71, 85, 105)
    teal = (15, 118, 110)
    orange = (194, 65, 12)

    canvas.fill_rect(0, 0, width, 130, (23, 50, 77))
    canvas.draw_text(48, 34, "ORACLE SPATIAL COVERAGE MAP", title_color, scale=4)
    canvas.draw_text(48, 88, "PNG ARTIFACT FOR GEMINI ENTERPRISE", subtitle_color, scale=2)

    map_bounds = (48, 160, 760, 660)
    canvas.fill_rect(map_bounds[0], map_bounds[1], map_bounds[2], map_bounds[3], (232, 239, 232))
    canvas.draw_rect_outline(map_bounds[0], map_bounds[1], map_bounds[2], map_bounds[3], (141, 162, 141), thickness=3)

    road_color = (197, 208, 199)
    for y in range(205, 625, 85):
        canvas.draw_line(88, y, 720, y + 18, road_color, thickness=5)
    for x in range(120, 705, 130):
        canvas.draw_line(x, 195, x - 24, 635, road_color, thickness=5)

    center_x = (map_bounds[0] + map_bounds[2]) / 2
    center_y = (map_bounds[1] + map_bounds[3]) / 2

    warehouses = fetch_warehouse_map_data(warehouse_ids)
    for warehouse in warehouses:
        x = int(map_bounds[0] + warehouse["x_ratio"] * (map_bounds[2] - map_bounds[0]))
        y = int(map_bounds[1] + warehouse["y_ratio"] * (map_bounds[3] - map_bounds[1]))

        canvas.draw_line(int(center_x), int(center_y), x, y, orange, thickness=4)
        canvas.draw_circle(x, y, 16, teal, fill=True)
        canvas.draw_circle(x, y, 18, title_color, fill=False)

        label_box = (x + 24, y - 22, x + 206, y + 26)
        canvas.fill_rect(label_box[0], label_box[1], label_box[2], label_box[3], (255, 255, 255))
        canvas.draw_rect_outline(label_box[0], label_box[1], label_box[2], label_box[3], (138, 160, 175), thickness=2)
        canvas.draw_text(label_box[0] + 14, label_box[1] + 12, warehouse["warehouse_id"], ink_color, scale=2)

    canvas.draw_circle(int(center_x), int(center_y), 20, (220, 38, 38), fill=True)
    canvas.draw_circle(int(center_x), int(center_y), 24, (255, 255, 255), fill=False)
    canvas.draw_text(int(center_x) + 34, int(center_y) - 8, "DISTRIBUTION HUB", (127, 29, 29), scale=2)

    sidebar = (800, 160, 1052, 660)
    canvas.fill_rect(sidebar[0], sidebar[1], sidebar[2], sidebar[3], (255, 253, 248))
    canvas.draw_rect_outline(sidebar[0], sidebar[1], sidebar[2], sidebar[3], (211, 199, 185), thickness=3)
    canvas.draw_text(832, 194, "ACTIVE WAREHOUSES", ink_color, scale=2)

    for index, warehouse_id in enumerate(warehouse_ids[:8]):
        bullet_y = 252 + (index * 46)
        canvas.draw_circle(842, bullet_y + 12, 8, teal, fill=True)
        canvas.draw_text(866, bullet_y, warehouse_id, muted_color, scale=2)

    canvas.draw_text(832, 560, "DATA SOURCE", ink_color, scale=2)
    canvas.draw_text(832, 598, "ORACLE SPATIAL PLACEHOLDER", muted_color, scale=2)
    canvas.draw_text(832, 630, "SWAP TO REAL QUERY LATER", muted_color, scale=2)

    return base64.b64encode(canvas.to_png_bytes()).decode("ascii")


class SpatialImageExecutor(AgentExecutor):
    async def execute(self, context, event_queue) -> None:
        updater = TaskUpdater(event_queue, context.task_id, context.context_id)
        if not context.current_task:
            await updater.submit()

        await updater.start_work()

        try:
            warehouse_ids = extract_warehouse_ids(context.get_user_input(""))
            png_bytes = render_warehouse_map_png(warehouse_ids)

            image_part = Part(
                root=FilePart(
                    file=FileWithBytes(
                        bytes=png_bytes,
                        mimeType="image/png",
                        name="warehouse-map.png",
                    )
                )
            )
            message_text = Part(
                root=TextPart(
                    text=f"Generated a PNG warehouse map for {', '.join(warehouse_ids)}."
                )
            )

            await updater.add_artifact(
                [image_part],
                name="warehouse_map_png",
                metadata={
                    "warehouse_ids": warehouse_ids,
                    "content_type": "image/png",
                },
            )
            await updater.complete(
                updater.new_agent_message(
                    [message_text],
                    metadata={
                        "warehouse_ids": warehouse_ids,
                        "artifact_name": "warehouse-map.png",
                    },
                )
            )
        except Exception as exc:
            await updater.failed(
                updater.new_agent_message(
                    [
                        Part(
                            root=TextPart(
                                text=f"Spatial agent failed: {exc}"
                            )
                        )
                    ],
                    metadata={"error": "spatial_agent_execution_failed"},
                )
            )
            raise JSONRPCError(code=-32603, message=f"Spatial agent execution failed: {exc}")

    async def cancel(self, context, event_queue) -> None:
        raise TaskNotCancelableError()


RPC_URL = f"{PUBLIC_PROTOCOL}://{PUBLIC_HOST}:{PORT}"
AGENT_CARD = AgentCard(
    capabilities=AgentCapabilities(streaming=False, pushNotifications=False, stateTransitionHistory=False, extensions=[]),
    defaultInputModes=["text/plain"],
    defaultOutputModes=["image/png", "text/plain"],
    description="Specialist in Oracle Geospatial data and map visualizations.",
    name="oracle_spatial_agent",
    preferredTransport="JSONRPC",
    protocolVersion="0.3.0",
    skills=[
        AgentSkill(
            description=(
                "Specialist in Oracle Geospatial data and map visualizations. "
                "Returns warehouse maps as PNG artifacts."
            ),
            examples=[],
            id="oracle_spatial_agent",
            inputModes=["text/plain"],
            name="model",
            outputModes=["image/png", "text/plain"],
            tags=["llm"],
        ),
        AgentSkill(
            description=(
                "Queries Oracle Spatial for warehouse coordinates and returns a PNG map artifact. "
                "Input: warehouse request text such as 'Show me a map for WH-101 and WH-202'."
            ),
            examples=[],
            id="oracle_spatial_agent-generate_warehouse_map",
            name="generate_warehouse_map",
            outputModes=["image/png"],
            tags=["llm", "tools"],
        ),
    ],
    supportsAuthenticatedExtendedCard=False,
    url=RPC_URL,
    version="0.0.1",
)

REQUEST_HANDLER = DefaultRequestHandler(
    agent_executor=SpatialImageExecutor(),
    task_store=InMemoryTaskStore(),
    push_config_store=InMemoryPushNotificationConfigStore(),
)
app = A2AStarletteApplication(
    agent_card=AGENT_CARD,
    http_handler=REQUEST_HANDLER,
).build()

if __name__ == "__main__":
    uvicorn.run(app, host=BIND_HOST, port=PORT)

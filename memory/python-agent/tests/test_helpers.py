import sys
import unittest
from pathlib import Path

APP_DIR = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(APP_DIR))

from app import result_to_dict, utc_iso  # noqa: E402
from ar import ArExperienceService  # noqa: E402


class FakeRecord:
    id = "memory-1"
    content = "stored content"
    record_type = "preference"
    user_id = "ava"
    agent_id = "starlight-concierge"
    thread_id = "thread-1"
    timestamp = "2026-07-28T00:00:00+00:00"
    metadata = {"kind": "semantic"}


class FakeResult:
    record = FakeRecord()
    content = "retrieved content"


class HelpersTest(unittest.TestCase):
    def test_result_serialization_preserves_scope(self):
        value = result_to_dict(FakeResult())
        self.assertEqual("ava", value["userId"])
        self.assertEqual("semantic", value["metadata"]["kind"])
        self.assertEqual("retrieved content", value["content"])

    def test_utc_iso_is_timezone_aware(self):
        self.assertIn("+00:00", utc_iso())

    def test_ar_retention_is_bounded(self):
        self.assertEqual(7, ArExperienceService._retention_days("7"))
        with self.assertRaises(ValueError):
            ArExperienceService._retention_days(31)

    def test_ar_identifiers_reject_injection_characters(self):
        self.assertEqual("ar-session_1", ArExperienceService._identifier("ar-session_1", "id"))
        with self.assertRaises(ValueError):
            ArExperienceService._identifier("ar-session' OR 1=1", "id")


if __name__ == "__main__":
    unittest.main()

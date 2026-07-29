import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent.parent))

from a2ui_payloads import review_messages, result_messages


class A2uiPayloadTests(unittest.TestCase):
    def test_review_uses_gemini_enterprise_v08_shape(self):
        messages = review_messages(
            [
                {
                    "recommendationId": "1001:102:101",
                    "sku": "BAT-48V",
                    "productName": "48V Solar Battery Pack",
                    "sourceLocationCode": "PHX-DC",
                    "targetLocationCode": "ATL-DC",
                    "recommendedTransferQuantity": 20,
                    "stockoutRiskScore": 96,
                    "riskLevel": "CRITICAL",
                    "rationale": "Target demand exceeds governed coverage.",
                }
            ],
            "approval-handle",
        )
        self.assertEqual(
            ["beginRendering", "surfaceUpdate", "dataModelUpdate"],
            [next(iter(message)) for message in messages],
        )
        encoded = str(messages)
        self.assertIn("approveInventoryTransfer", encoded)
        self.assertIn("rejectInventoryTransferReview", encoded)
        self.assertIn("approval-handle", encoded)
        self.assertNotIn("createSurface", encoded)
        self.assertNotIn("version", encoded)

    def test_result_is_a_small_native_surface(self):
        messages = result_messages("Approved", "Transfer 1001 approved.")
        self.assertEqual(2, len(messages))
        self.assertIn("surfaceUpdate", messages[1])


if __name__ == "__main__":
    unittest.main()

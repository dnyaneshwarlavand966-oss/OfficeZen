import os
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

os.environ.setdefault("GCP_AUTH_TOKEN", "offline-test-token")

from scripts import firebase_readonly_audit as audit


class ReadOnlyAuditorTests(unittest.TestCase):
    def test_rules_heuristics_flag_unconditional_and_unauthenticated_access(self):
        result = audit.analyze_rules(
            "match /databases/{database}/documents { allow read, write: if true; "
            "allow delete: if request.auth == null; }"
        )
        self.assertIn("unconditional_allow_true", result["heuristic_flags"])
        self.assertIn("unauthenticated_access_check", result["heuristic_flags"])
        self.assertEqual(len(result["source_sha256"]), 64)

    def test_main_reads_both_databases_and_follows_rules_release_pages(self):
        db_ids = audit.DATABASE_IDS
        fake_db_list = {
            "databases": [
                {
                    "name": f"projects/{audit.PROJECT}/databases/{db_ids[0]}",
                    "locationId": "asia-south1",
                    "type": "FIRESTORE_NATIVE",
                },
                {
                    "name": f"projects/{audit.PROJECT}/databases/{db_ids[1]}",
                    "locationId": "asia-southeast1",
                    "type": "FIRESTORE_NATIVE",
                },
            ]
        }
        first_page = {
            "releases": [
                {"name": f"projects/{audit.PROJECT}/releases/cloud.firestore/unrelated",
                 "rulesetName": f"projects/{audit.PROJECT}/rulesets/other"}
            ],
            "nextPageToken": "page-two",
        }
        second_page = {
            "releases": [
                {"name": f"projects/{audit.PROJECT}/releases/cloud.firestore/{db_ids[0]}",
                 "rulesetName": f"projects/{audit.PROJECT}/rulesets/rules-a"},
                {"name": f"projects/{audit.PROJECT}/releases/cloud.firestore/{db_ids[1]}",
                 "rulesetName": f"projects/{audit.PROJECT}/rulesets/rules-b"},
            ]
        }

        requested = []

        def fake_get_json(url):
            requested.append(url)
            if "firestore.googleapis.com" in url:
                return fake_db_list
            if "pageToken=page-two" in url:
                return second_page
            if url.endswith("/releases?pageSize=100"):
                return first_page
            if url.endswith("/rulesets/rules-a") or url.endswith("/rulesets/rules-b"):
                return {"source": {"files": [{"content": "allow read: if false;"}]}}
            raise AssertionError(f"Unexpected API request: {url}")

        with tempfile.TemporaryDirectory() as tmp:
            with patch.object(audit, "OUT", Path(tmp)), patch.object(
                audit, "get_json", side_effect=fake_get_json
            ):
                self.assertEqual(audit.main(), 0)
                report = (Path(tmp) / "report.json").read_text(encoding="utf-8")
                self.assertTrue((Path(tmp) / "report.html").is_file())

        import json
        parsed = json.loads(report)
        self.assertEqual(parsed["overall_status"], "VERIFIED_WITH_FINDINGS")
        self.assertEqual([x["status"] for x in parsed["database_checks"]], ["VERIFIED", "VERIFIED"])
        self.assertEqual([x["status"] for x in parsed["published_rules_checks"]], ["VERIFIED", "VERIFIED"])
        self.assertTrue(any("pageToken=page-two" in url for url in requested))
        self.assertFalse(any("/documents" in url for url in requested))


if __name__ == "__main__":
    unittest.main()

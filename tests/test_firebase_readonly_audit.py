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


    def test_get_json_sends_get_with_bearer_token_only(self):
        class FakeResponse:
            def __enter__(self):
                return self

            def __exit__(self, *args):
                return False

            def read(self):
                return b'{"ok": true}'

        captured = {}

        def fake_urlopen(request, timeout):
            captured["request"] = request
            captured["timeout"] = timeout
            return FakeResponse()

        with patch.object(audit, "TOKEN", "unit-test-token"), patch.object(
            audit.urllib.request, "urlopen", side_effect=fake_urlopen
        ):
            self.assertEqual(audit.get_json("https://example.invalid/read"), {"ok": True})

        request = captured["request"]
        self.assertEqual(request.get_method(), "GET")
        self.assertEqual(request.get_header("Authorization"), "Bearer unit-test-token")
        self.assertEqual(captured["timeout"], 25)

    def test_get_json_fails_closed_without_token(self):
        with patch.object(audit, "TOKEN", ""), patch.object(
            audit.urllib.request, "urlopen"
        ) as network:
            with self.assertRaisesRegex(RuntimeError, "GCP_AUTH_TOKEN is missing"):
                audit.get_json("https://example.invalid/read")
            network.assert_not_called()

    def test_http_error_does_not_expose_response_body(self):
        import urllib.error

        secret_body = b'private-project-response-content'
        error = urllib.error.HTTPError(
            "https://example.invalid/read", 403, "Forbidden", {}, None
        )
        with patch.object(audit, "TOKEN", "unit-test-token"), patch.object(
            audit.urllib.request, "urlopen", side_effect=error
        ):
            with self.assertRaises(RuntimeError) as caught:
                audit.get_json("https://example.invalid/read")
        self.assertEqual(str(caught.exception), "HTTP 403")
        self.assertNotIn(secret_body.decode(), str(caught.exception))

    def test_rules_analyzer_returns_no_flags_for_closed_rule(self):
        result = audit.analyze_rules(
            "match /databases/{database}/documents { allow read, write: if false; }"
        )
        self.assertEqual(result["heuristic_flags"], [])


if __name__ == "__main__":
    unittest.main()

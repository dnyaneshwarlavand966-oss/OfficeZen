#!/usr/bin/env python3
"""Read-only OfficeZen Firebase metadata and published Firestore Rules auditor.

This script performs GET requests only. It never reads Firestore documents,
never writes to Firebase, never publishes rules, and never changes IAM.
"""
from __future__ import annotations

import hashlib
import html
import json
import os
import re
import sys
import urllib.error
import urllib.parse
import urllib.request
from datetime import datetime, timezone
from pathlib import Path

PROJECT = os.environ.get("GOOGLE_CLOUD_PROJECT", "officezen-d42ad")
TOKEN = os.environ.get("GCP_AUTH_TOKEN", "")
DATABASE_IDS = (
    "officezen-db",
    "ai-studio-android-officeze-7dd93b37-6073-4473-9d46-73f59ebdea79",
)
OUT = Path("audit-output")
OUT.mkdir(parents=True, exist_ok=True)


def get_json(url: str) -> dict:
    if not TOKEN:
        raise RuntimeError("GCP_AUTH_TOKEN is missing")
    req = urllib.request.Request(
        url,
        headers={"Authorization": f"Bearer {TOKEN}", "Accept": "application/json"},
        method="GET",
    )
    try:
        with urllib.request.urlopen(req, timeout=25) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        # Do not emit response bodies: they can contain project metadata.
        raise RuntimeError(f"HTTP {exc.code}") from None
    except urllib.error.URLError:
        raise RuntimeError("Network/API connection failed") from None


def analyze_rules(source_text: str) -> dict:
    # Heuristic flags only; this is not a formal proof of Rules correctness.
    patterns = {
        "unconditional_allow_true": r"\ballow\s+(?:read|write|create|update|delete)(?:\s*,\s*(?:read|write|create|update|delete))*\s*:\s*if\s+true\b",
        "unauthenticated_access_check": r"request\.auth\s*==\s*null",
        "broad_authenticated_access": r"allow\s+(?:read|write)(?:\s*,\s*(?:read|write))*\s*:\s*if\s+request\.auth\s*!=\s*null",
    }
    flags = [name for name, pattern in patterns.items()
             if re.search(pattern, source_text, flags=re.IGNORECASE)]
    return {
        "heuristic_flags": flags,
        "source_sha256": hashlib.sha256(source_text.encode("utf-8")).hexdigest(),
        "source_bytes": len(source_text.encode("utf-8")),
        "note": "Heuristic scan only; manual semantic review and emulator tests are still required.",
    }


def main() -> int:
    now = datetime.now(timezone.utc).isoformat()
    report = {
        "project_id": PROJECT,
        "generated_at_utc": now,
        "mode": "READ_ONLY_GET_ONLY",
        "database_checks": [],
        "published_rules_checks": [],
        "privacy": {
            "firestore_documents_read": False,
            "employee_names_or_emails_read": False,
            "payroll_or_document_contents_read": False,
        },
    }

    base = "https://firestore.googleapis.com/v1"
    db_map = {}
    try:
        payload = get_json(
            f"{base}/projects/{urllib.parse.quote(PROJECT, safe='')}/databases"
        )
        for db in payload.get("databases", []):
            name = db.get("name", "")
            db_id = name.rsplit("/", 1)[-1]
            db_map[db_id] = db
    except Exception as exc:
        report["database_list_status"] = f"NOT_VERIFIED: {exc}"

    for db_id in DATABASE_IDS:
        db = db_map.get(db_id)
        if not db:
            report["database_checks"].append({
                "database_id": db_id,
                "status": "NOT_VERIFIED",
                "reason": report.get("database_list_status", "Database not returned by list API"),
            })
            continue
        report["database_checks"].append({
            "database_id": db_id,
            "status": "VERIFIED",
            "name": db.get("name"),
            "location_id": db.get("locationId"),
            "type": db.get("type"),
            "concurrency_mode": db.get("concurrencyMode"),
            "point_in_time_recovery_enablement": db.get("pointInTimeRecoveryEnablement"),
            "delete_protection_state": db.get("deleteProtectionState"),
        })

    rules_base = "https://firebaserules.googleapis.com/v1"
    try:
        # Firebase Rules list is paginated (pageSize <= 100). Read every page
        # so a release on a later page is not incorrectly reported as missing.
        releases = []
        page_token = None
        while True:
            releases_url = (
                f"{rules_base}/projects/{urllib.parse.quote(PROJECT, safe='')}/releases"
                "?pageSize=100"
            )
            if page_token:
                releases_url += "&pageToken=" + urllib.parse.quote(page_token, safe="")
            releases_payload = get_json(releases_url)
            releases.extend(releases_payload.get("releases", []))
            page_token = releases_payload.get("nextPageToken")
            if not page_token:
                break
        for db_id in DATABASE_IDS:
            matches = [
                item for item in releases
                if item.get("name", "").endswith("/cloud.firestore/" + db_id)
                or item.get("name", "").endswith("/cloud.firestore/" + db_id + "/")
                or item.get("name", "").endswith("/cloud.firestore/" + db_id.replace("-", "_"))
            ]
            if not matches:
                report["published_rules_checks"].append({
                    "database_id": db_id,
                    "status": "NOT_VERIFIED",
                    "reason": "No matching published Firestore Rules release was returned.",
                })
                continue
            release = matches[0]
            ruleset_name = release.get("rulesetName")
            if not ruleset_name:
                report["published_rules_checks"].append({
                    "database_id": db_id,
                    "status": "NOT_VERIFIED",
                    "reason": "Release has no rulesetName.",
                })
                continue
            ruleset = get_json(f"{rules_base}/{ruleset_name.lstrip('/')}")
            files = ruleset.get("source", {}).get("files", [])
            combined = "\n".join(item.get("content", "") for item in files)
            report["published_rules_checks"].append({
                "database_id": db_id,
                "status": "VERIFIED",
                "release_name": release.get("name"),
                "ruleset_name": ruleset_name,
                "ruleset_files_count": len(files),
                "rules_analysis": analyze_rules(combined),
            })
    except Exception as exc:
        report["rules_api_status"] = f"NOT_VERIFIED: {exc}"
        for db_id in DATABASE_IDS:
            if not any(x.get("database_id") == db_id for x in report["published_rules_checks"]):
                report["published_rules_checks"].append({
                    "database_id": db_id,
                    "status": "NOT_VERIFIED",
                    "reason": str(exc),
                })

    report["overall_status"] = (
        "VERIFIED_WITH_FINDINGS"
        if all(x["status"] == "VERIFIED" for x in report["database_checks"])
        and all(x["status"] == "VERIFIED" for x in report["published_rules_checks"])
        else "PARTIAL_NOT_VERIFIED"
    )

    json_path = OUT / "report.json"
    json_path.write_text(json.dumps(report, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")

    rows = []
    for item in report["database_checks"]:
        rows.append(f"<tr><td>Database</td><td>{html.escape(item['database_id'])}</td><td>{html.escape(item['status'])}</td></tr>")
    for item in report["published_rules_checks"]:
        rows.append(f"<tr><td>Published Rules</td><td>{html.escape(item['database_id'])}</td><td>{html.escape(item['status'])}</td></tr>")
    page = """<!doctype html><html lang="en"><meta charset="utf-8">
<title>OfficeZen Firebase Read-Only Audit</title>
<h1>OfficeZen Firebase Read-Only Audit</h1>
<p>Project: """ + html.escape(PROJECT) + """</p>
<p>Generated (UTC): """ + html.escape(now) + """</p>
<p>Overall: <strong>""" + html.escape(report["overall_status"]) + """</strong></p>
<table border="1" cellpadding="6"><thead><tr><th>Check</th><th>Target</th><th>Status</th></tr></thead><tbody>
""" + "\n".join(rows) + """</tbody></table>
<p>Firestore documents and personal/business records were not read. This is a metadata and heuristic Rules audit, not a proof of security.</p>
</html>"""
    (OUT / "report.html").write_text(page, encoding="utf-8")
    print(f"Audit completed: {report['overall_status']}")
    return 0 if report["overall_status"] == "VERIFIED_WITH_FINDINGS" else 1


if __name__ == "__main__":
    sys.exit(main())

# -*- coding: utf-8 -*-
"""Write service unit test Java files. Skips already-restored tests."""
from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
TEST_ROOT = ROOT / "src/test/java/com/neusoft/cloudbrain/service"

SKIP = {
    "AuthServiceTest.java",
    "RegistrationServiceTest.java",
    "PaymentServiceTest.java",
    "PrescriptionServiceTest.java",
    "ai/AiExamReportServiceTest.java",
}

TESTS: dict[str, str] = {}

def main() -> None:
    written: list[str] = []
    skipped: list[str] = []
    for rel, content in sorted(TESTS.items()):
        if rel in SKIP:
            skipped.append(rel)
            continue
        target = TEST_ROOT / rel
        target.parent.mkdir(parents=True, exist_ok=True)
        target.write_text(content.strip() + "\n", encoding="utf-8")
        written.append(rel)
    print("Wrote", len(written), "files")
    for name in written:
        print(" ", name)
    if skipped:
        print("Skipped", len(skipped))
        for name in skipped:
            print(" ", name)

if __name__ == "__main__":
    main()

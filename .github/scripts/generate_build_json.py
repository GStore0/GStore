
import hashlib
import json
import os
import subprocess
import sys

from datetime import datetime, timezone
from pathlib import Path


ICON_URL = (
    "https://raw.githubusercontent.com/"
    "{repository}/refs/heads/main/"
    "app/src/main/res/mipmap-hdpi/ic_launcher.png"
)


def run(command):
    return subprocess.check_output(
        command,
        text=True
    ).strip()


def calculate_sha256(file_path):
    digest = hashlib.sha256()

    with open(file_path, "rb") as file:
        for chunk in iter(lambda: file.read(1024 * 1024), b""):
            digest.update(chunk)

    return digest.hexdigest()


def get_apk_info(apk):
    version_name = run([
        "apkanalyzer",
        "manifest",
        "version-name",
        str(apk)
    ])

    version_code = run([
        "apkanalyzer",
        "manifest",
        "version-code",
        str(apk)
    ])

    return version_name, version_code


def generate_metadata(apk, build_type):
    repository = os.environ["GITHUB_REPOSITORY"]
    tag = os.environ["GITHUB_REF_NAME"]

    version_name, version_code = get_apk_info(apk)

    now = datetime.now(timezone.utc)

    filename = apk.name

    download_url = (
        f"https://github.com/{repository}/releases/download/"
        f"{tag}/{filename}"
    )

    changelog = run([
        "git",
        "log",
        "-1",
        "--format=%B",
        "HEAD"
    ])

    return {
        "version_name": version_name,
        "version_code": version_code,
        "download_url": download_url,
        "sha256": calculate_sha256(apk),
        "icon_url": ICON_URL.format(repository=repository),
        "changelog": changelog,
        "size": str(apk.stat().st_size),
        "last_commit": run([
            "git",
            "rev-parse",
            "--short=9",
            "HEAD"
        ]),
        "updated_on": now.strftime("%b %d, %Y"),
        "timestamp": str(int(now.timestamp()))
    }


def main():
    if len(sys.argv) != 3:
        print(
            "Usage: python generate_build_json.py "
            "<apk_path> <release|nightly>"
        )
        sys.exit(1)

    apk = Path(sys.argv[1])
    build_type = sys.argv[2]

    if build_type not in ("release", "nightly"):
        raise ValueError(f"Invalid build type: {build_type}")

    if not apk.is_file():
        raise FileNotFoundError(f"APK not found: {apk}")

    metadata = generate_metadata(apk, build_type)

    output_dir = Path("release")
    output_dir.mkdir(parents=True, exist_ok=True)

    output_file = output_dir / f"{build_type}_build.json"

    with output_file.open("w", encoding="utf-8") as file:
        json.dump(
            metadata,
            file,
            indent=2,
            ensure_ascii=False
        )
        file.write("\n")

    print(f"Generated: {output_file}")
    print(json.dumps(metadata, indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()

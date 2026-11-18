
import hashlib
import json
import os
import subprocess
import sys
import urllib.request
import shutil

from datetime import datetime, timezone
from pathlib import Path


ICON_URL = (
    "https://raw.githubusercontent.com/"
    "{repository}/refs/heads/master/"
    "app/src/main/res/mipmap-hdpi/ic_launcher.png"
)


def run(command):
    binary = shutil.which(command[0])

    if not binary:
        raise FileNotFoundError(f"Command not found: {command[0]}")

    command[0] = binary
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


def get_previous_release_tag(repository, current_tag, build_type):
    url = (
        f"https://api.github.com/repos/{repository}/releases"
        "?per_page=100"
    )

    headers = {
        "Accept": "application/vnd.github+json"
    }

    token = os.environ.get("GITHUB_TOKEN")

    if token:
        headers["Authorization"] = f"Bearer {token}"

    request = urllib.request.Request(url, headers=headers)

    with urllib.request.urlopen(request) as response:
        releases = json.loads(response.read())

    prerelease = build_type == "nightly"

    filtered_releases = [
        release["tag_name"]
        for release in releases
        if release["prerelease"] == prerelease and not release["draft"]
    ]

    if current_tag in filtered_releases:
        index = filtered_releases.index(current_tag) + 1
    else:
        index = 0

    if index >= len(filtered_releases):
        raise RuntimeError("Previous release not found")

    return filtered_releases[index]


def generate_metadata(apk, build_type):
    repository = os.environ["GITHUB_REPOSITORY"]
    tag = os.environ["GITHUB_REF_NAME"]

    version_name, version_code = get_apk_info(apk)

    now = datetime.now(timezone.utc)

    filename = apk.name

    download_url = (
        f"https://github.com/{repository}/releases/latest/download/"
        f"{filename}"
    )

    if build_type == "nightly":
        download_url = (
            f"https://github.com/{repository}/releases/download/"
            f"v{version_name}/"
            f"{filename}"
        )

    previous_tag = get_previous_release_tag(repository, tag, build_type)

    changelog = run([
        "git",
        "log",
        f"{previous_tag}..{tag}",
        "--pretty=format:- %s"
    ])

    last_commit_timestamp = run([
        "git",
        "log",
        "-1",
        "--format=%ct"
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
        "timestamp": last_commit_timestamp
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

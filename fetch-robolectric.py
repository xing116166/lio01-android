"""Pre-fetch Robolectric android-all jars for offline unit tests.

Usage:  python tools/fetch-robolectric.py

Robolectric downloads these jars on the first test run; when they already exist
in .robolectric-deps the Gradle build runs in offline mode (no network needed,
and no writes outside the project directory).
"""

import os
import sys
import urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DEST = os.path.join(ROOT, ".robolectric-deps")

ARTIFACTS = [
    ("android-all-instrumented", "15-robolectric-12650502-i7"),
    ("android-all-instrumented", "14-robolectric-10818077-i7"),
]

REPOS = [
    "https://maven.aliyun.com/repository/public",
    "https://repo1.maven.org/maven2",
]


def download(group, artifact, version):
    os.makedirs(DEST, exist_ok=True)
    filename = f"{artifact}-{version}.jar"
    target = os.path.join(DEST, filename)
    if os.path.isfile(target) and os.path.getsize(target) > 10_000_000:
        print(f"exists  {filename}")
        return

    rel = f"{group.replace('.', '/')}/{artifact}/{version}/{filename}"
    last_error = None
    for repo in REPOS:
        url = f"{repo}/{rel}"
        try:
            print(f"fetching {url}")
            request = urllib.request.Request(url, headers={"User-Agent": "Xboard-build"})
            with urllib.request.urlopen(request, timeout=120) as response, open(target + ".part", "wb") as out:
                total = int(response.headers.get("Content-Length") or 0)
                done = 0
                while True:
                    chunk = response.read(1024 * 256)
                    if not chunk:
                        break
                    out.write(chunk)
                    done += len(chunk)
                    if total:
                        print(f"\r  {done * 100 // total}%", end="", flush=True)
            os.replace(target + ".part", target)
            print(f"\ndone    {filename} ({os.path.getsize(target) // (1024 * 1024)} MB)")
            return
        except Exception as exc:  # noqa: BLE001 - try the next mirror
            last_error = exc
            print(f"  failed: {exc}")
            if os.path.exists(target + ".part"):
                os.remove(target + ".part")
    raise SystemExit(f"could not download {filename}: {last_error}")


def main():
    for artifact, version in ARTIFACTS:
        download("org.robolectric", artifact, version)
    print("Robolectric jars ready.")


if __name__ == "__main__":
    sys.exit(main())

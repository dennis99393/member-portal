import time
import sys
import urllib.request

ENDPOINT = "http://localhost:8081/cron/backfill-smartwaiver"
HEADERS = {
    "X-Api-Key": "dummy-key-for-dev",
    "X-Api-Client": "member-profile-ui",
}
DELAY_SECONDS = 60

iteration = 0
while True:
    iteration += 1
    print(f"\n=== Iteration {iteration} ===")

    req = urllib.request.Request(ENDPOINT, headers=HEADERS)
    try:
        with urllib.request.urlopen(req) as resp:
            body = resp.read().decode()
    except Exception as e:
        print(f"ERROR: {e}")
        sys.exit(1)

    print(body)

    if "done=true" in body:
        print("\nBackfill complete!")
        sys.exit(0)

    print(f"Waiting {DELAY_SECONDS}s before next batch...")
    time.sleep(DELAY_SECONDS)

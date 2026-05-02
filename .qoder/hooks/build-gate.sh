#!/bin/bash
# build-gate.sh — Ensures project still compiles/builds after changes.
# Exit 0 = pass, non-zero = fail.
# Usage: .qoder/hooks/build-gate.sh

PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
EXIT_CODE=0

cd "$PROJECT_ROOT" || exit 1

# --- Backend build gate ---
if [ -f "grace-platform/pom.xml" ]; then
    echo "Running backend build gate..."
    OUTPUT=$(cd grace-platform && mvn clean compile -q 2>&1)
    RC=$?
    if [ $RC -ne 0 ]; then
        echo "=== BACKEND BUILD FAILED ==="
        echo "$OUTPUT" | tail -30
        EXIT_CODE=$((EXIT_CODE + RC))
    else
        echo "Backend build OK."
    fi
fi

# --- Frontend build gate ---
if [ -f "grace-frontend/package.json" ]; then
    echo "Running frontend build gate..."
    OUTPUT=$(cd grace-frontend && npm run build 2>&1)
    RC=$?
    if [ $RC -ne 0 ]; then
        echo "=== FRONTEND BUILD FAILED ==="
        echo "$OUTPUT" | tail -30
        EXIT_CODE=$((EXIT_CODE + RC))
    else
        echo "Frontend build OK."
    fi
fi

exit $EXIT_CODE

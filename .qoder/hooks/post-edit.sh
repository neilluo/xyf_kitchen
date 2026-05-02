#!/bin/bash
# post-edit.sh — Runs after agent modifies code files.
# Exit 0 = pass, non-zero = fail (output goes to agent context).
# Usage: .qoder/hooks/post-edit.sh [changed files...]

CHANGED_FILES="$*"
PROJECT_ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
EXIT_CODE=0

cd "$PROJECT_ROOT" || exit 1

# --- Java backend checks ---
if echo "$CHANGED_FILES" | grep -qE '\.java$' || [ -z "$CHANGED_FILES" ]; then
    if [ -f "grace-platform/pom.xml" ]; then
        OUTPUT=$(cd grace-platform && mvn compile -q 2>&1)
        RC=$?
        if [ $RC -ne 0 ]; then
            echo "=== BACKEND COMPILE FAILED ==="
            echo "$OUTPUT" | tail -30
            echo ""
            echo "Fix: Read the compiler error above and correct the code."
            EXIT_CODE=$((EXIT_CODE + RC))
        fi
    fi
fi

# --- TypeScript/React frontend checks ---
if echo "$CHANGED_FILES" | grep -qE '\.(ts|tsx)$' || [ -z "$CHANGED_FILES" ]; then
    if [ -f "grace-frontend/package.json" ]; then
        OUTPUT=$(cd grace-frontend && npx eslint . --quiet 2>&1)
        RC=$?
        if [ $RC -ne 0 ]; then
            echo "=== FRONTEND ESLINT FAILED ==="
            echo "$OUTPUT" | tail -30
            echo ""
            echo "Fix: Read the ESLint errors above and correct the code."
            EXIT_CODE=$((EXIT_CODE + RC))
        fi

        OUTPUT=$(cd grace-frontend && npx tsc --noEmit 2>&1)
        RC=$?
        if [ $RC -ne 0 ]; then
            echo "=== FRONTEND TYPE CHECK FAILED ==="
            echo "$OUTPUT" | tail -30
            echo ""
            echo "Fix: Read the TypeScript errors above and correct the code."
            EXIT_CODE=$((EXIT_CODE + RC))
        fi
    fi
fi

if [ $EXIT_CODE -eq 0 ]; then
    # Success is silent — no output
    exit 0
fi

exit $EXIT_CODE

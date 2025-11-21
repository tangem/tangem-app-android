#!/bin/bash

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

OUTPUT_FILE="${1:-detekt_baseline_report.txt}"

INITIAL_ISSUES=1802

log() {
    echo "$@" | tee -a "$OUTPUT_FILE"
}

> "$OUTPUT_FILE"

log "=========================================="
log "Detekt Baseline Updater & Issue Counter"
log "=========================================="
log "Date: $(date '+%Y-%m-%d %H:%M:%S')"
log ""

log "Updating detekt baseline for debug variant..."
log ""

./gradlew detektBaselineDebug

log ""
log "Baseline updated successfully!"
log ""

log "=========================================="
log "Counting issues in baseline files..."
log "=========================================="
log ""

total_issues=0
module_count=0

temp_file=$(mktemp)

find . -name "detekt-baseline-debug.xml" -type f | while IFS= read -r file; do
    issue_count=$(grep -c "<ID>" "$file" 2>/dev/null || echo "0")

    if [ "$issue_count" -gt 0 ]; then
        module_name=$(echo "$file" | sed 's|^\./||' | sed 's|/detekt-baseline-debug.xml$||')
        echo "$issue_count|$module_name"
    fi
done | sort -rn -t'|' -k1 > "$temp_file"

total_issues=$(awk -F'|' '{sum+=$1} END {print sum}' "$temp_file")
module_count=$(wc -l < "$temp_file" | tr -d ' ')

log "Summary:"
log "  Total Issues: $total_issues"
log "  Modules with Issues: $module_count"

if [ "$module_count" -gt 0 ]; then
    avg_issues=$((total_issues / module_count))
    log "  Average Issues per Module: $avg_issues"
fi

fixed_issues=$((INITIAL_ISSUES - total_issues))
progress_percent=$((fixed_issues * 100 / INITIAL_ISSUES))

log ""
log "Progress:"
if [ "$fixed_issues" -gt 0 ]; then
    log "  Fixed: $fixed_issues out of $INITIAL_ISSUES ($progress_percent%)"
    log "  Remaining: $total_issues"
elif [ "$fixed_issues" -lt 0 ]; then
    new_issues=$((total_issues - INITIAL_ISSUES))
    log "  Warning: $new_issues new issues added since baseline!"
else
    log "  Fixed: 0 out of $INITIAL_ISSUES (0%)"
    log "  Remaining: $total_issues"
fi

log ""
log "=========================================="
log "All Modules with Issues (sorted by count)"
log "=========================================="
log ""
printf "%-60s %10s\n" "Module" "Issues" | tee -a "$OUTPUT_FILE"
log "────────────────────────────────────────────────────────────────"

awk -F'|' '{printf "%-60s %10s\n", $2, $1}' "$temp_file" | tee -a "$OUTPUT_FILE"

log "────────────────────────────────────────────────────────────────"

rm "$temp_file"


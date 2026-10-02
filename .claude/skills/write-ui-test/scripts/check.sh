#!/usr/bin/env bash
# Mechanical self-review of a UI-test change: the Android repo working tree (untracked files included)
# against <android-base>, and the tangem-api-mocks working tree against <mocks-base>.
# Usage: check.sh [<android-base>] [<mocks-base>]   (defaults: origin/develop, origin/main)
# MOCKS_ROOT overrides the mocks repo location (default: ../tangem-api-mocks next to this repo).
set -euo pipefail

repo_root="$(git rev-parse --show-toplevel)"
base="${1:-origin/develop}"
mocks_base="${2:-origin/main}"
mocks_root="${MOCKS_ROOT:-$(cd "$repo_root/.." && pwd)/tangem-api-mocks}"
tmp_root="${TMPDIR:-/tmp}"
out_dir="$(mktemp -d "${tmp_root%/}/write-ui-test-check.XXXXXX")"
report="$out_dir/report.txt"
review_diff="$out_dir/review.diff"
lines_file="$out_dir/lines.tsv"
: > "$report"
: > "$review_diff"

android_test_dir="app/src/androidTest/"

issue() {
    printf '%s:%s: [%s] %s: %s\n' "$1" "$2" "$3" "$4" "$5" >> "$report"
}

merge_base() {
    local repo="$1" ref="$2"
    if ! git -C "$repo" rev-parse --verify --quiet "$ref^{commit}" > /dev/null; then
        echo "check.sh: base '$ref' not found in $repo" >&2
        exit 2
    fi
    git -C "$repo" merge-base "$ref" HEAD
}

full_diff() {
    local repo="$1" mb="$2" context="$3" path
    git -C "$repo" diff --no-color --no-ext-diff "-U$context" "$mb" --
    git -C "$repo" ls-files --others --exclude-standard | while IFS= read -r path; do
        git -C "$repo" diff --no-color --no-ext-diff "-U$context" --no-index -- /dev/null "$path" || true
    done
}

# One TSV row per changed line: file, line (new side), +/-, text.
diff_lines() {
    awk '
        /^diff --git / { hdr = 1; old = ""; file = ""; next }
        hdr && /^--- / { old = substr($0, 5); sub(/^a\//, "", old); next }
        hdr && /^\+\+\+ / {
            file = substr($0, 5); sub(/^b\//, "", file)
            if (file == "/dev/null") file = old
            next
        }
        /^@@ / {
            hdr = 0
            match($0, /\+[0-9]+/)
            n = substr($0, RSTART + 1, RLENGTH - 1) + 0
            if (n == 0) n = 1
            next
        }
        hdr { next }
        /^\+/ { printf "%s\t%d\t+\t%s\n", file, n, substr($0, 2); n++; next }
        /^-/ { printf "%s\t%d\t-\t%s\n", file, n, substr($0, 2); next }
    '
}

changed_files() {
    awk -F '\t' '{ print $1 }' "$lines_file" | sort -u
}

check_test_lines() {
    awk -F '\t' -v at="$android_test_dir" '
        function emit(level, rule, msg) { printf "%s:%s: [%s] %s: %s\n", file, line, level, rule, msg }
        function strip_strings(s) { gsub(/"([^"\\]|\\.)*"/, "\"\"", s); return s }
        $1 !~ /\.kt$/ || index($1, at) != 1 || $3 != "+" { next }
        {
            file = $1; line = $2; text = $4
            for (i = 5; i <= NF; i++) text = text "\t" $i
            code = strip_strings(text)
            is_test = file ~ /\/com\/tangem\/tests\//
            is_scenario = file ~ /\/com\/tangem\/scenarios\//
            is_screen = file ~ /\/com\/tangem\/screens\//
            strict = is_test || is_scenario || is_screen
        }
        text ~ /^import[[:space:]]/ && text !~ /truth|common\.R/ { next }
        code ~ /Thread\.sleep|SystemClock\.sleep|(^|[^A-Za-z0-9_.])delay\(/ {
            emit(strict ? "error" : "warn", "no-sleep", "fixed sleeps are forbidden, wait on a condition (awaitSuccess / flakySafely)")
        }
        code ~ /(^|[^A-Za-z0-9_.])assert\(/ {
            emit("error", "no-kotlin-assert", "Kotlin assert() is a no-op on instrumentation runs, use JUnit Assert / Kakao assertions")
        }
        text ~ /^import com\.google\.common\.truth/ {
            emit("error", "no-truth", "Truth is not on the androidTest classpath, use org.junit.Assert")
        }
        text ~ /^import com\.tangem\.common\.R([^A-Za-z0-9_.]|$)/ {
            emit("error", "common-r", "com.tangem.common.R has almost no strings, import com.tangem.core.res.R")
        }
        code ~ /(^|[^A-Za-z0-9_])pullToRefresh\(/ && code !~ /fun[[:space:]]+([A-Za-z0-9_]+\.)?pullToRefresh\(/ && code !~ /^[[:space:]]*(\/\/|\*|\/\*)/ {
            emit("error", "uiautomator-pull-to-refresh", "the UiAutomator pullToRefresh() is a silent no-op on PullToRefreshBox, use a Compose swipeDown gesture")
        }
        text ~ /"""\{|"\{\\"|\{[[:space:]]*\\"[a-zA-Z]+\\"[[:space:]]*:/ {
            emit("error", "json-interpolation", "build JSON with JSONObject().put(...).toString(), not string interpolation")
        }
        text ~ /@Ignore/ && text !~ /atlassian\.net\/browse\/AND-[0-9]+/ {
            emit("error", "ignore-without-ticket", "@Ignore must carry the AND ticket URL: @Ignore(\"https://tangem.atlassian.net/browse/AND-XXXXX\")")
        }
        text ~ /step\("(Tap|Press|Check|Verify) / {
            emit("error", "step-naming", "use \"Click on '"'"'X'"'"' button\" / \"Assert X is displayed\"")
        }
        text ~ /step\("[^"]*(is visible|is not visible|does not exist|is shown|is not shown)/ {
            emit("error", "step-naming", "use \"is displayed\" / \"is not displayed\"")
        }
        code ~ /fetchSemanticsNodes\(\)/ {
            emit((is_test || is_scenario) ? "error" : "warn", "manual-poll", "manual semantics poll, use a Kakao matcher inside awaitSuccess / flakySafely")
        }
        code ~ /\.waitUntil[[:space:]]*[({]/ {
            emit("warn", "manual-wait", "prefer awaitSuccess (scenarios) / flakySafely (test body); keep waitUntil only to retry a mutating action, with a WHY comment")
        }
        is_test && code ~ /composeTestRule|semanticsProvider|onAllNodes\(|onNode\(|hasTestTag\(|TestTags\./ {
            emit("error", "raw-compose-in-test", "the test body calls page objects and scenarios only; move the locator into a page object")
        }
        is_test && code ~ /^[[:space:]]*private[[:space:]]+fun[[:space:]][^(]*\([^)]*\)[[:space:]]*\{/ {
            emit("warn", "private-helper-in-test", "a reused step sequence belongs in scenarios/ as a BaseTestCase extension")
        }
        text ~ /setWireMockScenarioState\([[:space:]]*"/ {
            emit("error", "scenario-literal", "declare the scenario name and state as vals at the top of the test")
        }
        code ~ /resetWireMockScenario/ {
            emit("warn", "scenario-rollback", "setupHooks already resets every scenario before each test; reset only when the reset is part of the case")
        }
        code ~ /assertIs(Not)?Enabled\(|assertHas(No)?ClickAction\(/ {
            emit("warn", "enabled-semantics", "check the target in production: clickable(enabled=false)/disabled() -> assertIsNotEnabled; TangemHoldToConfirmButton exposes neither, prove readiness another way")
        }
        code ~ /if[[:space:]]*\(.*isDisplayedSafely\(/ {
            emit("warn", "conditional-click", "an element that is always present after pm clear needs no if; click it directly")
        }
        code ~ /@OptIn\(ExperimentalTestApi/ {
            emit("warn", "opt-in", "needed only where an experimental API is called directly, not for calling an annotated helper")
        }
        code ~ /WAIT_UNTIL_TIMEOUT_(LONG|VERY_LONG|WALLET_CREATION)/ {
            emit("warn", "long-timeout", "a long ceiling needs a genuinely slow step; the default WAIT_UNTIL_TIMEOUT is 20 s")
        }
        match(code, /[0-9][0-9_]*L?/) {
            n = substr(code, RSTART, RLENGTH); gsub(/[_L]/, "", n)
            if (code ~ /[Tt]imeout/ && n + 0 >= 180000) emit("error", "timeout-over-marathon", "a wait of >= 180 s equals Marathon testOutputTimeoutMillis: the test is killed as incomplete instead of failing")
        }
    ' "$lines_file" >> "$report"
}

# Test method name, @AllureId presence and uniqueness need the whole file, not just the diff.
check_test_methods() {
    local file
    { changed_files | grep -E "^${android_test_dir}.*/com/tangem/tests/.*\.kt$" || true; } | while IFS= read -r file; do
        [ -f "$repo_root/$file" ] || continue
        awk -F '\t' -v f="$file" '
            FNR == NR { if ($1 == f && $3 == "+") added[$2] = 1; next }
            /^[[:space:]]*@AllureId\(/ { allure = 1 }
            /^[[:space:]]*@Test([^A-Za-z]|$)/ { test = 1 }
            /^[[:space:]]*(fun|internal fun|override fun)[[:space:]]/ {
                if (test && (FNR in added)) {
                    name = $0; sub(/^[^f]*fun[[:space:]]+/, "", name); sub(/\(.*/, "", name)
                    if (name ~ /^`/ || name !~ /^[a-z][A-Za-z0-9]*Test$/)
                        printf "%s:%d: [error] test-method-name: %s: camelCase ending with Test, no backticks or GIVEN/WHEN/THEN\n", f, FNR, name
                    if (!allure)
                        printf "%s:%d: [error] missing-allure-id: %s has no @AllureId\n", f, FNR, name
                }
                test = 0; allure = 0
            }
            /^[[:space:]]*$/ { test = 0; allure = 0 }
        ' "$lines_file" "$repo_root/$file" >> "$report"
    done

    awk -F '\t' -v at="$android_test_dir" '$3 == "+" && index($1, at) == 1 && $4 ~ /@AllureId\("[0-9]+"\)/ {
        s = $4; sub(/.*@AllureId\("/, "", s); sub(/".*/, "", s); print s "\t" $1 "\t" $2
    }' "$lines_file" | while IFS=$'\t' read -r id file line; do
        if [ "$(git -C "$repo_root" grep --untracked -hE "@AllureId\(\"$id\"\)" -- "$android_test_dir" | wc -l)" -gt 1 ]; then
            issue "$file" "$line" error duplicate-allure-id "AllureId $id is used by more than one test method"
        fi
    done
}

check_unused_imports() {
    local file
    { changed_files | grep -E "^${android_test_dir}.*\.kt$" || true; } | while IFS= read -r file; do
        [ -f "$repo_root/$file" ] || continue
        awk -v f="$file" '
            FNR == NR { if ($0 !~ /^import /) body = body "\n" $0; next }
            /^import / && $0 !~ /\*$/ {
                name = $0; sub(/^import[[:space:]]+/, "", name)
                if (name ~ / as /) { sub(/.* as[[:space:]]+/, "", name) } else { sub(/.*\./, "", name) }
                sub(/[[:space:]].*/, "", name)
                if (name == "getValue" || name == "setValue") next
                if (body !~ ("(^|[^A-Za-z0-9_])" name "([^A-Za-z0-9_]|$)"))
                    printf "%s:%d: [warn] unused-import: %s\n", f, FNR, $0
            }
        ' "$repo_root/$file" "$repo_root/$file" >> "$report"
    done
}

# Comments in every changed Kotlin file — test, production and mocked source sets alike. The default
# outcome of a comment finding is deleting the comment, not justifying it.
check_comments() {
    awk -F '\t' '
        function emit(level, rule, msg) { printf "%s:%s: [%s] %s: %s\n", file, line, level, rule, msg }
        $1 !~ /\.kt$/ || $3 != "+" { prev_file = ""; next }
        {
            file = $1; line = $2; text = $4
            for (i = 5; i <= NF; i++) text = text "\t" $i
            code = text; gsub(/"([^"\\]|\\.)*"/, "\"\"", code)
            is_comment = code ~ /^[[:space:]]*(\/\/|\/\*|\*)/
            trailing = !is_comment && code ~ /[^[:space:]].*[[:space:]]\/\/[[:space:]]/
        }
        is_comment || trailing {
            body = text
            if (trailing) sub(/^.*[[:space:]]\/\/[[:space:]]*/, "", body)
            else sub(/^[[:space:]]*(\/\/+|\/\*\*?|\*)[[:space:]]*/, "", body)
            sub(/[[:space:]]*\*\/[[:space:]]*$/, "", body)
            emit("warn", "comment", "delete it unless removing it would get the code broken or reverted; try a better name first: " text)
            if (code ~ /^[[:space:]]*\/\*\*/) emit("warn", "kdoc", "a KDoc that paraphrases the name is noise; keep it only for a contract the signature cannot state")
            if (tolower(body) ~ /^(mirrors|see|same as|matches|wraps|helper|stubs|makes|returns|opens|asserts|clicks|waits) /) emit("error", "comment-restates-code", "the comment describes what the code does; delete it or rename the symbol")
            run = (prev_file == file && prev_line == line - 1 && prev_comment) ? run + 1 : 1
            if (run == 2 && body != "") emit("error", "comment-block", "one line max: a comment that needs two lines is a sign the code needs a better name or the note belongs in the PR")
            if (body != "" && length(body) > 15) { seen[body]++; if (seen[body] == 2) emit("error", "comment-duplicate", "the same comment is added twice; keep at most one, or none") }
            prev_comment = 1
        }
        !(is_comment || trailing) { prev_comment = 0 }
        { prev_file = file; prev_line = line }
    ' "$lines_file" >> "$report"
}

# Production code may change only to add test tags; a new tag constant must reach a composable.
check_production() {
    awk -F '\t' -v at="$android_test_dir" '
        $1 !~ /\.kt$/ || index($1, at) == 1 || $1 !~ /\/src\/main\// { next }
        {
            text = $4
            for (i = 5; i <= NF; i++) text = text "\t" $i
        }
        $1 ~ /TestTags[A-Za-z]*\.kt$/ { next }
        text ~ /^[[:space:]]*$/ || text ~ /^[[:space:]]*[)}\],]*[[:space:]]*$/ || text ~ /^import[[:space:]]/ { next }
        tolower(text) ~ /testtag|semantics/ { next }
        { printf "%s:%s: [warn] production-non-tag: %s production line is not a test-tag change: %s\n", $1, $2, ($3 == "+" ? "added" : "removed"), text }
    ' "$lines_file" >> "$report"

    awk -F '\t' -v at="$android_test_dir" '
        $3 == "+" && $1 ~ /TestTags[A-Za-z]*\.kt$/ && index($1, at) != 1 && $4 ~ /(const[[:space:]]+)?val[[:space:]]+[A-Z_][A-Z0-9_]*/ {
            s = $4; sub(/.*val[[:space:]]+/, "", s); match(s, /^[A-Z_][A-Z0-9_]*/); print substr(s, 1, RLENGTH) "\t" $1 "\t" $2
        }
    ' "$lines_file" | while IFS=$'\t' read -r name file line; do
        if ! git -C "$repo_root" grep --untracked -qE "\\.$name([^A-Za-z0-9_]|\$)" -- '*/src/main/*.kt' ":!$file"; then
            issue "$file" "$line" error unapplied-test-tag "'$name' is not applied to any composable"
        fi
        if ! git -C "$repo_root" grep --untracked -qE "(^|[^A-Za-z0-9_])$name([^A-Za-z0-9_]|\$)" -- "$android_test_dir"; then
            issue "$file" "$line" warn unused-test-tag "'$name' is not used by any page object"
        fi
    done
}

check_mocks() {
    local mb status path state line body
    mb="$(merge_base "$mocks_root" "$mocks_base")"
    {
        echo "### tangem-api-mocks ($mocks_root), base $mocks_base"
        full_diff "$mocks_root" "$mb" 5
    } >> "$review_diff"

    {
        git -C "$mocks_root" diff --name-status "$mb" -- mocks
        git -C "$mocks_root" ls-files --others --exclude-standard -- mocks | sed 's/^/A\t/'
    } | while IFS=$'\t' read -r status path _; do
        case "$status" in
            A) ;;
            D*) issue "tangem-api-mocks/$path" 1 error mock-deleted "shared mock deleted; other tests and iOS may use it" ;;
            *) issue "tangem-api-mocks/$path" 1 warn mock-existing-changed "shared mock changed ($status); list every test that uses it and re-run them" ;;
        esac
        if [ "${status:0:1}" != "D" ] && [[ "$path" == *.json ]] && [ -f "$mocks_root/$path" ]; then
            jq empty "$mocks_root/$path" 2> /dev/null || issue "tangem-api-mocks/$path" 1 error mock-invalid-json "file is not valid JSON"
        fi
    done

    full_diff "$mocks_root" "$mb" 0 | diff_lines > "$out_dir/mocks-lines.tsv"

    awk -F '\t' '$3 == "+" && $1 ~ /^mocks\/mappings\// && $4 ~ /"bodyFileName"/ {
        s = $4; sub(/.*"bodyFileName"[[:space:]]*:[[:space:]]*"/, "", s); sub(/".*/, "", s); print s "\t" $1 "\t" $2
    }' "$out_dir/mocks-lines.tsv" | while IFS=$'\t' read -r body path line; do
        [[ "$body" == *"{{"* ]] && continue
        [ -f "$mocks_root/mocks/__files/$body" ] || issue "tangem-api-mocks/$path" "$line" error mock-missing-body "bodyFileName '$body' does not exist under mocks/__files"
    done

    awk -F '\t' '$3 == "-" && $4 ~ /"(requiredScenarioState|newScenarioState)"/ {
        s = $4; sub(/.*"(requiredScenarioState|newScenarioState)"[[:space:]]*:[[:space:]]*"/, "", s); sub(/".*/, "", s)
        print s "\t" $1 "\t" $2
    }' "$out_dir/mocks-lines.tsv" | sort -u | while IFS=$'\t' read -r state path line; do
        if ! grep -rqE "\"(requiredScenarioState|newScenarioState)\"[[:space:]]*:[[:space:]]*\"$state\"" "$mocks_root/mocks/mappings"; then
            issue "tangem-api-mocks/$path" "$line" error mock-removed-state "scenario state '$state' no longer exists in any mapping"
        fi
    done

    awk -F '\t' '$3 == "+" && $4 ~ /"priority"/ {
        p = $4; sub(/.*"priority"[[:space:]]*:[[:space:]]*/, "", p); sub(/[^-0-9].*/, "", p)
        printf "tangem-api-mocks/%s:%s: [warn] mock-priority: new priority %s, check that it does not shadow other mappings of the same endpoint (lower wins)\n", $1, $2, p
    }' "$out_dir/mocks-lines.tsv" >> "$report"

    awk -F '\t' '$1 ~ /^mocks\/mappings\// { files[$1] = 1 }
        $3 == "+" && $4 ~ /exchange-data/ { ed[$1] = $2 }
        $3 == "+" && $4 ~ /request\.query\.requestId/ { echo[$1] = 1 }
        END { for (f in ed) if (!(f in echo)) printf "tangem-api-mocks/%s:%s: [warn] exchange-data-request-id: exchange-data must echo {{request.query.requestId}} and {{request.query.toAddress}}, or every DEX provider silently disappears\n", f, ed[f] }
    ' "$out_dir/mocks-lines.tsv" >> "$report"
}

android_mb="$(merge_base "$repo_root" "$base")"
{
    echo "### tangem-app-android ($repo_root), base $base"
    full_diff "$repo_root" "$android_mb" 5
} >> "$review_diff"
full_diff "$repo_root" "$android_mb" 0 | diff_lines > "$lines_file"

check_test_lines
check_test_methods
check_unused_imports
check_comments
check_production

if git -C "$mocks_root" rev-parse --git-dir > /dev/null 2>&1; then
    check_mocks
else
    echo "check.sh: mocks repo not found at $mocks_root, mock checks skipped" >&2
fi

sort -u -o "$report" "$report"
errors="$(grep -c ': \[error\] ' "$report" || true)"
warnings="$(grep -c ': \[warn\] ' "$report" || true)"

cat "$report"
echo
echo "errors: $errors, warnings: $warnings"
echo "report: $report"
echo "diff for review: $review_diff"

[ "$errors" -eq 0 ]

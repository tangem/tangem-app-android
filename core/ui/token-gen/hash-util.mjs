// Shared hashing helpers for the design-token generators (build-tokens.mjs / build-icons.mjs).
// Kept in one place because the exact byte stream AND file ordering produced here must stay
// identical to the Kotlin verifier in core/ui/build.gradle.kts (VerifyDesignTokensTask):
// any drift silently breaks the token hash gate.

/**
 * Strip every CR (0x0D) byte so the hash ignores CRLF vs LF line endings. The ds-tokens submodule
 * is not covered by the parent repo's .gitattributes, so its .json/.svg sources may be checked out
 * with CRLF on some platforms. Lone CRs are dropped too — safe here because these sources are UTF-8
 * (0x0D never appears inside a multi-byte sequence) and carry no bare CR.
 * Must stay byte-for-byte identical to ByteArray.stripCr() in core/ui/build.gradle.kts.
 */
export function stripCr(buf) {
  const out = Buffer.allocUnsafe(buf.length);
  let n = 0;
  for (let i = 0; i < buf.length; i++) {
    if (buf[i] !== 0x0d) out[n++] = buf[i];
  }
  return out.subarray(0, n);
}

/**
 * Compare two forward-slash relative paths by UTF-16 code unit — identical to Kotlin's
 * String.compareTo used by `sortedBy { …invariantSeparatorsPath }` in the verifier.
 * Do NOT use String.prototype.localeCompare for file ordering: it is locale/ICU-version dependent
 * (differs across machines) and case-insensitive at the primary level (disagrees with the Kotlin
 * code-unit sort) — either would reorder files and change the hash.
 */
export function compareCodeUnits(a, b) {
  return a < b ? -1 : a > b ? 1 : 0;
}

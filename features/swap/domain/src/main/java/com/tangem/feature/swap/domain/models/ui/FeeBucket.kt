package com.tangem.feature.swap.domain.models.ui

/**
 * Domain-level classification of a transaction fee tier.
 *
 * [REDACTED_TASK_KEY] — replaces the role of the legacy `FeeType` enum (which had only `NORMAL` / `PRIORITY`)
 * for swap fee analytics and selector wiring. Maps cleanly to the send-v2 UI's `FeeItem`:
 *
 * | FeeBucket   | FeeItem        |
 * |-------------|----------------|
 * | [SLOW]      | `FeeItem.Slow` (built from `TransactionFee.Choosable.minimum`)  |
 * | [MARKET]    | `FeeItem.Market` (built from `TransactionFee.Choosable.normal` or `TransactionFee.Single.normal`) |
 * | [FAST]      | `FeeItem.Fast` (built from `TransactionFee.Choosable.priority`) |
 * | [SUGGESTED] | `FeeItem.Suggested` (built from `FeeStateConfiguration.Suggestion`) |
 * | [CUSTOM]    | `FeeItem.Custom`                                              |
 *
 * The send-v2 `FeeItem` type is intentionally **not** imported here — the domain layer must not
 * depend on UI types. The mapping above is enforced by a converter in the impl module.
 *
 * Phase 5 of the swap fee redesign will delete `FeeType.getNameForAnalytics()` and route all
 * fee-tier analytics through [toAnalyticsName]. Phase 3 only introduces this enum alongside the
 * legacy one — the legacy `FeeType` and its analytics extension stay until Phase 5.
 */
enum class FeeBucket {
    SLOW,
    MARKET,
    FAST,
    SUGGESTED,
    CUSTOM,
    ;

    /**
     * Returns the human-readable analytics label for this bucket.
     *
     * Values are kept compatible with the labels previously emitted by
     * `FeeType.getNameForAnalytics()` so that downstream analytics reporting does not break when
     * the migration completes:
     *  - [SLOW] → `"Min"`
     *  - [MARKET] → `"Normal"` (same as legacy `FeeType.NORMAL`)
     *  - [FAST] → `"Max"` (same as legacy `FeeType.PRIORITY`)
     *  - [SUGGESTED] → `"Suggested"`
     *  - [CUSTOM] → `"Custom"`
     */
    fun toAnalyticsName(): String = when (this) {
        SLOW -> "Min"
        MARKET -> "Normal"
        FAST -> "Max"
        SUGGESTED -> "Suggested"
        CUSTOM -> "Custom"
    }
}
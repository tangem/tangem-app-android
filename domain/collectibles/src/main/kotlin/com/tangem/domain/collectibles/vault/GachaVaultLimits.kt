package com.tangem.domain.collectibles.vault

/**
 * Spending caps the vault enforces on its `hot_delegate`, in USDC base units (6 decimals).
 *
 * [dailyCap] is a UTC-day bucket, not a rolling window, so up to twice its value can legitimately pass in a
 * 24-hour span straddling midnight. The program requires `0 < perSpinCap <= dailyCap` and rejects the rest with
 * error 6004.
 */
data class GachaVaultLimits(val perSpinCap: Long, val dailyCap: Long) {

    companion object {

        /** 50 000 USDC per spin, 500 000 per UTC day. */
        val Default = GachaVaultLimits(
            perSpinCap = 50_000_000_000L,
            dailyCap = 500_000_000_000L,
        )
    }
}
package com.tangem.features.earn.impl.fake

enum class FakeEarnSessionPreset { Lending, Vault }

/**
 * Switches for scenarios the happy path never reaches.
 *
 * @property isFeeGrowingOnVerify the first `verify` answers `Changed` with a larger fee
 * @property isExecuteFailing `execute` ends with a `Failed` step instead of `Sent` and `Done`
 */
data class FakeEarnSessionBehavior(
    val isFeeGrowingOnVerify: Boolean = false,
    val isExecuteFailing: Boolean = false,
)
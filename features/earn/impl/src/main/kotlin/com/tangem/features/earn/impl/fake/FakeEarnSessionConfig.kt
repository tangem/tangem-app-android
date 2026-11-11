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

/** The withdrawal notices of the fake presets, for a caller that shows them before the session prepares a plan. */
fun fakeWithdrawNotice(preset: FakeEarnSessionPreset): String = when (preset) {
    FakeEarnSessionPreset.Lending -> FAKE_INSTANT_WITHDRAW_NOTICE
    FakeEarnSessionPreset.Vault -> FAKE_QUEUED_WITHDRAW_NOTICE
}

internal const val FAKE_INSTANT_WITHDRAW_NOTICE = "Your funds will be available for use immediately after withdrawal."
internal const val FAKE_QUEUED_WITHDRAW_NOTICE = "Withdrawals settle within 7 days, the funds arrive after that."
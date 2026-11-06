package com.tangem.features.earn.impl.fake

enum class FakeEarnProductPreset { Lending, Staking, Vault }

enum class FakeEarnPosition { None, Known, Unknown }

/**
 * Switches for scenarios the default state never reaches.
 *
 * @property position what the fake product knows about the user's position
 */
data class FakeEarnProductBehavior(
    val position: FakeEarnPosition = FakeEarnPosition.None,
)
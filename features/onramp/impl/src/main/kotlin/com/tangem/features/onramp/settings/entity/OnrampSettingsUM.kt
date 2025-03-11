package com.tangem.features.onramp.settings.entity

import kotlinx.collections.immutable.ImmutableList

internal data class OnrampSettingsUM(
    val onBack: () -> Unit,
    val items: ImmutableList<OnrampSettingsItemUM>,
)
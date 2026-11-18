package com.tangem.feature.wallet.presentation.wallet.state.model

import androidx.compose.runtime.Immutable

/** The badge above the balance of a mobile wallet that has once held $5,000; it leads to the hardware upgrade */
@Immutable
internal data class PermanentUpgradeBannerUM(val onClick: () -> Unit)
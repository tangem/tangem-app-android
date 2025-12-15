package com.tangem.features.createwalletstart.entity

import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class CreateWalletStartUM(
    val title: TextReference,
    val description: TextReference,
    val featureItems: ImmutableList<FeatureItem>,
    val imageResId: Int,
    val isScanInProgress: Boolean,
    val shouldShowScanSecondaryButton: Boolean,
    val primaryButtonText: TextReference,
    val onPrimaryButtonClick: () -> Unit,
    val otherMethodDescription: TextReference,
    val otherMethodTitle: TextReference,
    val otherMethodClick: () -> Unit,
    val onScanClick: () -> Unit,
    val onBackClick: () -> Unit,
) {
    data class FeatureItem(
        val iconResId: Int,
        val text: TextReference,
    )
}
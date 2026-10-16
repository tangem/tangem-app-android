package com.tangem.features.yield.supply.impl.promobottomsheet.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class YieldModePromoBottomSheetUM(
    val title: TextReference,
    val tokensCount: TextReference,
    val tokenIconUrls: ImmutableList<String>,
    val onShown: () -> Unit,
    val onCloseClick: () -> Unit,
    val onBack: () -> Unit,
    val onDismissRequest: () -> Unit,
    val onTryClick: () -> Unit,
) : TangemBottomSheetConfigContent
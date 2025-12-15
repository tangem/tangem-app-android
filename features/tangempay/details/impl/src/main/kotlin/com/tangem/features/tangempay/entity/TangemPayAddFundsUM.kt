package com.tangem.features.tangempay.entity

import androidx.annotation.DrawableRes
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayAddFundsUM(
    val items: ImmutableList<TangemPayAddFundsItemUM>,
    val dismiss: () -> Unit,
    val errorMessage: MessageBottomSheetUMV2?,
)

internal data class TangemPayAddFundsItemUM(
    @DrawableRes val iconRes: Int,
    val title: TextReference,
    val description: TextReference,
    val onClick: () -> Unit,
)
package com.tangem.features.tangempay.entity

import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class TangemPayAddFundsUM(
    val items: ImmutableList<TangemPayAddFundsItemUM>,
    val dismiss: () -> Unit,
    val errorMessage: MessageBottomSheetUM?,
)

internal data class TangemPayAddFundsItemUM(
    val icon: TangemIconUM,
    val title: TextReference,
    val description: TextReference,
    val onClick: () -> Unit,
)
package com.tangem.features.details.entity

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import kotlinx.collections.immutable.ImmutableList

internal data class DetailsUM(
    val items: ImmutableList<DetailsItemUM>,
    val footer: DetailsFooterUM,
    val selectFeedbackEmailTypeBSConfig: TangemBottomSheetConfig,
    val popBack: () -> Unit,
)
package com.tangem.features.earn.api.ui

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

sealed interface EarnRowInfoUM : TangemBottomSheetConfigContent {

    val title: TextReference

    data class Text(override val title: TextReference, val text: TextReference) : EarnRowInfoUM

    data class Items(override val title: TextReference, val items: ImmutableList<Item>) : EarnRowInfoUM

    data class Item(val title: TextReference, val description: TextReference)
}
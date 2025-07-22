package com.tangem.features.swap.v2.impl.choosetoken.fromSupported.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

internal data class SwapChooseTokenNetworkUM(
    val bottomSheetConfig: TangemBottomSheetConfig,
)

@Immutable
internal sealed class SwapChooseTokenNetworkContentUM : TangemBottomSheetConfigContent {

    abstract val messageContent: MessageBottomSheetUMV2

    data class Loading(
        override val messageContent: MessageBottomSheetUMV2,
    ) : SwapChooseTokenNetworkContentUM()

    data class Error(
        override val messageContent: MessageBottomSheetUMV2,
    ) : SwapChooseTokenNetworkContentUM()

    data class Content(
        override val messageContent: MessageBottomSheetUMV2,
        val swapNetworks: ImmutableList<SwapChooseNetworkUM>,
    ) : SwapChooseTokenNetworkContentUM()
}

internal data class SwapChooseNetworkUM(
    val title: TextReference,
    val subtitle: TextReference,
    @DrawableRes val iconResId: Int,
    val hasFixedRate: Boolean,
    val onNetworkClick: () -> Unit,
)
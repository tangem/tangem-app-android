package com.tangem.feature.wallet.child.tokenActions

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.core.ui.ds.row.token.TangemTokenRowUM
import com.tangem.feature.wallet.presentation.wallet.state.model.TokenActionButtonUM
import kotlinx.collections.immutable.ImmutableList

internal interface TokenActionsComponent : ComposableBottomSheetComponent, ComposableContentComponent {
    data class Params(
        val actions: ImmutableList<TokenActionButtonUM>,
        val tokenRowUM: TangemTokenRowUM?,
        val offsetX: Float,
        val offsetY: Float,
        val onDismiss: () -> Unit,
    )

    interface Factory : ComponentFactory<Params, TokenActionsComponent>
}
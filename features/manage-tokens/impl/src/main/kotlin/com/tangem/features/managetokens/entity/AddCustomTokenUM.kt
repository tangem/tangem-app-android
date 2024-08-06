package com.tangem.features.managetokens.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.tokens.model.Network

@Immutable
internal sealed class AddCustomTokenUM : TangemBottomSheetConfigContent {

    abstract val selectedNetwork: SelectedNetworkUM?
    abstract val addTokenButton: AddCustomTokenButtonUM

    abstract val popBack: () -> Unit

    data class NetworkSelector(
        override val selectedNetwork: SelectedNetworkUM? = null,
        override val popBack: () -> Unit,
    ) : AddCustomTokenUM() {

        override val addTokenButton: AddCustomTokenButtonUM = AddCustomTokenButtonUM.Hidden
    }

    data class Form(
        override val selectedNetwork: SelectedNetworkUM,
        override val addTokenButton: AddCustomTokenButtonUM.Visible,
        override val popBack: () -> Unit,
    ) : AddCustomTokenUM()
}

@Immutable
internal data class SelectedNetworkUM(
    val id: Network.ID,
    val name: String,
)

@Immutable
internal sealed class AddCustomTokenButtonUM {

    open val onClick: () -> Unit = {}

    open val isEnabled: Boolean = false

    val isVisible: Boolean
        get() = this is Visible

    data object Hidden : AddCustomTokenButtonUM() {
        override val onClick: () -> Unit = {}
    }

    data class Visible(
        override val isEnabled: Boolean,
        override val onClick: () -> Unit,
    ) : AddCustomTokenButtonUM()
}

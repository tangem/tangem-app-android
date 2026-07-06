package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.utils.transformer.Transformer

/**
 * Wires the callbacks owned by [com.tangem.features.addressbook.addaddress.model.AddAddressModel] into the initial
 * state produced by [com.tangem.features.addressbook.addaddress.state.AddAddressStateController].
 */
internal class UpdateAddAddressInitialStateTransformer(
    private val intents: Intents,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        return prevState.copy(
            onAddressChange = intents.onAddressChange,
            onAddressClear = intents.onAddressClear,
            onPasteClick = intents.onPasteClick,
            onQrClick = intents.onQrClick,
            onBackClick = intents.onBackClick,
            onNetworkClick = intents.onNetworkClick,
            memoField = prevState.memoField.copy(
                onValueChange = intents.onMemoChange,
                onPasteClick = intents.onMemoPasteClick,
            ),
            buttonUM = prevState.buttonUM.copy(onClick = intents.onConfirmClick),
        )
    }

    data class Intents(
        val onAddressChange: (String) -> Unit,
        val onAddressClear: () -> Unit,
        val onPasteClick: () -> Unit,
        val onQrClick: () -> Unit,
        val onBackClick: () -> Unit,
        val onNetworkClick: () -> Unit,
        val onMemoChange: (String) -> Unit,
        val onMemoPasteClick: () -> Unit,
        val onConfirmClick: () -> Unit,
    )
}
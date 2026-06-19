package com.tangem.features.addressbook.addaddress.state.transformers

import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.utils.transformer.Transformer

/**
 * Wires the callbacks owned by [com.tangem.features.addressbook.addaddress.model.AddAddressModel] into the initial
 * state produced by [com.tangem.features.addressbook.addaddress.state.AddAddressStateController].
 */
internal class UpdateAddAddressInitialStateTransformer(
    private val onAddressChange: (String) -> Unit,
    private val onAddressClear: () -> Unit,
    private val onPasteClick: () -> Unit,
    private val onQrClick: () -> Unit,
    private val onBackClick: () -> Unit,
    private val onConfirmClick: () -> Unit,
) : Transformer<AddAddressUM> {

    override fun transform(prevState: AddAddressUM): AddAddressUM {
        return prevState.copy(
            onAddressChange = onAddressChange,
            onAddressClear = onAddressClear,
            onPasteClick = onPasteClick,
            onQrClick = onQrClick,
            onBackClick = onBackClick,
            buttonUM = prevState.buttonUM.copy(onClick = onConfirmClick),
        )
    }
}
package com.tangem.features.addressbook.addaddress.state

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.addressbook.addaddress.ui.state.AddAddressUM
import com.tangem.features.addressbook.addaddress.ui.state.AddressFieldUM
import com.tangem.utils.transformer.Transformer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@ModelScoped
internal class AddAddressStateController @Inject constructor() {

    val uiState: StateFlow<AddAddressUM>
        field = MutableStateFlow(value = getInitialState())

    fun update(transformer: Transformer<AddAddressUM>) {
        uiState.update(function = transformer::transform)
    }

    private fun getInitialState(): AddAddressUM = AddAddressUM(
        addressField = AddressFieldUM(
            value = "",
            placeholder = resourceReference(R.string.address_book_enter_address),
            label = resourceReference(R.string.common_address),
            isError = false,
        ),
        buttonUM = TangemButtonUM(
            text = TextReference.Res(R.string.address_book_add_address),
            type = TangemButtonType.Primary,
            isEnabled = false,
            onClick = {},
        ),
        chosenNetworkStateUM = AddAddressUM.ChosenNetworkStateUM.Empty,
        onAddressChange = {},
        onAddressClear = {},
        onPasteClick = {},
        onQrClick = {},
        onBackClick = {},
        onNetworkClick = {},
    )
}
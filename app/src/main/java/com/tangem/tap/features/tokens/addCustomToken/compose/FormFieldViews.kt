package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.domain.common.form.Field
import com.tangem.domain.features.addCustomToken.TokenBlockchainField
import com.tangem.domain.features.addCustomToken.TokenDerivationPathField
import com.tangem.domain.features.addCustomToken.TokenField
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.redux.domainStore
import com.tangem.tap.common.compose.*
import com.tangem.tap.common.extensions.getFromClipboard
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
fun TokenContractAddressView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        fieldData = tokenField.data,
        labelId = R.string.custom_token_contract_address_input_title,
        placeholder = "0x0000000000000000000000000000000000000000",
        isEnabled = screenFieldData.viewState.isEnabled,
        isLoading = screenFieldData.viewState.isLoading,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
//        trailingIcon = { PasteClearButton(showFirst = tokenField.data.value.isEmpty()) }
    ) {
        domainStore.dispatch(OnTokenContractAddressChanged(Field.Data(it, true)))
    }
    SpacerH8()
}

@Composable
private fun PasteClearButton(
    showFirst: Boolean,
) {
    if (showFirst) {
        val context = LocalContext.current
        PasteButton(
            onClick = {
                context.getFromClipboard()?.let {
                    val fieldData = Field.Data(it.toString(), false)
                    domainStore.dispatch(OnTokenContractAddressChanged(fieldData))
                }

            }
        )
    } else {
        ClearButton(
            onClick = {
                val fieldData = Field.Data("", false)
                domainStore.dispatch(OnTokenContractAddressChanged(fieldData))
            }
        )
    }
}

@Composable
fun TokenNameView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        fieldData = tokenField.data,
        labelId = R.string.custom_token_name_input_title,
        placeholderId = R.string.custom_token_name_input_placeholder,
        isEnabled = screenFieldData.viewState.isEnabled,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
    ) {
        domainStore.dispatch(OnTokenNameChanged(Field.Data(it, true)))
    }
    SpacerH8()
}

@Composable
fun TokenNetworkView(screenFieldData: ScreenFieldData, state: AddCustomTokenState) {
    if (!screenFieldData.viewState.isVisible) return

    val notSelected = stringResource(id = R.string.custom_token_network_input_not_selected)
    val networkField = screenFieldData.field as TokenBlockchainField

    BlockchainSpinner(
        title = R.string.custom_token_network_input_title,
        itemList = networkField.itemList,
        selectedItem = networkField.data,
        isEnabled = screenFieldData.viewState.isEnabled,
        textFieldConverter = { state.blockchainToName(it) ?: notSelected },
    ) { domainStore.dispatch(OnTokenNetworkChanged(Field.Data(it, true))) }
    SpacerH8()
}

@Composable
fun TokenSymbolView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        fieldData = tokenField.data,
        labelId = R.string.custom_token_token_symbol_input_title,
        placeholderId = R.string.custom_token_token_symbol_input_placeholder,
        isEnabled = screenFieldData.viewState.isEnabled,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
    ) { domainStore.dispatch(OnTokenSymbolChanged(Field.Data(it, true))) }
    SpacerH8()
}

@Composable
fun TokenDecimalsView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        fieldData = tokenField.data,
        labelId = R.string.custom_token_decimals_input_title,
        placeholder = "8",
        isEnabled = screenFieldData.viewState.isEnabled,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    ) { domainStore.dispatch(OnTokenDecimalsChanged(Field.Data(it, true))) }
    SpacerH8()
}

@Composable
fun TokenDerivationPathView(screenFieldData: ScreenFieldData, state: AddCustomTokenState) {
    if (!screenFieldData.viewState.isVisible) return

    val notSelected = stringResource(id = R.string.custom_token_derivation_path_default)
    val networkField = screenFieldData.field as TokenDerivationPathField

    BlockchainSpinner(
        title = R.string.custom_token_derivation_path_input_title,
        itemList = networkField.itemList,
        selectedItem = networkField.data,
        isEnabled = screenFieldData.viewState.isEnabled,
        textFieldConverter = { state.blockchainToName(it) ?: notSelected },
        dropdownItemView = { blockchain ->
            val derivationPathName = state.blockchainToName(blockchain, true) ?: notSelected
            val blockchainName = state.blockchainToName(blockchain) ?: notSelected
            TitleSubtitle(derivationPathName, blockchainName)
        }
    ) { domainStore.dispatch(OnTokenDerivationPathChanged(Field.Data(it, true))) }
    SpacerH8()
}

@Preview
@Composable
fun TestTokenContractAddressView() {

}
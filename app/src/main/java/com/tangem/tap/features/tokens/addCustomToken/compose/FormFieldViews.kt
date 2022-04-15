package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.domain.common.form.Field
import com.tangem.domain.features.addCustomToken.TokenBlockchainField
import com.tangem.domain.features.addCustomToken.TokenDerivationPathField
import com.tangem.domain.features.addCustomToken.TokenField
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.redux.domainStore
import com.tangem.tap.common.compose.BlockchainSpinner
import com.tangem.tap.common.compose.OutlinedTextFieldWidget
import com.tangem.tap.common.compose.SpacerH8
import com.tangem.tap.common.compose.TitleSubtitle
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
@Composable
fun TokenContractAddressView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        textFieldData = tokenField.data,
        labelId = R.string.custom_token_contract_address_input_title,
        placeholder = "0x0000000000000000",
        isEnabled = screenFieldData.viewState.isEnabled,
        isLoading = screenFieldData.viewState.isLoading,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
    ) {
        domainStore.dispatch(AddCustomTokenAction.OnTokenContractAddressChanged(Field.Data(it)))
    }
    SpacerH8()
}

@Composable
fun TokenNameView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        textFieldData = tokenField.data,
        labelId = R.string.custom_token_name_input_title,
        placeholderId = R.string.custom_token_name_input_placeholder,
        isEnabled = screenFieldData.viewState.isEnabled,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
    ) {
        domainStore.dispatch(AddCustomTokenAction.OnTokenNameChanged(Field.Data(it)))
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
        textFieldConverter = { state.convertBlockchainName(it, notSelected) },
    ) { domainStore.dispatch(AddCustomTokenAction.OnTokenNetworkChanged(Field.Data(it))) }
    SpacerH8()
}

@Composable
fun TokenSymbolView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        textFieldData = tokenField.data,
        labelId = R.string.custom_token_token_symbol_input_title,
        placeholderId = R.string.custom_token_token_symbol_input_placeholder,
        isEnabled = screenFieldData.viewState.isEnabled,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
    ) { domainStore.dispatch(AddCustomTokenAction.OnTokenSymbolChanged(Field.Data(it))) }
    SpacerH8()
}

@Composable
fun TokenDecimalsView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        textFieldData = tokenField.data,
        labelId = R.string.custom_token_decimals_input_title,
        placeholder = "8",
        isEnabled = screenFieldData.viewState.isEnabled,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
    ) { domainStore.dispatch(AddCustomTokenAction.OnTokenDecimalsChanged(Field.Data(it))) }
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
        textFieldConverter = { state.convertBlockchainName(it, notSelected) },
        dropdownItemView = { blockchain ->
            val derivationPathLabel = state.convertDerivationPathLabel(blockchain, notSelected)
            val blockchainName = state.convertBlockchainName(blockchain, notSelected)
            TitleSubtitle(derivationPathLabel, blockchainName)
        }
    ) { domainStore.dispatch(AddCustomTokenAction.OnTokenDerivationPathChanged(Field.Data(it))) }
    SpacerH8()
}
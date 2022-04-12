package com.tangem.tap.features.tokens.addCustomToken.compose

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Scaffold
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.rememberScaffoldState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.domain.ErrorConverter
import com.tangem.domain.common.form.DataField
import com.tangem.domain.common.form.Field
import com.tangem.domain.common.form.FieldId
import com.tangem.domain.features.addCustomToken.*
import com.tangem.domain.features.addCustomToken.CustomTokenFieldId.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction.*
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenState
import com.tangem.domain.features.addCustomToken.redux.ScreenState
import com.tangem.domain.features.addCustomToken.redux.ViewStates
import com.tangem.domain.redux.domainStore
import com.tangem.tap.common.compose.ComposeDialogManager
import com.tangem.tap.common.compose.OutlinedTextFieldWidget
import com.tangem.tap.common.compose.SpacerH8
import com.tangem.tap.common.compose.keyboardObserverAsState
import com.tangem.tap.features.tokens.addCustomToken.CustomTokenErrorConverter
import com.tangem.tap.features.tokens.addCustomToken.CustomTokenWarningConverter
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
private class AddCustomTokenScreen {} // for simple search

@Composable
fun AddCustomTokenScreen(state: MutableState<AddCustomTokenState>) {
    val scaffoldState = rememberScaffoldState()

    Scaffold(
        scaffoldState = scaffoldState,
        backgroundColor = colorResource(id = R.color.backgroundLightGray),
    ) {
        Box(Modifier.fillMaxSize()) {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 80.dp)
            ) {
                item {
                    Surface(
                        modifier = Modifier.padding(16.dp),
                        shape = RoundedCornerShape(4.dp),
                        elevation = 4.dp,
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            FormFields(state)
                        }
                    }
                }
                item { Warnings(state.value.warnings.toList()) }
            }
            HangingOverKeyboardView(
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                keyboardState = keyboardObserverAsState(),
                defaultBottomPadding = 30.dp,
                spaceBetweenKeyboard = 20.dp,
            ) {
                AddButton(
                    isEnabled = state.value.screenState.addButton.isEnabled
                ) {
                }
            }
        }
        ComposeDialogManager()
    }

    LaunchedEffect(key1 = Unit, block = { domainStore.dispatch(AddCustomTokenAction.OnCreate) })
    DisposableEffect(key1 = Unit, effect = { onDispose { domainStore.dispatch(AddCustomTokenAction.OnDestroy) } })
}

@Composable
private fun FormFields(state: MutableState<AddCustomTokenState>) {
    val context = LocalContext.current
    val errorConverter = remember { CustomTokenErrorConverter(context) }

    state.value.form.fieldList.forEach { field ->
        val data = ScreenFieldData.fromState(field, state.value, errorConverter)
        when (field.id) {
            ContractAddress -> TokenContractAddressView(data)
            Network -> TokenNetworkView(data)
            Name -> TokenNameView(data)
            Symbol -> TokenSymbolView(data)
            Decimals -> TokenDecimalsView(data)
            DerivationPath -> TokenDerivationPathView(data)
        }
    }
}

@Composable
private fun TokenContractAddressView(screenFieldData: ScreenFieldData) {
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
        domainStore.dispatch(OnTokenContractAddressChanged(Field.Data(it)))
    }
    SpacerH8()
}

@Composable
private fun TokenNameView(screenFieldData: ScreenFieldData) {
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
        domainStore.dispatch(OnTokenFieldChanged(screenFieldData.field.id, Field.Data(it)))
    }
    SpacerH8()
}

@Composable
private fun TokenNetworkView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val notSelected = stringResource(id = R.string.custom_token_network_input_not_selected)
    val networkField = screenFieldData.field as TokenNetworkField

    TokenNetworkSpinner(
        title = R.string.custom_token_network_input_title,
        itemList = networkField.itemList,
        selectedItem = networkField.data,
        isEnabled = screenFieldData.viewState.isEnabled,
        itemNameConverter = { AddCustomTokenState.convertBlockchainName(it, notSelected) },
    ) { domainStore.dispatch(OnTokenNetworkChanged(Field.Data(it))) }
    SpacerH8()
}

@Composable
private fun TokenSymbolView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val tokenField = screenFieldData.field as TokenField

    OutlinedTextFieldWidget(
        textFieldData = tokenField.data,
        labelId = R.string.custom_token_token_symbol_input_title,
        placeholderId = R.string.custom_token_token_symbol_input_placeholder,
        isEnabled = screenFieldData.viewState.isEnabled,
        error = screenFieldData.error,
        errorConverter = screenFieldData.errorConverter,
    ) { domainStore.dispatch(OnTokenFieldChanged(screenFieldData.field.id, Field.Data(it))) }
    SpacerH8()
}

@Composable
private fun TokenDecimalsView(screenFieldData: ScreenFieldData) {
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
    ) { domainStore.dispatch(OnTokenDecimalsChanged(Field.Data(it))) }
    SpacerH8()
}

@Composable
private fun TokenDerivationPathView(screenFieldData: ScreenFieldData) {
    if (!screenFieldData.viewState.isVisible) return

    val notSelected = stringResource(id = R.string.custom_token_derivation_path_default)
    val networkField = screenFieldData.field as TokenDerivationPathField

    TokenNetworkSpinner(
        title = R.string.custom_token_network_input_title,
        itemList = networkField.itemList,
        selectedItem = networkField.data,
        isEnabled = screenFieldData.viewState.isEnabled,
        itemNameConverter = { AddCustomTokenState.convertDerivationPathName(it, notSelected) },
    ) { domainStore.dispatch(OnTokenDerivationPathChanged(Field.Data(it))) }
    SpacerH8()
}

@Composable
private fun Warnings(warnings: List<AddCustomTokenWarning>) {
    if (warnings.isEmpty()) return

    val context = LocalContext.current
    val warningConverter = remember { CustomTokenWarningConverter(context) }

    Column {
        warnings.forEachIndexed { index, item ->
            val modifier = when (index) {
                0 -> Modifier.padding(16.dp, 0.dp, 16.dp, 16.dp)
                warnings.lastIndex -> Modifier.padding(16.dp, 16.dp, 16.dp, 16.dp)
                else -> Modifier.padding(16.dp, 16.dp, 16.dp, 0.dp)
            }
            Surface(
                modifier = modifier.fillMaxWidth(),
                shape = RoundedCornerShape(4.dp),
                color = colorResource(id = R.color.darkGray2),
                contentColor = colorResource(id = R.color.darkGray3)
            ) {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = warningConverter.convertError(item),
                    color = colorResource(id = R.color.lightGray0),
                    fontSize = 14.sp
                )
            }
        }
    }
}

private data class ScreenFieldData(
    val field: DataField<*>,
    val error: AddCustomTokenError?,
    val errorConverter: ErrorConverter<String>,
    val viewState: ViewStates.TokenField
) {
    companion object {
        fun fromState(
            field: DataField<*>,
            state: AddCustomTokenState,
            errorConverter: CustomTokenErrorConverter
        ): ScreenFieldData {
            return ScreenFieldData(
                field = field,
                error = state.getError(field.id),
                errorConverter = errorConverter,
                viewState = selectField(field.id, state.screenState)
            )
        }

        private fun selectField(id: FieldId, screenState: ScreenState): ViewStates.TokenField {
            return when (id) {
                ContractAddress -> screenState.contractAddressField
                Network -> screenState.network
                Name -> screenState.name
                Symbol -> screenState.symbol
                Decimals -> screenState.decimals
                DerivationPath -> screenState.derivationPath
                else -> throw UnsupportedOperationException()
            }
        }
    }
}
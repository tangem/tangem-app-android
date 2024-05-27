package com.tangem.managetokens.presentation.addcustomtoken.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.Keyboard
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.keyboardAsState
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.ui.ChooseWalletBottomSheet
import com.tangem.managetokens.presentation.common.ui.ChooseWalletBottomSheetConfig
import com.tangem.managetokens.presentation.common.ui.EventEffect
import com.tangem.managetokens.presentation.common.ui.components.Alert
import com.tangem.managetokens.presentation.common.ui.components.SimpleSelectionBlock
import com.tangem.managetokens.presentation.addcustomtoken.state.AddCustomTokenState
import com.tangem.managetokens.presentation.addcustomtoken.state.CustomTokenData
import com.tangem.managetokens.presentation.addcustomtoken.state.TextFieldState
import com.tangem.managetokens.presentation.addcustomtoken.state.previewdata.AddCustomTokenPreviewData

@Composable
internal fun AddCustomTokenScreen(state: AddCustomTokenState, modifier: Modifier = Modifier) {
    var alertState by remember { mutableStateOf<AlertState?>(value = null) }

    EventEffect(
        event = state.event,
        onAlertStateSet = { alertState = it },
    )
    alertState?.let {
        Alert(state = it, onDismiss = { alertState = null })
    }
    Content(state = state, modifier = modifier)
}

@Composable
private fun Content(state: AddCustomTokenState, modifier: Modifier = Modifier) {
    val keyboard by keyboardAsState()

    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
            .padding(
                top = TangemTheme.dimens.spacing10,
                start = TangemTheme.dimens.spacing16,
                end = TangemTheme.dimens.spacing16,
                bottom = TangemTheme.dimens.spacing18,
            )
            .fillMaxWidth(),
    ) {
        Text(
            text = stringResource(id = R.string.manage_tokens_network_selector_title),
            color = TangemTheme.colors.text.primary1,
            style = TangemTheme.typography.subtitle1,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TangemTheme.dimens.spacing10),
        )
        Text(
            text = stringResource(id = R.string.custom_token_subtitle),
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.caption2,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = TangemTheme.dimens.spacing16),
        )
        CustomTokenItemsList(state = state)
        if (keyboard is Keyboard.Closed) {
            PrimaryButton(
                text = stringResource(id = R.string.custom_token_add_token),
                onClick = state.addTokenButton.onClick,
                enabled = state.addTokenButton.isEnabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }

    if (state.chooseWalletState is ChooseWalletState.Choose && state.chooseWalletState.show) {
        val config = TangemBottomSheetConfig(
            isShow = true,
            content = ChooseWalletBottomSheetConfig(state.chooseWalletState),
            onDismissRequest = state.chooseWalletState.onCloseChoosingWalletClick,
        )
        ChooseWalletBottomSheet(config)
    }
}

@Composable
private fun ColumnScope.CustomTokenItemsList(state: AddCustomTokenState, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier
            .fillMaxWidth()
            .weight(1f),
    ) {
        if (state.chooseWalletState is ChooseWalletState.Choose) {
            item {
                SimpleSelectionBlock(
                    title = stringResource(id = R.string.manage_tokens_network_selector_wallet),
                    subtitle = state.chooseWalletState.selectedWallet?.walletName ?: "",
                    onClick = state.chooseWalletState.onChooseWalletClick,
                    modifier = Modifier
                        .padding(bottom = TangemTheme.dimens.spacing12),
                )
            }
        }
        item {
            SimpleSelectionBlock(
                title = stringResource(id = R.string.custom_token_network_input_title),
                subtitle = state.chooseNetworkState.selectedNetwork?.name
                    ?: stringResource(id = R.string.manage_tokens_network_selector_title),
                onClick = state.chooseNetworkState.onChooseNetworkClick,
                modifier = Modifier
                    .padding(bottom = TangemTheme.dimens.spacing12),
            )
        }
        if (state.tokenData != null) {
            item {
                TokenFields(
                    state = state.tokenData,
                    modifier = Modifier
                        .padding(bottom = TangemTheme.dimens.spacing12),
                )
            }
        }
        if (state.chooseDerivationState != null) {
            item {
                SimpleSelectionBlock(
                    title = stringResource(id = R.string.custom_token_derivation_path),
                    subtitle = state.chooseDerivationState.selectedDerivation?.path
                        ?: stringResource(id = R.string.custom_token_derivation_path_default),
                    onClick = state.chooseDerivationState.onChooseDerivationClick,
                    modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing12),
                )
            }
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun TokenFields(state: CustomTokenData, modifier: Modifier = Modifier) {
    val focusManager = LocalFocusManager.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(TangemTheme.dimens.radius16))
            .background(color = TangemTheme.colors.background.action),
    ) {
        TokenField(
            textFieldState = state.contractAddressTextField,
            placeholder = "0x0000000000000000000000000000000",
            title = R.string.custom_token_contract_address_input_title,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = if (state.isNameSymbolDecimalsDisabled()) {
                    ImeAction.Done
                } else {
                    ImeAction.Next
                },
            ),
            onImeAction = {
                if (state.isNameSymbolDecimalsDisabled()) {
                    focusManager.moveFocus(FocusDirection.Exit)
                } else {
                    focusManager.moveFocus(FocusDirection.Down)
                }
            },
        )
        TokenField(
            textFieldState = state.nameTextField,
            placeholder = stringResource(id = R.string.custom_token_name_input_placeholder),
            title = R.string.custom_token_name_input_title,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )
        TokenField(
            textFieldState = state.symbolTextField,
            placeholder = stringResource(id = R.string.custom_token_token_symbol_input_placeholder),
            title = R.string.custom_token_token_symbol_input_title,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Next,
            ),
            onImeAction = { focusManager.moveFocus(FocusDirection.Down) },
        )
        TokenField(
            textFieldState = state.decimalsTextField,
            placeholder = "0",
            title = R.string.custom_token_decimals_input_title,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done,
            ),
            onImeAction = { focusManager.moveFocus(FocusDirection.Exit) },
        )
    }
}

@Composable
private fun TokenField(
    textFieldState: TextFieldState,
    placeholder: String,
    title: Int,
    onImeAction: () -> Unit,
    keyboardOptions: KeyboardOptions,
) {
    Column(
        modifier = Modifier
            .background(color = TangemTheme.colors.background.action)
            .padding(TangemTheme.dimens.spacing16),
    ) {
        TokenTextFieldTitle(
            state = textFieldState,
            title = stringResource(id = title),
        )
        when (textFieldState) {
            is TextFieldState.Editable -> TokenTextField(
                state = textFieldState,
                placeholder = placeholder,
                onImeAction = onImeAction,
                keyboardOptions = keyboardOptions,
            )
            is TextFieldState.Loading -> TokenShimmer()
        }
    }
}

@Composable
private fun TokenShimmer() {
    RectangleShimmer(
        radius = TangemTheme.dimens.radius3,
        modifier = Modifier
            .padding(vertical = TangemTheme.dimens.spacing4)
            .size(
                height = TangemTheme.dimens.size12,
                width = TangemTheme.dimens.size90,
            ),
    )
}

@Composable
private fun TokenTextFieldTitle(state: TextFieldState?, title: String) {
    val modifier = Modifier.padding(bottom = TangemTheme.dimens.spacing4)
    val error = (state as? TextFieldState.Editable)?.error
    if (error == null) {
        Text(
            text = title,
            color = TangemTheme.colors.text.secondary,
            style = TangemTheme.typography.subtitle1,
            modifier = modifier,
        )
    } else {
        Text(
            text = error.title.resolveReference(),
            color = TangemTheme.colors.text.warning,
            style = TangemTheme.typography.subtitle1,
            modifier = modifier,
        )
    }
}

@Preview
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ChooseDerivationScreen() {
    TangemThemePreview {
        AddCustomTokenScreen(state = AddCustomTokenPreviewData.state)
    }
}
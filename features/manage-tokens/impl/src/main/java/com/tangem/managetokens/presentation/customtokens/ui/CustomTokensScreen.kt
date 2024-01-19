package com.tangem.managetokens.presentation.customtokens.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.RectangleShimmer
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.managetokens.impl.R
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.ui.ChooseWalletBottomSheet
import com.tangem.managetokens.presentation.common.ui.ChooseWalletBottomSheetConfig
import com.tangem.managetokens.presentation.common.ui.EventEffect
import com.tangem.managetokens.presentation.common.ui.components.Alert
import com.tangem.managetokens.presentation.common.ui.components.SimpleSelectionBlock
import com.tangem.managetokens.presentation.customtokens.state.AddCustomTokenState
import com.tangem.managetokens.presentation.customtokens.state.CustomTokenData
import com.tangem.managetokens.presentation.customtokens.state.TextFieldState
import com.tangem.managetokens.presentation.customtokens.state.previewdata.AddCustomTokenPreviewData

@Composable
internal fun CustomTokensScreen(state: AddCustomTokenState, modifier: Modifier = Modifier) {
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
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .statusBarsPadding()
            .navigationBarsPadding()
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
        PrimaryButton(
            text = stringResource(id = R.string.custom_token_add_token),
            onClick = state.addTokenButton.onClick,
            enabled = state.addTokenButton.isEnabled,
            modifier = Modifier.fillMaxWidth(),
        )
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

@Composable
private fun TokenFields(state: CustomTokenData, modifier: Modifier = Modifier) {
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
        )
        TokenField(
            textFieldState = state.nameTextField,
            placeholder = stringResource(id = R.string.custom_token_name_input_placeholder),
            title = R.string.custom_token_name_input_title,
        )
        TokenField(
            textFieldState = state.symbolTextField,
            placeholder = stringResource(id = R.string.custom_token_token_symbol_input_placeholder),
            title = R.string.custom_token_token_symbol_input_title,
        )
        TokenField(
            textFieldState = state.decimalsTextField,
            placeholder = "0",
            title = R.string.custom_token_decimals_input_title,
        )
    }
}

@Composable
private fun TokenField(
    textFieldState: TextFieldState,
    placeholder: String,
    title: Int,
    keyboardType: KeyboardType = KeyboardType.Text,
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
                keyboardType = keyboardType,
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
@Composable
private fun Preview_ChooseDerivationScreen_Light() {
    TangemTheme(isDark = false) {
        CustomTokensScreen(state = AddCustomTokenPreviewData.state)
    }
}

@Preview
@Composable
private fun Preview_ChooseDerivationScreen_Dark() {
    TangemTheme(isDark = true) {
        CustomTokensScreen(state = AddCustomTokenPreviewData.state)
    }
}
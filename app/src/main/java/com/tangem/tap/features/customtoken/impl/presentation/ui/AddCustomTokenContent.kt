package com.tangem.tap.features.customtoken.impl.presentation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FabPosition
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.blockchain.common.Blockchain
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenFloatingButton
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenInputField
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenSelectorField
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokensToolbar
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenFloatingButton
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenForm
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenToolbar
import com.tangem.tap.features.customtoken.impl.presentation.ui.components.AddCustomTokenWarning
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

/**
 * Add custom token content
 *
 * @param state screen state
 *
[REDACTED_AUTHOR]
 */
@Composable
internal fun AddCustomTokenContent(state: AddCustomTokenStateHolder.Content) {
    BackHandler(onBack = state.onBackButtonClick)

    Scaffold(
        topBar = {
            AddCustomTokenToolbar(
                title = state.toolbar.title,
                onBackButtonClick = state.toolbar.onBackButtonClick,
            )
        },
        floatingActionButton = { AddCustomTokenFloatingButton(model = state.floatingButton) },
        floatingActionButtonPosition = FabPosition.Center,
    ) {
        Column(
            modifier = Modifier
                .padding(paddingValues = it)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            AddCustomTokenForm(model = state.form)

            state.warnings.forEach { description ->
                key(description) {
                    AddCustomTokenWarning(description)
                }
            }
        }
    }
}

@Preview(showSystemUi = true)
@Composable
private fun Preview_AddCustomTokenContent() {
    TangemTheme {
        AddCustomTokenContent(
            state = AddCustomTokenStateHolder.Content(
                onBackButtonClick = {},
                toolbar = AddCustomTokensToolbar(
                    title = TextReference.Res(R.string.add_custom_token_title),
                    onBackButtonClick = {},
                ),
                form = com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenForm(
                    contractAddressInputField = AddCustomTokenInputField.ContactAddress(
                        value = "",
                        onValueChange = {},
                        isError = false,
                        isLoading = false,
                    ),
                    networkSelectorField = AddCustomTokenSelectorField.Network(
                        selectedItem = AddCustomTokenSelectorField.SelectorItem.Title(
                            title = TextReference.Str("Avalanche"),
                            blockchain = Blockchain.Avalanche,
                        ),
                        items = listOf(),
                        onMenuItemClick = {},
                    ),
                    tokenNameInputField = AddCustomTokenInputField.TokenName(
                        value = "",
                        onValueChange = {},
                        isEnabled = false,
                        isError = false,
                    ),
                    tokenSymbolInputField = AddCustomTokenInputField.TokenSymbol(
                        value = "",
                        onValueChange = {},
                        isEnabled = false,
                        isError = false,
                    ),
                    decimalsInputField = AddCustomTokenInputField.Decimals(
                        value = "",
                        onValueChange = {},
                        isEnabled = false,
                        isError = false,
                    ),
                    derivationPathSelectorField = null,
                ),
                warnings = listOf(),
                floatingButton = AddCustomTokenFloatingButton(
                    isEnabled = false,
                    onClick = {},
                ),
            ),
        )
    }
}
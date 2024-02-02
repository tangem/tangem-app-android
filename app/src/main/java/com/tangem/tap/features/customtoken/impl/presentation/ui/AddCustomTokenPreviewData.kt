package com.tangem.tap.features.customtoken.impl.presentation.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.customtoken.impl.presentation.models.*
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
internal object AddCustomTokenPreviewData {

    fun createWarnings(): Set<AddCustomTokenWarning> {
        return setOf(
            AddCustomTokenWarning.PotentialScamToken,
            AddCustomTokenWarning.TokenAlreadyAdded,
            AddCustomTokenWarning.UnsupportedToken(networkName = "Solana"),
        )
    }

    fun createDefaultForm(): AddCustomTokenForm {
        return AddCustomTokenForm(
            contractAddressInputField = AddCustomTokenInputField.ContactAddress(
                value = "",
                onValueChange = {},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_contract_address_input_title),
                placeholder = TextReference.Str(value = "0x0000000000000000000000000000000000000000"),
                isLoading = false,
                isError = false,
                error = null,
            ),
            networkSelectorField = AddCustomTokenSelectorField.Network(
                label = TextReference.Res(R.string.custom_token_network_input_title),
                selectedItem = AddCustomTokenSelectorField.SelectorItem.Title(
                    title = TextReference.Res(R.string.custom_token_network_input_not_selected),
                    blockchain = Blockchain.Unknown,
                ),
                items = emptyList(),
                onMenuItemClick = {},
            ),
            tokenNameInputField = AddCustomTokenInputField.TokenName(
                value = "",
                onValueChange = {},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_name_input_title),
                placeholder = TextReference.Res(id = R.string.custom_token_name_input_placeholder),
                isEnabled = false,
            ),
            tokenSymbolInputField = AddCustomTokenInputField.TokenSymbol(
                value = "",
                onValueChange = {},
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_token_symbol_input_title_old),
                placeholder = TextReference.Res(id = R.string.custom_token_token_symbol_input_placeholder),
                isEnabled = false,
            ),
            decimalsInputField = AddCustomTokenInputField.Decimals(
                value = "",
                onValueChange = {},
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_decimals_input_title),
                placeholder = TextReference.Str(value = "8"),
                isEnabled = false,
            ),
            derivationPathSelectorField = AddCustomTokenSelectorField.DerivationPath(
                label = TextReference.Res(R.string.custom_token_derivation_path_input_title),
                selectedItem = AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle(
                    title = TextReference.Res(R.string.custom_token_derivation_path_default),
                    subtitle = TextReference.Res(R.string.custom_token_derivation_path_default),
                    blockchain = Blockchain.Unknown,
                ),
                items = emptyList(),
                onMenuItemClick = {},
                isEnabled = true,
            ),
            derivationPathInputField = null,
        )
    }

    fun createTestContent(): AddCustomTokenStateHolder.TestContent {
        return AddCustomTokenStateHolder.TestContent(
            onBackButtonClick = {},
            toolbar = AddCustomTokensToolbar(
                title = TextReference.Res(R.string.add_custom_token_title),
                onBackButtonClick = {},
            ),
            form = createDefaultForm(),
            warnings = createWarnings(),
            floatingButton = AddCustomTokenFloatingButton(
                isEnabled = false,
                showProgress = false,
                onClick = {},
            ),
            testBlock = AddCustomTokenTestBlock(
                chooseTokenButtonText = "Choose token",
                clearButtonText = "Clear address",
                resetButtonText = "Reset",
                onClearAddressButtonClick = {},
                onResetButtonClick = {},
            ),
            bottomSheet = AddCustomTokenChooseTokenBottomSheet(categoriesBlocks = emptyList(), onTestTokenClick = {}),
        )
    }

    fun createContent(): AddCustomTokenStateHolder.Content {
        return AddCustomTokenStateHolder.Content(
            onBackButtonClick = {},
            toolbar = AddCustomTokensToolbar(
                title = TextReference.Res(R.string.add_custom_token_title),
                onBackButtonClick = {},
            ),
            form = createDefaultForm(),
            warnings = createWarnings(),
            floatingButton = AddCustomTokenFloatingButton(
                isEnabled = false,
                showProgress = false,
                onClick = {},
            ),
        )
    }
}
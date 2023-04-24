package com.tangem.tap.features.customtoken.impl.presentation.models

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.wallet.R

/**
 * Toolbar model of add custom token screen
 *
 * @property title             title
 * @property onBackButtonClick lambda be invoked when back button is been pressed
 */
internal data class AddCustomTokensToolbar(val title: TextReference, val onBackButtonClick: () -> Unit)

/**
 * Model of block with fields for testing
 *
 * @property chooseTokenButtonText     choose token button text
 * @property clearButtonText           clear button text
 * @property resetButtonText           reset button text
 * @property onClearAddressButtonClick lambda be invoked when clear address button is been pressed
 * @property onResetButtonClick        lambda be invoked when reset form fields button is been pressed
 */
internal data class AddCustomTokenTestBlock(
    val chooseTokenButtonText: String,
    val clearButtonText: String,
    val resetButtonText: String,
    val onClearAddressButtonClick: () -> Unit,
    val onResetButtonClick: () -> Unit,
)

/**
 * Bottom sheet model for choose custom token
 *
 * @property categoriesBlocks tokens categories
 * @property onTestTokenClick lambda be invoked when token is been pressed
 */
internal data class AddCustomTokenChooseTokenBottomSheet(
    val categoriesBlocks: List<TokensCategoryBlock>,
    val onTestTokenClick: (String) -> Unit,
) {

    /**
     * Tokens category model
     *
     * @property name  category name
     * @property items category items
     */
    data class TokensCategoryBlock(val name: String, val items: List<TestTokenItem>)

    /**
     * Test token model
     *
     * @property name    token name
     * @property address token address
     */
    data class TestTokenItem(val name: String, val address: String)
}

/**
 * Form with fields model of add custom token screen
 *
 * @property contractAddressInputField   input field to enter the contract address
 * @property networkSelectorField        selector field to select the token network
 * @property tokenNameInputField         input field to enter the token name
 * @property tokenSymbolInputField       input field to enter the token symbol
 * @property decimalsInputField          input field to enter the token decimals
 * @property derivationPathSelectorField selector field to select the derivation path
 */
internal data class AddCustomTokenForm(
    val contractAddressInputField: AddCustomTokenInputField.ContactAddress,
    val networkSelectorField: AddCustomTokenSelectorField.Network,
    val tokenNameInputField: AddCustomTokenInputField.TokenName,
    val tokenSymbolInputField: AddCustomTokenInputField.TokenSymbol,
    val decimalsInputField: AddCustomTokenInputField.Decimals,
    val derivationPathSelectorField: AddCustomTokenSelectorField.DerivationPath?,
)

/** Base input field model of add custom token screen */
internal sealed interface AddCustomTokenInputField {

    /** Current value */
    val value: String

    /** Lambda be invoked when value is been changed */
    val onValueChange: (String) -> Unit

    /** Keyboard options */
    val keyboardOptions: KeyboardOptions

    /** Label */
    val label: TextReference

    /** Input availability */
    val isEnabled: Boolean

    /** Flag that determine if current value has error */
    val isError: Boolean

    /** Placeholder (hint) */
    val placeholder: TextReference

    /** Flag that determine the processing of current value */
    val isLoading: Boolean

    /**
     * Input field model to enter the contract address
     *
     * @property value         current value
     * @property onValueChange lambda be invoked when value is been changed
     * @property isError       flag that determine if current value has error
     * @property isLoading     flag that determine the processing of current value
     */
    data class ContactAddress(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val isError: Boolean,
        override val isLoading: Boolean,
    ) : AddCustomTokenInputField {
        override val isEnabled = true
        override val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        override val label = TextReference.Res(R.string.custom_token_contract_address_input_title)
        override val placeholder = TextReference.Str(value = "0x0000000000000000000000000000000000000000")
    }

    /**
     * Input field model to enter the token name
     *
     * @property value         current value
     * @property onValueChange lambda be invoked when value is been changed
     * @property isEnabled     input availability
     * @property isError       flag that determine if current value has error
     */
    data class TokenName(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val isEnabled: Boolean,
        override val isError: Boolean,
    ) : AddCustomTokenInputField {
        override val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        override val label = TextReference.Res(R.string.custom_token_name_input_title)
        override val placeholder = TextReference.Res(id = R.string.custom_token_name_input_placeholder)
        override val isLoading = false
    }

    /**
     * Input field model to enter the token symbol
     *
     * @property value         current value
     * @property onValueChange lambda be invoked when value is been changed
     * @property isEnabled     input availability
     * @property isError       flag that determine if current value has error
     */
    data class TokenSymbol(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val isEnabled: Boolean,
        override val isError: Boolean,
    ) : AddCustomTokenInputField {
        override val keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
        override val label = TextReference.Res(R.string.custom_token_token_symbol_input_title)
        override val placeholder = TextReference.Res(id = R.string.custom_token_token_symbol_input_placeholder)
        override val isLoading = false
    }

    /**
     * Input field model to enter the token decimals
     *
     * @property value         current value
     * @property onValueChange lambda be invoked when value is been changed
     * @property isEnabled     input availability
     * @property isError       flag that determine if current value has error
     */
    data class Decimals(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val isEnabled: Boolean,
        override val isError: Boolean,
    ) : AddCustomTokenInputField {
        override val keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next)
        override val label = TextReference.Res(R.string.custom_token_decimals_input_title)
        override val placeholder = TextReference.Str(value = "8")
        override val isLoading = false
    }
}

/** Base selector field model of add custom token screen */
internal sealed interface AddCustomTokenSelectorField {

    /** Selection availability */
    val isEnabled: Boolean

    /** Label string resource id */
    val label: TextReference

    /** Selected menu item */
    val selectedItem: SelectorItem

    /** Menu items */
    val items: List<SelectorItem>

    /** Lambda be invoked when menu item is been selected  */
    val onMenuItemClick: (Int) -> Unit

    /**
     * Network selector model
     *
     * @property selectedItem    selected menu item
     * @property items           menu items
     * @property onMenuItemClick lambda be invoked when menu item is been selected
     */
    data class Network(
        override val selectedItem: SelectorItem.Title,
        override val items: List<SelectorItem.Title>,
        override val onMenuItemClick: (Int) -> Unit,
    ) : AddCustomTokenSelectorField {
        override val isEnabled = true
        override val label = TextReference.Res(R.string.custom_token_network_input_title)
    }

    /**
     * Derivation path selector model
     *
     * @property isEnabled       selection availability
     * @property selectedItem    selected menu item
     * @property items           menu items
     * @property onMenuItemClick lambda be invoked when menu item is been selected
     */
    data class DerivationPath(
        override val isEnabled: Boolean,
        override val selectedItem: SelectorItem.TitleWithSubtitle,
        override val items: List<SelectorItem.TitleWithSubtitle>,
        override val onMenuItemClick: (Int) -> Unit,
    ) : AddCustomTokenSelectorField {
        override val label = TextReference.Res(R.string.custom_token_derivation_path_input_title)
    }

    /** Base menu item model */
    sealed interface SelectorItem {

        /** Title */
        val title: TextReference

        /**
         * Menu item with title
         *
         * @property title title text
         */
        data class Title(override val title: TextReference) : SelectorItem

        /**
         * Menu item with title ans subtitle
         *
         * @property title    title text
         * @property subtitle subtitle text
         */
        data class TitleWithSubtitle(override val title: TextReference, val subtitle: TextReference) : SelectorItem
    }
}

/**
 * Floating button of add custom token screen
 *
 * @property isEnabled button availability
 * @property onClick   lambda be invoked when button is been pressed
 */
internal data class AddCustomTokenFloatingButton(val isEnabled: Boolean, val onClick: () -> Unit)
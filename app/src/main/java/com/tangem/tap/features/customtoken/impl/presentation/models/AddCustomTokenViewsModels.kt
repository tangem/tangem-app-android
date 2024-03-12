package com.tangem.tap.features.customtoken.impl.presentation.models

import androidx.compose.foundation.text.KeyboardOptions
import com.tangem.blockchain.common.Blockchain
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
 * @property derivationPathInputField    input field for a custom derivation path
 * @property showTokenFields             if token fields should be shown
 */
internal data class AddCustomTokenForm(
    val contractAddressInputField: AddCustomTokenInputField.ContactAddress,
    val networkSelectorField: AddCustomTokenSelectorField.Network,
    val tokenNameInputField: AddCustomTokenInputField.TokenName,
    val tokenSymbolInputField: AddCustomTokenInputField.TokenSymbol,
    val decimalsInputField: AddCustomTokenInputField.Decimals,
    val derivationPathSelectorField: AddCustomTokenSelectorField.DerivationPath?,
    val derivationPathInputField: AddCustomTokenInputField.DerivationPath?,
    val showTokenFields: Boolean = false,
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

    /** Placeholder (hint) */
    val placeholder: TextReference

    /**
     * Input field model to enter the contract address
     *
     * @property value           current value
     * @property onValueChange   lambda be invoked when value is been changed
     * @property keyboardOptions keyboard options
     * @property label           label
     * @property placeholder     placeholder (hint)
     * @property isLoading       flag that determine the processing of current value
     * @property isError         flag that determine if current value has error
     * @property error           error description
     */
    data class ContactAddress(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        override val label: TextReference,
        override val placeholder: TextReference,
        val isLoading: Boolean,
        val isError: Boolean,
        val error: TextReference? = null,
    ) : AddCustomTokenInputField

    /**
     * Input field model to enter the token name
     *
     * @property value           current value
     * @property onValueChange   lambda be invoked when value is been changed
     * @property keyboardOptions keyboard options
     * @property label           label
     * @property placeholder     placeholder (hint)
     * @property isEnabled       input availability
     */
    data class TokenName(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        override val label: TextReference,
        override val placeholder: TextReference,
        val isEnabled: Boolean,
    ) : AddCustomTokenInputField

    /**
     * Input field model to enter the token symbol
     *
     * @property value           current value
     * @property onValueChange   lambda be invoked when value is been changed
     * @property keyboardOptions keyboard options
     * @property label           label
     * @property placeholder     placeholder (hint)
     * @property isEnabled       input availability
     */
    data class TokenSymbol(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        override val label: TextReference,
        override val placeholder: TextReference,
        val isEnabled: Boolean,
    ) : AddCustomTokenInputField

    /**
     * Input field model to enter the token decimals
     *
     * @property value           current value
     * @property onValueChange   lambda be invoked when value is been changed
     * @property keyboardOptions keyboard options
     * @property label           label
     * @property placeholder     placeholder (hint)
     * @property isEnabled       input availability
     */
    data class Decimals(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        override val label: TextReference,
        override val placeholder: TextReference,
        val isEnabled: Boolean,
    ) : AddCustomTokenInputField

    /**
     * Input field model to enter a custom derivation path
     *
     * @property value           current value
     * @property onValueChange   lambda be invoked when value is been changed
     * @property keyboardOptions keyboard options
     * @property label           label
     * @property placeholder     placeholder (hint)
     * @property showField       whether the field should be shown
     */
    data class DerivationPath(
        override val value: String,
        override val onValueChange: (String) -> Unit,
        override val keyboardOptions: KeyboardOptions,
        override val label: TextReference,
        override val placeholder: TextReference,
        val showField: Boolean = false,
    ) : AddCustomTokenInputField
}

/** Base selector field model of add custom token screen */
internal sealed interface AddCustomTokenSelectorField {

    /** Label */
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
     * @property label           label
     * @property selectedItem    selected menu item
     * @property items           menu items
     * @property onMenuItemClick lambda be invoked when menu item is been selected
     */
    data class Network(
        override val label: TextReference,
        override val selectedItem: SelectorItem.Title,
        override val items: List<SelectorItem.Title>,
        override val onMenuItemClick: (Int) -> Unit,
    ) : AddCustomTokenSelectorField

    /**
     * Derivation path selector model
     *
     * @property label           label
     * @property selectedItem    selected menu item
     * @property items           menu items
     * @property onMenuItemClick lambda be invoked when menu item is been selected
     * @property isEnabled       selection availability
     */
    data class DerivationPath(
        override val label: TextReference,
        override val selectedItem: SelectorItem.TitleWithSubtitle,
        override val items: List<SelectorItem.TitleWithSubtitle>,
        override val onMenuItemClick: (Int) -> Unit,
        val isEnabled: Boolean,
    ) : AddCustomTokenSelectorField

    /** Base menu item model */
    sealed interface SelectorItem {

        /** Title */
        val title: TextReference

        /** Blockchain */
        val blockchain: Blockchain

        /**
         * Menu item with title
         *
         * @property title      title text
         * @property blockchain blockchain
         */
        data class Title(override val title: TextReference, override val blockchain: Blockchain) : SelectorItem

        /**
         * Menu item with title ans subtitle
         *
         * @property title      title text
         * @property blockchain blockchain
         * @property subtitle   subtitle text
         */
        data class TitleWithSubtitle(
            override val title: TextReference,
            override val blockchain: Blockchain,
            val subtitle: TextReference,
            val type: DerivationPathSelectorType = DerivationPathSelectorType.BLOCKCHAIN,
        ) : SelectorItem
    }
}

enum class DerivationPathSelectorType {
    DEFAULT, CUSTOM, BLOCKCHAIN
}

/**
 * Warning model of add custom token screen
 *
 * @property description warning description
 */
internal sealed class AddCustomTokenWarning(val description: TextReference) {

    /** Potential scam warning */
    data object PotentialScamToken : AddCustomTokenWarning(
        description = TextReference.Res(R.string.custom_token_validation_error_not_found),
    )

    /** Token already added warning */
    data object TokenAlreadyAdded : AddCustomTokenWarning(
        description = TextReference.Res(R.string.custom_token_validation_error_already_added),
    )

    /** Unsupported token warning */
    data class UnsupportedToken(val networkName: String) : AddCustomTokenWarning(
        description = TextReference.Res(R.string.alert_manage_tokens_unsupported_message, networkName),
    )

    data object WrongDerivationPath : AddCustomTokenWarning(
        description = TextReference.Res(R.string.custom_token_invalid_derivation_path),
    )
}

/**
 * Floating button of add custom token screen
 *
 * @property isEnabled    button availability
 * @property showProgress whether circle progress indication is enabled
 * @property onClick      lambda be invoked when button is been pressed
 */
internal data class AddCustomTokenFloatingButton(
    val isEnabled: Boolean,
    val showProgress: Boolean,
    val onClick: () -> Unit,
)

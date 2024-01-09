package com.tangem.managetokens.presentation.customtokens.state.previewdata

import com.tangem.managetokens.presentation.common.state.ChooseWalletState
import com.tangem.managetokens.presentation.common.state.WalletState
import com.tangem.managetokens.presentation.customtokens.state.*
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf

internal object AddCustomTokenPreviewData {
    val state = AddCustomTokenState(
        chooseWalletState = ChooseWalletState.Choose(
            wallets = persistentListOf(),
            selectedWallet = WalletState(
                "",
                "",
                "My Wallet",
                {},
            ),
            onChooseWalletClick = { },
            onCloseChoosingWalletClick = { },
        ),
        chooseNetworkState = ChooseNetworkState(
            networks = persistentListOf(),
            selectedNetwork = null,
            onChooseNetworkClick = { },
            onCloseChoosingNetworkClick = {},
        ),
        chooseDerivationState = ChooseDerivationState(
            derivations = persistentListOf(),
            selectedDerivation = null,
            enterCustomDerivationState = null,
            onChooseDerivationClick = { },
            onCloseChoosingDerivationClick = {},
            onEnterCustomDerivation = {},
        ),
        tokenData = CustomTokenData(
            contractAddressTextField = TextFieldState.Editable(
                value = "0x4ace7262705b68bcba5b91de96889349394",
                isEnabled = false,
                onValueChange = {},
            ),
            nameTextField = TextFieldState.Loading,
            symbolTextField = TextFieldState.Loading,
            decimalsTextField = TextFieldState.Loading,
        ),
        warnings = persistentSetOf(),
        addTokenButton = ButtonState(isEnabled = true, onClick = {}),
    )
}
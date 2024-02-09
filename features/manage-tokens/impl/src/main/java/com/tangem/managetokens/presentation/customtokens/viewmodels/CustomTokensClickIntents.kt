package com.tangem.managetokens.presentation.customtokens.viewmodels

import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.customtokens.state.Derivation

@Suppress("TooManyFunctions")
internal interface CustomTokensClickIntents {

    fun onNetworkSelected(networkItemState: NetworkItemState)

    fun onChooseNetworkClick()

    fun onCloseChoosingNetworkClick()

    fun onWalletSelected(walletId: String)

    fun onChooseWalletClick()

    fun onCloseChoosingWalletClick()

    fun onContractAddressChange(input: String)

    fun onTokenNameChange(input: String)

    fun onSymbolChange(input: String)

    fun onDecimalsChange(input: String)

    fun onContractAddressFocusExit()

    fun onTokenNameFocusExit()

    fun onSymbolFocusExit()

    fun onDecimalsFocusExit()

    fun onDerivationSelected(derivation: Derivation)

    fun onChooseDerivationClick()

    fun onCloseChoosingDerivationClick()

    fun onEnterCustomDerivation()

    fun onCustomDerivationChange(input: String)

    fun onCustomDerivationSelected()

    fun onCustomDerivationDialogDismissed()

    fun onAddCustomButtonClick()

    fun onBack()
}
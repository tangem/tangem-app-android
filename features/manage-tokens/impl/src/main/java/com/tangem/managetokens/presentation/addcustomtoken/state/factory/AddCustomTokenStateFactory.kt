package com.tangem.managetokens.presentation.addcustomtoken.state.factory

import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.event.triggeredEvent
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.tokens.error.AddCustomTokenError
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.managetokens.presentation.common.state.*
import com.tangem.managetokens.presentation.addcustomtoken.state.*
import com.tangem.managetokens.presentation.addcustomtoken.viewmodels.AddCustomTokenClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toPersistentList
import kotlinx.collections.immutable.toPersistentSet

@Suppress("LargeClass")
internal class AddCustomTokenStateFactory(
    private val currentStateProvider: Provider<AddCustomTokenState>,
    private val clickIntents: AddCustomTokenClickIntents,
) {

    fun getInitialState(): AddCustomTokenState {
        return AddCustomTokenState(
            chooseWalletState = ChooseWalletState.NoSelection,
            chooseNetworkState = ChooseNetworkState(
                networks = persistentListOf(),
                selectedNetwork = null,
                onChooseNetworkClick = clickIntents::onChooseNetworkClick,
                onCloseChoosingNetworkClick = clickIntents::onCloseChoosingNetworkClick,
            ),
            chooseDerivationState = ChooseDerivationState(
                derivations = persistentListOf(),
                selectedDerivation = null,
                enterCustomDerivationState = null,
                onChooseDerivationClick = clickIntents::onChooseDerivationClick,
                onCloseChoosingDerivationClick = clickIntents::onCloseChoosingDerivationClick,
                onEnterCustomDerivation = clickIntents::onEnterCustomDerivation,
            ),
            tokenData = null,
            warnings = persistentSetOf(),
            addTokenButton = ButtonState(isEnabled = false, onClick = clickIntents::onAddCustomButtonClick),
        )
    }

    fun getFullState(
        suitableUserWallets: List<UserWallet>,
        allUserWallets: List<UserWallet>,
        selectedWalletId: UserWalletId?,
        supportedNetworks: List<Network>,
    ): AddCustomTokenState {
        val derivations = getListOfDerivations(supportedNetworks)

        val chooseDerivationState = createChooseDerivationState(derivations)
        val networkConverter = NetworkToNetworkItemStateConverter(clickIntents::onNetworkSelected)
        val networks = supportedNetworks.map { networkConverter.convert(it) }

        val chooseWalletState = getNewChooseWalletState(allUserWallets, suitableUserWallets, selectedWalletId)

        return AddCustomTokenState(
            chooseWalletState = chooseWalletState,
            chooseNetworkState = ChooseNetworkState(
                networks = networks.toPersistentList(),
                selectedNetwork = null,
                onChooseNetworkClick = clickIntents::onChooseNetworkClick,
                onCloseChoosingNetworkClick = clickIntents::onBack,
            ),
            chooseDerivationState = chooseDerivationState,
            tokenData = null,
            warnings = persistentSetOf(),
            addTokenButton = ButtonState(isEnabled = false, onClick = clickIntents::onAddCustomButtonClick),
        )
    }

    private fun getListOfDerivations(
        networksListToGenerateDerivations: List<Network>,
        filterOnlyHardenedDerivations: Boolean = false,
    ): List<Derivation> {
        return networksListToGenerateDerivations.mapNotNull { network ->
            network.derivationPath.value?.let { rawPath ->
                Derivation(
                    networkName = network.name,
                    standardType = network.standardType.name,
                    path = rawPath,
                    networkId = network.backendId,
                    onDerivationSelected = clickIntents::onDerivationSelected,
                )
            }.takeIf { derivation ->
                if (filterOnlyHardenedDerivations) {
                    derivation?.let { allNodesHardened(createDerivationPathOrNull(it.path)) } ?: false
                } else {
                    true
                }
            }
        }
    }

    private fun createChooseDerivationState(derivations: List<Derivation>): ChooseDerivationState? {
        return if (derivations.isNotEmpty()) {
            ChooseDerivationState(
                derivations = derivations.toPersistentList(),
                selectedDerivation = null,
                enterCustomDerivationState = null,
                onChooseDerivationClick = clickIntents::onChooseDerivationClick,
                onCloseChoosingDerivationClick = clickIntents::onBack,
                onEnterCustomDerivation = clickIntents::onEnterCustomDerivation,
            )
        } else {
            null
        }
    }

    fun updateWithNewWalletSelected(
        selectedWalletId: UserWalletId,
        supportedNetworks: List<Network>,
    ): AddCustomTokenState {
        val derivations = getListOfDerivations(supportedNetworks)

        val chooseDerivationState = createChooseDerivationState(derivations)
        val networkConverter = NetworkToNetworkItemStateConverter(clickIntents::onNetworkSelected)
        val networks = supportedNetworks.map { networkConverter.convert(it) }

        val currentWalletState = requireNotNull(
            currentStateProvider().chooseWalletState as? ChooseWalletState.Choose,
        ) {
            "If user wallet was chosen, ChooseWalletState type must be Choose"
        }
        val selectedWalletState = currentWalletState.wallets.find { it.walletId == selectedWalletId.stringValue }
        val chooseWalletState = currentWalletState.copy(selectedWallet = selectedWalletState)

        return AddCustomTokenState(
            chooseWalletState = chooseWalletState,
            chooseNetworkState = ChooseNetworkState(
                networks = networks.toPersistentList(),
                selectedNetwork = null,
                onChooseNetworkClick = clickIntents::onChooseNetworkClick,
                onCloseChoosingNetworkClick = clickIntents::onBack,
            ),
            chooseDerivationState = chooseDerivationState,
            tokenData = null,
            warnings = persistentSetOf(),
            addTokenButton = ButtonState(isEnabled = false, onClick = clickIntents::onAddCustomButtonClick),
        )
    }

    private fun getNewChooseWalletState(
        suitableUserWallets: List<UserWallet>,
        allUserWallets: List<UserWallet>,
        selectedWalletId: UserWalletId?,
    ): ChooseWalletState {
        val chooseWalletState = if (suitableUserWallets.size == 1) {
            ChooseWalletState.NoSelection
        } else if (suitableUserWallets.isEmpty() && allUserWallets.all { !it.isMultiCurrency }) {
            ChooseWalletState.Warning(ChooseWalletWarning.SINGLE_CURRENCY)
        } else {
            var selectedWalletState: WalletState? = null
            ChooseWalletState.Choose(
                wallets = suitableUserWallets.map { wallet ->
                    val walletState = WalletState(
                        walletId = wallet.walletId.stringValue,
                        artworkUrl = wallet.artworkUrl,
                        onSelected = clickIntents::onWalletSelected,
                        walletName = wallet.name,
                    )
                    if (wallet.walletId.stringValue == selectedWalletId?.stringValue) {
                        selectedWalletState = walletState
                    }
                    walletState
                }.toPersistentList(),
                selectedWallet = requireNotNull(selectedWalletState),
                onChooseWalletClick = clickIntents::onChooseWalletClick,
                onCloseChoosingWalletClick = clickIntents::onCloseChoosingWalletClick,
            )
        }
        return chooseWalletState
    }

    fun removeTokenAddressError(): AddCustomTokenState {
        return addTokenAddressFieldError(null)
    }

    private fun addTokenAddressFieldError(error: AddCustomTokenWarning?): AddCustomTokenState {
        val tokenData = currentStateProvider().tokenData ?: return currentStateProvider()
        val contractAddressField = tokenData.contractAddressTextField.copySealed(error = error)
        return currentStateProvider().copy(tokenData = tokenData.copy(contractAddressTextField = contractAddressField))
    }

    private fun unlockAndClearNameSymbolAndDecimals(state: AddCustomTokenState): AddCustomTokenState {
        val currentTokenData = state.tokenData
        return state.copy(
            tokenData = currentTokenData?.copy(
                nameTextField = TextFieldState.Editable(
                    value = "",
                    isEnabled = true,
                    onValueChange = clickIntents::onTokenNameChange,
                    onFocusExit = clickIntents::onTokenNameFocusExit,
                ),
                symbolTextField = TextFieldState.Editable(
                    value = "",
                    isEnabled = true,
                    onValueChange = clickIntents::onSymbolChange,
                    onFocusExit = clickIntents::onSymbolFocusExit,
                ),
                decimalsTextField = TextFieldState.Editable(
                    value = "",
                    isEnabled = true,
                    onValueChange = clickIntents::onDecimalsChange,
                    onFocusExit = clickIntents::onDecimalsFocusExit,
                ),
            ),
        )
    }

    fun getStateAndTriggerEvent(
        state: AddCustomTokenState,
        event: Event,
        setUiState: (AddCustomTokenState) -> Unit,
    ): AddCustomTokenState {
        return state.copy(
            event = triggeredEvent(
                data = event,
                onConsume = {
                    val currentState = currentStateProvider()
                    setUiState(currentState.copy(event = consumedEvent()))
                },
            ),
        )
    }

    fun updateStateOnNetworkSelected(
        networkItemState: NetworkItemState,
        supportsTokens: Boolean,
        networks: List<Network>,
        requiresHardenedDerivationOnly: Boolean,
    ): AddCustomTokenState {
        val uiState = currentStateProvider()
        val tokenData = if (supportsTokens) {
            uiState.tokenData ?: CustomTokenData(
                contractAddressTextField = TextFieldState.Editable(
                    value = "",
                    isEnabled = true,
                    onValueChange = clickIntents::onContractAddressChange,
                    onFocusExit = clickIntents::onContractAddressFocusExit,
                ),
                nameTextField = TextFieldState.Editable(
                    value = "",
                    isEnabled = false,
                    onValueChange = clickIntents::onTokenNameChange,
                    onFocusExit = clickIntents::onTokenNameFocusExit,
                ),
                symbolTextField = TextFieldState.Editable(
                    value = "",
                    isEnabled = false,
                    onValueChange = clickIntents::onSymbolChange,
                    onFocusExit = clickIntents::onSymbolFocusExit,
                ),
                decimalsTextField = TextFieldState.Editable(
                    value = "",
                    isEnabled = false,
                    onValueChange = clickIntents::onDecimalsChange,
                    onFocusExit = clickIntents::onDecimalsFocusExit,
                ),
            )
        } else {
            null
        }

        val derivations = getListOfDerivations(networks, requiresHardenedDerivationOnly)
        val chooseDerivationState = createChooseDerivationState(derivations)

        return uiState.copy(
            chooseNetworkState = uiState.chooseNetworkState.copy(
                selectedNetwork = networkItemState,
            ),
            chooseDerivationState = chooseDerivationState,
            tokenData = tokenData,
            addTokenButton = uiState.addTokenButton.copy(isEnabled = true),
        )
    }

    fun updateOnCustomDerivationSelected(): AddCustomTokenState {
        val uiState = currentStateProvider()
        return uiState.copy(
            chooseDerivationState = uiState.chooseDerivationState?.copy(
                enterCustomDerivationState = null,
                selectedDerivation = Derivation(
                    networkName = "",
                    path = uiState.chooseDerivationState.enterCustomDerivationState?.value ?: "",
                    networkId = null,
                    standardType = null,
                    onDerivationSelected = clickIntents::onDerivationSelected,
                ),
            ),
        )
    }

    fun updateStateOnEnterCustomDerivation(): AddCustomTokenState {
        val customDerivationState = EnterCustomDerivationState(
            value = "",
            onValueChange = clickIntents::onCustomDerivationChange,
            confirmButtonEnabled = false,
            derivationIncorrect = false,
            onConfirmButtonClick = clickIntents::onCustomDerivationSelected,
            onDismiss = clickIntents::onCustomDerivationDialogDismissed,
        )
        val uiState = currentStateProvider()
        return uiState.copy(
            chooseDerivationState = uiState.chooseDerivationState?.copy(
                enterCustomDerivationState = customDerivationState,
            ),
        )
    }

    fun updateOnCustomDerivationEntered(input: String, requiresHardenedDerivationOnly: Boolean): AddCustomTokenState {
        val uiState = currentStateProvider()
        val path = createDerivationPathOrNull(input)
        val isWrongDerivationForWallet2 = isWrongDerivationForWallet2(
            requiresHardenedDerivationOnly = requiresHardenedDerivationOnly,
            derivationPath = path,
        )
        val enterDerivationState = uiState.chooseDerivationState?.enterCustomDerivationState?.copy(
            confirmButtonEnabled = path != null && !isWrongDerivationForWallet2,
            derivationIncorrect = input.isNotBlank() && path == null || isWrongDerivationForWallet2,
        )
        return uiState.copy(
            chooseDerivationState = uiState.chooseDerivationState?.copy(
                enterCustomDerivationState = enterDerivationState,
            ),
        )
    }

    private fun isWrongDerivationForWallet2(
        requiresHardenedDerivationOnly: Boolean,
        derivationPath: DerivationPath?,
    ): Boolean {
        return if (requiresHardenedDerivationOnly) {
            !allNodesHardened(derivationPath)
        } else {
            false
        }
    }

    private fun allNodesHardened(derivationPath: DerivationPath?): Boolean {
        return derivationPath?.nodes?.all { it.isHardened } ?: false
    }

    private fun createDerivationPathOrNull(rawPath: String): DerivationPath? {
        return try {
            DerivationPath(rawPath)
        } catch (error: Throwable) {
            null
        }
    }

    fun updateStateOnLoadingTokenInfo(contractAddress: String): AddCustomTokenState {
        return currentStateProvider().copy(
            tokenData = CustomTokenData(
                contractAddressTextField = TextFieldState.Editable(
                    value = contractAddress,
                    isEnabled = true,
                    onValueChange = clickIntents::onContractAddressChange,
                    onFocusExit = clickIntents::onContractAddressFocusExit,

                ),
                nameTextField = TextFieldState.Loading,
                symbolTextField = TextFieldState.Loading,
                decimalsTextField = TextFieldState.Loading,
            ),
        )
    }

    fun handleAddressError(error: AddCustomTokenError): AddCustomTokenState {
        val uiState = currentStateProvider()
        return when (error) {
            AddCustomTokenError.INVALID_CONTRACT_ADDRESS -> {
                removeTokenAddressError().also { unlockAndClearNameSymbolAndDecimals(it) }
                    .copy(
                        addTokenButton = uiState.addTokenButton.copy(isEnabled = false),
                        warnings = uiState.warnings
                            .filterNot { it is AddCustomTokenWarning.PotentialScamToken }
                            .toPersistentSet(),
                    )
            }
            AddCustomTokenError.FIELD_IS_EMPTY ->
                removeTokenAddressError().also { unlockAndClearNameSymbolAndDecimals(it) }
                    .copy(
                        addTokenButton = uiState.addTokenButton.copy(
                            isEnabled = uiState.tokenData?.isRequiredInformationProvided() == true,
                        ),
                    )
        }
    }
}

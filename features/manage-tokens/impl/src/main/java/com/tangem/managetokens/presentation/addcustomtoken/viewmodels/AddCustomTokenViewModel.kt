package com.tangem.managetokens.presentation.addcustomtoken.viewmodels

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import arrow.core.getOrElse
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.managetokens.presentation.addcustomtoken.router.AddCustomTokenRouter
import com.tangem.managetokens.presentation.common.analytics.ManageTokens
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.Event
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.addcustomtoken.state.*
import com.tangem.managetokens.presentation.addcustomtoken.state.factory.AddCustomTokenStateToCryptoCurrencyConverter
import com.tangem.managetokens.presentation.addcustomtoken.state.factory.ContractAddressToCustomTokenDataConverter
import com.tangem.managetokens.presentation.addcustomtoken.state.factory.AddCustomTokenStateFactory
import com.tangem.managetokens.presentation.addcustomtoken.state.factory.FoundTokenToCustomTokenDataConverter
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.Debouncer.Companion.DEFAULT_WAIT_TIME_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList", "TooManyFunctions", "LargeClass")
@Stable
@HiltViewModel
internal class AddCustomTokenViewModel @Inject constructor(
    private val dispatchers: CoroutineDispatcherProvider,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val addCryptoCurrenciesUseCase: AddCryptoCurrenciesUseCase,
    private val selectWalletUseCase: SelectWalletUseCase,
    private val getCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val findTokenByContractAddressUseCase: FindTokenByContractAddressUseCase,
    private val validateContractAddressUseCase: ValidateContractAddressUseCase,
    private val getNetworksSupportedByWallet: GetNetworksSupportedByWallet,
    private val areTokensSupportedByNetworkUseCase: AreTokensSupportedByNetworkUseCase,
    private val requiresHardenedDerivationOnlyUseCase: RequiresHardenedDerivationOnlyUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel(), AddCustomTokenClickIntents, DefaultLifecycleObserver {

    private val debouncer = Debouncer()

    private val stateFactory = AddCustomTokenStateFactory(
        currentStateProvider = Provider { uiState },
        clickIntents = this,
    )

    var router: AddCustomTokenRouter by Delegates.notNull()

    var uiState: AddCustomTokenState by mutableStateOf(stateFactory.getInitialState())
        private set

    /**
     *  Called when the bottom sheet was opened
     */
    fun onInitialize() {
        viewModelScope.launch(dispatchers.io) {
            getWalletsUseCase()
                .distinctUntilChanged()
                .collectLatest { userWallets ->
                    val suitableUserWallets = userWallets.filter { it.isMultiCurrency && !it.isLocked }
                    val selectedWalletId = selectSuitableWallet(suitableUserWallets)
                    val networks = selectedWalletId?.let { getSupportedNetworks(selectedWalletId) } ?: emptyList()
                    withContext(dispatchers.main) {
                        uiState = stateFactory.getFullState(
                            allUserWallets = suitableUserWallets,
                            suitableUserWallets = userWallets,
                            selectedWalletId = selectedWalletId,
                            supportedNetworks = networks,
                        )
                    }
                }
        }
    }

    /**
     *  Called after the bottom sheet is closed
     */
    fun onDispose() {
        // We have to manually cancel viewModelScope's child jobs when bottom sheet is closed
        viewModelScope.coroutineContext.cancelChildren()
        // and reset state
        uiState = stateFactory.getInitialState()
    }

    private suspend fun selectSuitableWallet(suitableUserWallets: List<UserWallet>): UserWalletId? {
        val selectedWallet = getSelectedWalletSyncUseCase().getOrNull()
        val selectedWalletId = if (walletSupportsAddingTokens(selectedWallet) && suitableUserWallets.isNotEmpty()) {
            val walletId = suitableUserWallets.first().walletId
            selectWalletUseCase(walletId)
            walletId
        } else {
            selectedWallet?.walletId
        }
        return selectedWalletId
    }

    private fun walletSupportsAddingTokens(userWallet: UserWallet?): Boolean {
        return userWallet != null && userWallet.isMultiCurrency && !userWallet.isLocked
    }

    private suspend fun getSupportedNetworks(userWalletId: UserWalletId): List<Network> {
        return getNetworksSupportedByWallet(userWalletId).fold(
            ifLeft = { emptyList() },
            ifRight = { it },
        )
    }

    override fun onNetworkSelected(networkItemState: NetworkItemState) {
        analyticsEventHandler.send(ManageTokens.CustomTokenNetworkSelected(networkItemState.name))
        selectNetwork(networkItemState)
        router.popBackStack()
    }

    private fun selectNetwork(networkItemState: NetworkItemState) {
        viewModelScope.launch(dispatchers.io) {
            // TODO [REDACTED_TASK_KEY]
            val selectedWalletId = getSelectedWalletSyncUseCase().getOrNull()?.walletId ?: return@launch
            val supportsTokens = areTokensSupportedByNetworkUseCase(
                networkId = networkItemState.id,
                userWalletId = selectedWalletId,
            ).getOrNull() ?: false

            val networksForDerivations = getSupportedNetworks(selectedWalletId)

            withContext(dispatchers.main) {
                uiState = stateFactory.updateStateOnNetworkSelected(
                    networkItemState = networkItemState,
                    supportsTokens = supportsTokens,
                    networks = networksForDerivations,
                    requiresHardenedDerivationOnly = requiresHardenedDerivationOnly(
                        networkId = networkItemState.id,
                        userWalletId = selectedWalletId,
                    ),
                )
            }
        }
    }

    override fun onChooseNetworkClick() {
        router.openCustomTokenChooseNetwork()
    }

    override fun onCloseChoosingNetworkClick() {
        router.popBackStack()
    }

    override fun onWalletSelected(walletId: String) {
        analyticsEventHandler.send(ManageTokens.WalletSelected(ManageTokens.WalletSelected.Source.CustomToken))
        viewModelScope.launch(dispatchers.io) {
            val userWalletId = UserWalletId(walletId)
            selectWalletUseCase(userWalletId)
            val supportedNetworks = getSupportedNetworks(UserWalletId(walletId))

            withContext(dispatchers.main) {
                uiState = stateFactory.updateWithNewWalletSelected(
                    selectedWalletId = userWalletId,
                    supportedNetworks = supportedNetworks,
                )
                router.popBackStack()
            }
        }
    }

    override fun onChooseWalletClick() {
        router.openCustomTokenChooseWallet()
    }

    override fun onCloseChoosingWalletClick() {
        router.popBackStack()
    }

    override fun onContractAddressChange(input: String) {
        uiState = uiState.copy(
            tokenData = uiState.tokenData?.copy(
                contractAddressTextField = TextFieldState.Editable(
                    value = input,
                    isEnabled = true,
                    onValueChange = this::onContractAddressChange,
                    onFocusExit = this::onContractAddressFocusExit,
                ),
            ),
        )
        debouncer.debounce(waitMs = DEFAULT_WAIT_TIME_MS, coroutineScope = viewModelScope + dispatchers.io) {
            uiState.chooseNetworkState.selectedNetwork?.let { networkItemState ->
                validateContractAddressUseCase(input, networkItemState.id).fold(
                    ifRight = {
                        uiState = stateFactory.removeTokenAddressError()
                            .copy(
                                addTokenButton = uiState.addTokenButton.copy(
                                    isEnabled = uiState.tokenData?.isRequiredInformationProvided() == true,
                                ),
                                warnings = uiState.warnings
                                    .filterNot { it is AddCustomTokenWarning.PotentialScamToken }.toPersistentSet(),
                            )
                        fetchTokenInformation(contractAddress = input, networkId = networkItemState.id)
                    },
                    ifLeft = { error ->
                        uiState = stateFactory.handleAddressError(error)
                    },
                )
            }
        }
    }

    private fun fetchTokenInformation(contractAddress: String, networkId: String) {
        viewModelScope.launch(dispatchers.main) {
            uiState = stateFactory.updateStateOnLoadingTokenInfo(contractAddress)
            withContext(dispatchers.io) {
                findTokenByContractAddressUseCase(
                    contractAddress = contractAddress,
                    networkId = networkId,
                ).fold(
                    ifLeft = {
                        val tokenData = ContractAddressToCustomTokenDataConverter(this@AddCustomTokenViewModel)
                            .convert(contractAddress)

                        val isButtonEnabled = tokenData.isRequiredInformationProvided()
                        uiState = uiState.copy(
                            tokenData = tokenData,
                            warnings = (uiState.warnings + AddCustomTokenWarning.PotentialScamToken).toPersistentSet(),
                            addTokenButton = uiState.addTokenButton.copy(
                                isEnabled = isButtonEnabled,
                            ),
                        )
                    },
                    ifRight = { token ->
                        val tokenData = if (token != null) {
                            FoundTokenToCustomTokenDataConverter(this@AddCustomTokenViewModel).convert(token)
                        } else {
                            ContractAddressToCustomTokenDataConverter(this@AddCustomTokenViewModel).convert(
                                contractAddress,
                            )
                        }

                        val isButtonEnabled = tokenData.isRequiredInformationProvided()
                        uiState = uiState.copy(
                            tokenData = tokenData,
                            addTokenButton = uiState.addTokenButton.copy(
                                isEnabled = isButtonEnabled,
                            ),
                        )
                    },
                )
            }
        }
    }

    override fun onTokenNameChange(input: String) {
        uiState = uiState.copy(
            tokenData = uiState.tokenData?.copy(
                nameTextField = TextFieldState.Editable(
                    value = input,
                    isEnabled = true,
                    onValueChange = this::onTokenNameChange,
                    onFocusExit = this::onTokenNameFocusExit,
                ),
            ),
            addTokenButton = uiState.addTokenButton.copy(
                isEnabled = uiState.tokenData?.isRequiredInformationProvided() == true,
            ),
        )
    }

    override fun onSymbolChange(input: String) {
        uiState = uiState.copy(
            tokenData = uiState.tokenData?.copy(
                symbolTextField = TextFieldState.Editable(
                    value = input,
                    isEnabled = true,
                    onValueChange = this::onSymbolChange,
                    onFocusExit = this::onSymbolFocusExit,
                ),
            ),
            addTokenButton = uiState.addTokenButton.copy(
                isEnabled = uiState.tokenData?.isRequiredInformationProvided() == true,
            ),
        )
    }

    override fun onDecimalsChange(input: String) {
        val correctInput = input.toIntOrNull()
        val error = if (input.isNotBlank() && correctInput == null) {
            AddCustomTokenWarning.WrongDecimals
        } else {
            null
        }
        uiState = uiState.copy(
            tokenData = uiState.tokenData?.copy(
                decimalsTextField = TextFieldState.Editable(
                    value = input,
                    isEnabled = true,
                    onValueChange = this::onDecimalsChange,
                    error = error,
                    onFocusExit = this::onDecimalsFocusExit,
                ),
            ),
            addTokenButton = uiState.addTokenButton.copy(
                isEnabled = uiState.tokenData?.isRequiredInformationProvided() == true,
            ),
        )
    }

    override fun onContractAddressFocusExit() {
        val error = (uiState.tokenData?.contractAddressTextField as? TextFieldState.Editable)?.error
        val validated = error !is AddCustomTokenWarning.InvalidContractAddress
        analyticsEventHandler.send(ManageTokens.CustomTokenAddress(validated = validated))
    }

    override fun onTokenNameFocusExit() {
        analyticsEventHandler.send(ManageTokens.CustomTokenName)
    }

    override fun onSymbolFocusExit() {
        analyticsEventHandler.send(ManageTokens.CustomTokenSymbol)
    }

    override fun onDecimalsFocusExit() {
        analyticsEventHandler.send(ManageTokens.CustomTokenDecimals)
    }

    override fun onDerivationSelected(derivation: Derivation) {
        derivation.standardType?.let {
            analyticsEventHandler.send(ManageTokens.CustomTokenDerivationSelected(derivation.networkName))
        }
        uiState = uiState.copy(
            chooseDerivationState = uiState.chooseDerivationState?.copy(selectedDerivation = derivation),
        )
        router.popBackStack()
    }

    override fun onChooseDerivationClick() {
        router.openCustomTokenChooseDerivation()
    }

    override fun onCloseChoosingDerivationClick() {
        router.popBackStack()
    }

    override fun onCustomDerivationChange(input: String) {
        val selectedWallet = getSelectedWalletSyncUseCase().getOrNull() ?: return

        analyticsEventHandler.send(ManageTokens.CustomTokenDerivationSelected(ManageTokens.Derivation.CUSTOM.value))
        uiState = uiState.copy(
            chooseDerivationState = uiState.chooseDerivationState?.copy(
                enterCustomDerivationState = uiState.chooseDerivationState?.enterCustomDerivationState?.copy(
                    value = input,
                ),
            ),
        )
        debouncer.debounce(waitMs = DEFAULT_WAIT_TIME_MS, coroutineScope = viewModelScope + dispatchers.io) {
            val networkId = uiState.chooseNetworkState.selectedNetwork?.id ?: return@debounce
            uiState = stateFactory.updateOnCustomDerivationEntered(
                input = input,
                requiresHardenedDerivationOnly = requiresHardenedDerivationOnly(networkId, selectedWallet.walletId),
            )
        }
    }

    private suspend fun requiresHardenedDerivationOnly(networkId: String, userWalletId: UserWalletId): Boolean {
        return requiresHardenedDerivationOnlyUseCase.invoke(
            networkId = networkId,
            userWalletId = userWalletId,
        ).getOrElse { false }
    }

    override fun onCustomDerivationSelected() {
        uiState = stateFactory.updateOnCustomDerivationSelected()
        router.popBackStack()
    }

    override fun onEnterCustomDerivation() {
        uiState = stateFactory.updateStateOnEnterCustomDerivation()
    }

    override fun onCustomDerivationDialogDismissed() {
        uiState = uiState.copy(
            chooseDerivationState = uiState.chooseDerivationState?.copy(
                enterCustomDerivationState = null,
            ),
        )
    }

    override fun onAddCustomButtonClick() {
        viewModelScope.launch(dispatchers.io) {
            val selectedWallet = getSelectedWalletSyncUseCase().getOrNull() ?: return@launch
            val cryptoCurrency = AddCustomTokenStateToCryptoCurrencyConverter(
                selectedWallet.scanResponse.derivationStyleProvider,
            ).convert(uiState)
            val alreadyAdded = isCryptoCurrencyAlreadyAdded(selectedWallet, cryptoCurrency)
            if (alreadyAdded) {
                uiState = stateFactory.getStateAndTriggerEvent(
                    state = uiState,
                    event = Event.ShowAlert(AlertState.TokenAlreadyAdded),
                    setUiState = { uiState = it },
                )
            } else {
                sendTokenAddedEvent(cryptoCurrency)
                addCryptoCurrenciesUseCase(selectedWallet.walletId, currency = cryptoCurrency)
                withContext(dispatchers.main) { router.popBackStack() }
            }
        }
    }

    private suspend fun isCryptoCurrencyAlreadyAdded(
        selectedWallet: UserWallet,
        cryptoCurrency: CryptoCurrency,
    ): Boolean {
        val currenciesList = getCurrenciesUseCase.getSync(selectedWallet.walletId).getOrElse { emptyList() }
        return when (cryptoCurrency) {
            is CryptoCurrency.Coin -> {
                currenciesList.any {
                    it is CryptoCurrency.Coin &&
                        it.id == cryptoCurrency.id &&
                        it.network.derivationPath == cryptoCurrency.network.derivationPath
                }
            }
            is CryptoCurrency.Token -> {
                currenciesList.any {
                    (it as? CryptoCurrency.Token)?.let {
                        it.id == cryptoCurrency.id &&
                            it.contractAddress == cryptoCurrency.contractAddress &&
                            it.network.id == cryptoCurrency.network.id &&
                            it.network.derivationPath == cryptoCurrency.network.derivationPath
                    } ?: false
                }
            }
        }
    }

    private fun sendTokenAddedEvent(cryptoCurrency: CryptoCurrency) {
        val selectedDerivation = uiState.chooseDerivationState?.selectedDerivation

        val derivation = when {
            selectedDerivation == null -> ManageTokens.Derivation.DEFAULT.value
            selectedDerivation.networkName.isNotEmpty() -> selectedDerivation.networkName
            else -> ManageTokens.Derivation.CUSTOM.value
        }
        when (cryptoCurrency) {
            is CryptoCurrency.Token -> {
                analyticsEventHandler.send(
                    ManageTokens.CustomTokenWasAdded(
                        derivation = derivation,
                        networkId = cryptoCurrency.network.name,
                        contractAddress = cryptoCurrency.contractAddress,
                        token = cryptoCurrency.symbol,
                    ),
                )
            }
            is CryptoCurrency.Coin -> {
                analyticsEventHandler.send(
                    ManageTokens.CustomTokenWasAdded(
                        derivation = derivation,
                        networkId = cryptoCurrency.network.name,
                    ),
                )
            }
        }
    }

    override fun onBack() {
        router.popBackStack()
    }
}
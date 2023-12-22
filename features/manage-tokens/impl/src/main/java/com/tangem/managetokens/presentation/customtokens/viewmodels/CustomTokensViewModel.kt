package com.tangem.managetokens.presentation.customtokens.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.HDWalletError
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.tokens.*
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.SelectWalletUseCase
import com.tangem.managetokens.presentation.common.state.AlertState
import com.tangem.managetokens.presentation.common.state.Event
import com.tangem.managetokens.presentation.common.state.NetworkItemState
import com.tangem.managetokens.presentation.customtokens.state.*
import com.tangem.managetokens.presentation.customtokens.state.factory.AddCustomTokenStateToCryptoCurrencyConverter
import com.tangem.managetokens.presentation.customtokens.state.factory.ContractAddressToCustomTokenDataConverter
import com.tangem.managetokens.presentation.customtokens.state.factory.CustomTokensStateFactory
import com.tangem.managetokens.presentation.customtokens.state.factory.FoundTokenToCustomTokenDataConverter
import com.tangem.managetokens.presentation.router.InnerManageTokensRouter
import com.tangem.utils.Provider
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.Debouncer
import com.tangem.utils.coroutines.Debouncer.Companion.DEFAULT_WAIT_TIME_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.properties.Delegates

@Suppress("LongParameterList")
@HiltViewModel
internal class CustomTokensViewModel @Inject constructor(
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
) : ViewModel(), CustomTokensClickIntents, DefaultLifecycleObserver {

    private val debouncer = Debouncer()

    private val stateFactory = CustomTokensStateFactory(
        currentStateProvider = Provider { uiState },
        clickIntents = this,
    )

    var router: InnerManageTokensRouter by Delegates.notNull()

    var uiState: AddCustomTokenState by mutableStateOf(stateFactory.getInitialState())
        private set

    init {
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
        selectNetwork(networkItemState)
        router.popBackStack()
    }

    private fun selectNetwork(networkItemState: NetworkItemState) {
        viewModelScope.launch(dispatchers.io) {
            val selectedWalletId = getSelectedWalletSyncUseCase().getOrNull()?.walletId
            val supportsTokens = areTokensSupportedByNetworkUseCase(
                networkId = networkItemState.id,
                userWalletId = selectedWalletId,
            ).getOrNull() ?: false
            withContext(dispatchers.main) {
                uiState = stateFactory.updateStateOnNetworkSelected(networkItemState, supportsTokens)
            }
        }
    }

    override fun onChooseNetworkClick() {
        router.openCustomTokensChooseNetwork()
    }

    override fun onCloseChoosingNetworkClick() {
        router.popBackStack()
    }

    override fun onWalletSelected(walletId: String) {
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
        router.openCustomTokensChooseWallet()
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
                        val tokenData = ContractAddressToCustomTokenDataConverter(this@CustomTokensViewModel)
                            .convert(contractAddress)
                        uiState = uiState.copy(
                            tokenData = tokenData,
                            warnings = (uiState.warnings + AddCustomTokenWarning.PotentialScamToken).toPersistentSet(),
                        )
                    },
                    ifRight = { token ->
                        val tokenData = if (token != null) {
                            FoundTokenToCustomTokenDataConverter(this@CustomTokensViewModel).convert(token)
                        } else {
                            ContractAddressToCustomTokenDataConverter(this@CustomTokensViewModel).convert(
                                contractAddress,
                            )
                        }
                        uiState = uiState.copy(tokenData = tokenData)
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
                ),
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
                ),
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
                ),
            ),
        )
    }

    override fun onDerivationSelected(derivation: Derivation) {
        uiState =
            uiState.copy(chooseDerivationState = uiState.chooseDerivationState?.copy(selectedDerivation = derivation))
        router.popBackStack()
    }

    override fun onChooseDerivationClick() {
        router.openCustomTokensChooseDerivation()
    }

    override fun onCloseChoosingDerivationClick() {
        router.popBackStack()
    }

    override fun onCustomDerivationChange(input: String) {
        uiState = uiState.copy(
            chooseDerivationState = uiState.chooseDerivationState?.copy(
                enterCustomDerivationState = uiState.chooseDerivationState?.enterCustomDerivationState?.copy(
                    value = input,
                ),
            ),
        )
        debouncer.debounce(waitMs = DEFAULT_WAIT_TIME_MS, coroutineScope = viewModelScope + dispatchers.io) {
            val path = createDerivationPathOrNull(input)
            val enterDerivationState = uiState.chooseDerivationState?.enterCustomDerivationState?.copy(
                confirmButtonEnabled = path != null,
                derivationIncorrect = input.isNotBlank() && path == null,
            )
            uiState = uiState.copy(
                chooseDerivationState = uiState.chooseDerivationState?.copy(
                    enterCustomDerivationState = enterDerivationState,
                ),
            )
        }
    }

    private fun createDerivationPathOrNull(rawPath: String): DerivationPath? {
        return try {
            DerivationPath(rawPath)
        } catch (error: HDWalletError) {
            null
        }
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
            val alreadyAdded =
                getCurrenciesUseCase(selectedWallet.walletId).getOrNull()?.any { it == cryptoCurrency }
            if (alreadyAdded == true) {
                uiState = stateFactory.getStateAndTriggerEvent(
                    state = uiState,
                    event = Event.ShowAlert(AlertState.TokenAlreadyAdded),
                    setUiState = { uiState = it },
                )
            } else {
                addCryptoCurrenciesUseCase(selectedWallet.walletId, currency = cryptoCurrency)
                withContext(dispatchers.main) { router.popBackStack() }
            }
        }
    }

    override fun onBack() {
        router.popBackStack()
    }
}
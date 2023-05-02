package com.tangem.tap.features.customtoken.impl.presentation.viewmodels

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.blockchain.common.Token
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.extensions.isSupportedInApp
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.features.customtoken.impl.domain.CustomTokenInteractor
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet.TestTokenItem
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet.TokensCategoryBlock
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenFloatingButton
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenForm
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenInputField
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenSelectorField
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenTestBlock
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokensToolbar
import com.tangem.tap.features.customtoken.impl.presentation.routers.CustomTokenRouter
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder
import com.tangem.tap.features.customtoken.impl.presentation.validators.ContactAddressValidator
import com.tangem.tap.features.customtoken.impl.presentation.validators.ContractAddressValidatorResult
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for add custom token screen
 *
 * @param featureRouter            feature router
 * @property featureInteractor     feature interactor
 * @property dispatchers           coroutine dispatchers provider
 * @property reduxStateHolder      redux state holder
 * @property analyticsEventHandler analytics event handler
 *
* [REDACTED_AUTHOR]
 */
@HiltViewModel
internal class AddCustomTokenViewModel @Inject constructor(
    featureRouter: CustomTokenRouter,
    private val featureInteractor: CustomTokenInteractor,
    private val dispatchers: AppCoroutineDispatcherProvider,
    private val reduxStateHolder: AppStateHolder,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : ViewModel(), DefaultLifecycleObserver {

    private val actionsHandler = ActionsHandler(featureRouter)
    private val testActionsHandler = TestActionsHandler()

    /** Screen state */
    var uiState by mutableStateOf(getInitialUiState())
        private set

    private var foundTokenId: String? = null

    override fun onCreate(owner: LifecycleOwner) {
        analyticsEventHandler.send(ManageTokens.CustomToken.ScreenOpened)
    }

    private fun getInitialUiState(): AddCustomTokenStateHolder {
        return if (BuildConfig.TEST_ACTION_ENABLED) {
            AddCustomTokenStateHolder.TestContent(
                onBackButtonClick = actionsHandler::onBackButtonClick,
                toolbar = createToolbar(),
                form = createForm(),
                warnings = listOf(),
                floatingButton = createFloatingButton(),
                testBlock = AddCustomTokenTestBlock(
                    chooseTokenButtonText = "Choose token",
                    clearButtonText = "Clear address",
                    resetButtonText = "Reset",
                    onClearAddressButtonClick = testActionsHandler::onClearAddressButtonClick,
                    onResetButtonClick = testActionsHandler::onResetButtonClick,
                ),
                bottomSheet = AddCustomTokenChooseTokenBottomSheet(
                    categoriesBlocks = listOf(
                        TokensCategoryBlock(name = "Common", items = COMMON_TOKENS),
                        TokensCategoryBlock(name = "Solana", items = SOLANA_TOKENS),
                    ),
                    onTestTokenClick = actionsHandler::onContactAddressValueChange,
                ),
            )
        } else {
            AddCustomTokenStateHolder.Content(
                onBackButtonClick = actionsHandler::onBackButtonClick,
                toolbar = createToolbar(),
                form = createForm(),
                warnings = listOf(),
                floatingButton = createFloatingButton(),
            )
        }
    }

    private fun createToolbar(): AddCustomTokensToolbar {
        return AddCustomTokensToolbar(
            title = TextReference.Res(R.string.add_custom_token_title),
            onBackButtonClick = actionsHandler::onBackButtonClick,
        )
    }

    private fun createForm(): AddCustomTokenForm {
        return AddCustomTokenForm(
            contractAddressInputField = createContractAddressInputField(),
            networkSelectorField = createNetworkSelectorField(),
            tokenNameInputField = createTokenNameInputField(),
            tokenSymbolInputField = createTokenSymbolInputField(),
            decimalsInputField = createDecimalsInputField(),
            derivationPathSelectorField = createDerivationPathsSelectorField(),
        )
    }

    private fun createContractAddressInputField(): AddCustomTokenInputField.ContactAddress {
        return AddCustomTokenInputField.ContactAddress(
            value = "",
            onValueChange = actionsHandler::onContactAddressValueChange,
            isError = false,
            isLoading = false,
        )
    }

    private fun createNetworkSelectorField(): AddCustomTokenSelectorField.Network {
        val selectorItems = getNetworkSelectorItems()
        return AddCustomTokenSelectorField.Network(
            selectedItem = requireNotNull(selectorItems.firstOrNull()),
            items = selectorItems,
            onMenuItemClick = {
                actionsHandler.onNetworkSelectorItemClick(
                    selectedItem = requireNotNull(selectorItems.getOrNull(it)),
                )
            },
        )
    }

    private fun getNetworkSelectorItems(): List<AddCustomTokenSelectorField.SelectorItem.Title> {
        val card = reduxStateHolder.scanResponse?.card
        val evmBlockchains = Blockchain.values().filter { card?.isTestCard == it.isTestnet() && it.isEvm() }

        val additionalBlockchains = listOf(
            Blockchain.Binance,
            Blockchain.BinanceTestnet,
            Blockchain.Solana,
            Blockchain.SolanaTestnet,
            Blockchain.Tron,
            Blockchain.TronTestnet,
        )

        return (evmBlockchains + additionalBlockchains)
            .filter { card?.supportedBlockchains()?.contains(it) == true }
            .map(::createNetworkSelectorItem)
            .toMutableList()
            .apply {
                add(index = 0, element = createNetworkSelectorItem(blockchain = Blockchain.Unknown))
            }
    }

    private fun createNetworkSelectorItem(blockchain: Blockchain): AddCustomTokenSelectorField.SelectorItem.Title {
        return when (blockchain) {
            Blockchain.Unknown -> {
                AddCustomTokenSelectorField.SelectorItem.Title(
                    title = TextReference.Res(R.string.custom_token_network_input_not_selected),
                    blockchain = Blockchain.Unknown,
                )
            }

            else -> {
                AddCustomTokenSelectorField.SelectorItem.Title(
                    title = TextReference.Str(blockchain.fullName),
                    blockchain = blockchain,
                )
            }
        }
    }

    private fun createTokenNameInputField(): AddCustomTokenInputField.TokenName {
        return AddCustomTokenInputField.TokenName(
            value = "",
            onValueChange = actionsHandler::onTokenNameValueChange,
            isEnabled = false,
            isError = false,
        )
    }

    private fun createTokenSymbolInputField(): AddCustomTokenInputField.TokenSymbol {
        return AddCustomTokenInputField.TokenSymbol(
            value = "",
            onValueChange = actionsHandler::onTokenSymbolValueChange,
            isEnabled = false,
            isError = false,
        )
    }

    private fun createDecimalsInputField(): AddCustomTokenInputField.Decimals {
        return AddCustomTokenInputField.Decimals(
            value = "",
            onValueChange = actionsHandler::onDecimalsValueChange,
            isEnabled = false,
            isError = false,
        )
    }

    private fun createDerivationPathsSelectorField(): AddCustomTokenSelectorField.DerivationPath? {
        if (reduxStateHolder.scanResponse?.card?.settings?.isHDWalletAllowed == false) return null

        val selectorItems = getDerivationPathsSelectorItems()
        return AddCustomTokenSelectorField.DerivationPath(
            isEnabled = true,
            selectedItem = requireNotNull(selectorItems.firstOrNull()),
            items = selectorItems,
            onMenuItemClick = {
                val field = requireNotNull(uiState.form.derivationPathSelectorField)
                uiState = uiState.copySealed(
                    form = uiState.form.copy(
                        derivationPathSelectorField = field.copy(
                            selectedItem = requireNotNull(selectorItems.getOrNull(it)),
                        ),
                    ),
                )
            },
        )
    }

    private fun getDerivationPathsSelectorItems(): List<AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle> {
        val evmBlockchains = Blockchain.values().filter {
            reduxStateHolder.scanResponse?.card?.isTestCard == it.isTestnet() && it.isEvm() && it.isSupportedInApp()
        }

        return evmBlockchains
            .sortedBy(Blockchain::fullName)
            .map(::createDerivationPathSelectorItem)
            .toMutableList()
            .apply {
                add(index = 0, element = createDerivationPathSelectorItem(Blockchain.Unknown))
            }
    }

    private fun createDerivationPathSelectorItem(
        blockchain: Blockchain,
    ): AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle {
        return when (blockchain) {
            Blockchain.Unknown -> {
                AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle(
                    title = TextReference.Res(R.string.custom_token_derivation_path_default),
                    subtitle = TextReference.Res(R.string.custom_token_derivation_path_default),
                    blockchain = Blockchain.Unknown,
                )
            }

            else -> {
                AddCustomTokenSelectorField.SelectorItem.TitleWithSubtitle(
                    title = blockchain.derivationPath(DerivationStyle.LEGACY)?.rawPath?.let(TextReference::Str)
                        ?: TextReference.Res(R.string.custom_token_derivation_path_default),
                    subtitle = TextReference.Str(blockchain.fullName),
                    blockchain = blockchain,
                )
            }
        }
    }

    private fun createFloatingButton(): AddCustomTokenFloatingButton {
        return AddCustomTokenFloatingButton(isEnabled = false, onClick = actionsHandler::onAddCustomTokenClick)
    }

    private inner class ActionsHandler(private val featureRouter: CustomTokenRouter) {

        fun onBackButtonClick() {
            featureRouter.popBackStack()
        }

        fun onAddCustomTokenClick() {
            if (uiState.form.networkSelectorField.selectedItem.blockchain != Blockchain.Unknown) {
                val selectedNetwork = uiState.form.networkSelectorField.selectedItem.blockchain

                val currency = if (isAnyTokenFieldsFilled() || isAllTokenFieldsFilled()) {
                    Currency.Token(
                        token = Token(
                            name = uiState.form.tokenNameInputField.value,
                            symbol = uiState.form.tokenSymbolInputField.value,
                            contractAddress = uiState.form.contractAddressInputField.value,
                            decimals = uiState.form.decimalsInputField.value.toInt(),
                            id = foundTokenId,
                        ),
                        blockchain = selectedNetwork,
                        derivationPath = getDerivationPath(
                            mainNetwork = selectedNetwork,
                            derivationNetwork = uiState.form.derivationPathSelectorField?.selectedItem?.blockchain,
                            derivationStyle = reduxStateHolder.scanResponse?.card?.derivationStyle,
                        )?.rawPath,
                    )
                } else {
                    Currency.Blockchain(
                        blockchain = selectedNetwork,
                        derivationPath = getDerivationPath(
                            mainNetwork = selectedNetwork,
                            derivationNetwork = uiState.form.derivationPathSelectorField?.selectedItem?.blockchain,
                            derivationStyle = reduxStateHolder.scanResponse?.card?.derivationStyle,
                        )?.rawPath,
                    )
                }

                sendOnAddTokenButtonClick(currency = currency, address = uiState.form.contractAddressInputField.value)

                viewModelScope.launch(dispatchers.io) {
                    featureInteractor.saveToken(
                        currency = currency,
                        address = uiState.form.contractAddressInputField.value,
                    )
                }
            }
        }

        fun onContactAddressValueChange(enteredValue: String) {
            with(uiState.form) {
                val selectedNetwork = networkSelectorField.selectedItem.blockchain
                val isValid = ContactAddressValidator.validate(
                    address = enteredValue,
                    blockchain = selectedNetwork,
                )

                when (isValid) {
                    is ContractAddressValidatorResult.Success -> {
                        uiState = uiState.copySealed(
                            form = uiState.form.copy(
                                contractAddressInputField = contractAddressInputField.copy(
                                    isError = false,
                                    isLoading = true,
                                ),
                            ),
                        )
                        updateForm(address = enteredValue, selectedNetwork = selectedNetwork)
                    }

                    is ContractAddressValidatorResult.Error -> {
                        handleContractAddressErrorValidation(type = isValid.type)
                    }
                }

                updateDerivationPathSelector()
// [REDACTED_TODO_COMMENT]
// [REDACTED_TODO_COMMENT]
            }
        }

        fun onNetworkSelectorItemClick(selectedItem: AddCustomTokenSelectorField.SelectorItem.Title) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    networkSelectorField = uiState.form.networkSelectorField.copy(selectedItem = selectedItem),
                ),
            )
            onContactAddressValueChange(uiState.form.contractAddressInputField.value)
        }

        fun onTokenNameValueChange(enteredValue: String) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    tokenNameInputField = uiState.form.tokenNameInputField.copy(value = enteredValue),
                ),
            )
// [REDACTED_TODO_COMMENT]
        }

        fun onTokenSymbolValueChange(enteredValue: String) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    tokenSymbolInputField = uiState.form.tokenSymbolInputField.copy(value = enteredValue),
                ),
            )
// [REDACTED_TODO_COMMENT]
        }

        fun onDecimalsValueChange(enteredValue: String) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    decimalsInputField = uiState.form.decimalsInputField.copy(value = enteredValue),
                ),
            )
// [REDACTED_TODO_COMMENT]
        }

        private fun getDerivationPath(
            mainNetwork: Blockchain,
            derivationNetwork: Blockchain?,
            derivationStyle: DerivationStyle?,
        ): DerivationPath? {
            val network = if (derivationNetwork == Blockchain.Unknown) mainNetwork else derivationNetwork

            return network?.derivationPath(
                style = if (derivationNetwork == Blockchain.Unknown) derivationStyle else DerivationStyle.LEGACY,
            )
        }

        private fun sendOnAddTokenButtonClick(currency: Currency, address: String) {
            when (currency) {
                is Currency.Blockchain -> {
                    analyticsEventHandler.send(
                        ManageTokens.CustomToken.TokenWasAdded.Blockchain(
                            derivationPath = currency.derivationPath,
                            blockchain = currency.blockchain,
                        ),
                    )
                }

                is Currency.Token -> {
                    analyticsEventHandler.send(
                        ManageTokens.CustomToken.TokenWasAdded.Token(
                            symbol = currency.currencySymbol,
                            derivationPath = currency.derivationPath,
                            blockchain = currency.blockchain,
                            contractAddress = address,
                        ),
                    )
                }
            }
        }

        private fun isAnyTokenFieldsFilled(): Boolean {
            return with(uiState.form) {
                contractAddressInputField.value.isNotEmpty() || tokenNameInputField.value.isNotEmpty() ||
                    tokenSymbolInputField.value.isNotEmpty() || decimalsInputField.value.isNotEmpty()
            }
        }

        private fun isAllTokenFieldsFilled(): Boolean {
            return with(uiState.form) {
                contractAddressInputField.value.isNotEmpty() && tokenNameInputField.value.isNotEmpty() &&
                    tokenSymbolInputField.value.isNotEmpty() && decimalsInputField.value.isNotEmpty()
            }
        }

        private fun updateForm(address: String, selectedNetwork: Blockchain) {
            viewModelScope.launch(dispatchers.main) {
                runCatching(dispatchers.io) {
                    featureInteractor.findToken(address = address, blockchain = selectedNetwork)
                }
                    .onSuccess { token ->
                        with(uiState.form) {
                            uiState = uiState.copySealed(
                                form = copy(
                                    contractAddressInputField = contractAddressInputField.copy(isLoading = false),
                                    networkSelectorField = networkSelectorField.copy(
                                        selectedItem = createNetworkSelectorItem(
                                            blockchain = Blockchain.fromNetworkId(token.network.id)
                                                ?: Blockchain.Unknown,
                                        ),
                                    ),
                                    tokenNameInputField = tokenNameInputField.copy(
                                        value = token.name,
                                        isEnabled = false,
                                    ),
                                    tokenSymbolInputField = tokenSymbolInputField.copy(
                                        value = token.symbol,
                                        isEnabled = false,
                                    ),
                                    decimalsInputField = decimalsInputField.copy(
                                        value = token.network.decimalCount,
                                        isEnabled = false,
                                    ),
                                ),
                            )
                        }
                    }
                    .onFailure {
                        foundTokenId = null
                        Timber.e(it)
                    }
            }
        }

        private fun handleContractAddressErrorValidation(type: AddCustomTokenError) {
            with(uiState.form) {
                val isNetworkSelectorFilled = networkSelectorField.selectedItem.blockchain != Blockchain.Unknown
                val isAnotherTokenFieldsFilled = isAnyTokenFieldsFilled()

                when {
                    isNetworkSelectorFilled && type == AddCustomTokenError.InvalidContractAddress -> {
// [REDACTED_TODO_COMMENT]

                        uiState = uiState.copySealed(
                            form = uiState.form.copy(
                                tokenNameInputField = tokenNameInputField.copy(isEnabled = isAnotherTokenFieldsFilled),
                                tokenSymbolInputField = tokenSymbolInputField.copy(
                                    isEnabled = isAnotherTokenFieldsFilled,
                                ),
                                decimalsInputField = decimalsInputField.copy(isEnabled = isAnotherTokenFieldsFilled),
                            ),
                        )
                    }

                    !isNetworkSelectorFilled || type == AddCustomTokenError.FieldIsEmpty -> {
                        uiState = uiState.copySealed(
                            form = uiState.form.copy(
                                contractAddressInputField = contractAddressInputField.copy(isError = false),
                                tokenNameInputField = tokenNameInputField.copy(value = "", isEnabled = false),
                                tokenSymbolInputField = tokenSymbolInputField.copy(value = "", isEnabled = false),
                                decimalsInputField = decimalsInputField.copy(value = "", isEnabled = false),
                            ),
                        )
                    }

                    else -> Unit
                }
            }
        }

        private fun updateDerivationPathSelector() {
            val selectedValue = uiState.form.derivationPathSelectorField?.selectedItem?.blockchain ?: return
            val isSupported = selectedValue.isEvm() || selectedValue == Blockchain.Unknown

            if (selectedValue != Blockchain.Unknown && !isSupported) {
                uiState = uiState.copySealed(
                    form = uiState.form.copy(
                        derivationPathSelectorField = uiState.form.derivationPathSelectorField?.copy(
                            selectedItem = createDerivationPathSelectorItem(Blockchain.Unknown),
                        ),
                    ),
                )
            }

            if (uiState.form.derivationPathSelectorField?.isEnabled != isSupported) {
                uiState = uiState.copySealed(
                    form = uiState.form.copy(
                        derivationPathSelectorField = uiState.form.derivationPathSelectorField?.copy(
                            isEnabled = isSupported,
                        ),
                    ),
                )
            }
        }
    }

    private inner class TestActionsHandler {

        fun onClearAddressButtonClick() {
            with(uiState.form) {
                uiState = uiState.copySealed(
                    form = copy(
                        contractAddressInputField = contractAddressInputField.copy(value = ""),
                        tokenNameInputField = tokenNameInputField.copy(value = "", isEnabled = false),
                        tokenSymbolInputField = tokenSymbolInputField.copy(value = "", isEnabled = false),
                        decimalsInputField = decimalsInputField.copy(value = "", isEnabled = false),
                    ),
                )
            }
        }

        fun onResetButtonClick() {
            with(uiState.form) {
                uiState = uiState.copySealed(
                    form = copy(
                        contractAddressInputField = contractAddressInputField.copy(value = ""),
                        networkSelectorField = networkSelectorField.copy(
                            selectedItem = requireNotNull(networkSelectorField.items.firstOrNull()),
                        ),
                        tokenNameInputField = tokenNameInputField.copy(value = "", isEnabled = false),
                        tokenSymbolInputField = tokenSymbolInputField.copy(value = "", isEnabled = false),
                        decimalsInputField = decimalsInputField.copy(value = "", isEnabled = false),
                        derivationPathSelectorField = derivationPathSelectorField?.copy(
                            selectedItem = requireNotNull(derivationPathSelectorField.items.firstOrNull()),
                        ),
                    ),
                )
            }
        }
    }

    private companion object {
        val COMMON_TOKENS = persistentListOf(
            TestTokenItem(name = "USDC on ETH", address = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"),
            TestTokenItem(name = "BUSD on ETH", address = "0x4fabb145d64652a948d72533023f6e7a623c7c53"),
            TestTokenItem(name = "ETH on AVALANCHE", address = "0xf20d962a6c8f70c731bd838a3a388d7d48fa6e15"),
            TestTokenItem(name = "USDC on ETH (invalid - cut address)", address = "0xa0b86991c6218b36c1d1"),
            TestTokenItem(name = "Custom EVM", address = "0x1111111111111111112111111111111111111113"),
            TestTokenItem(
                name = "Supported by several networks",
                address = "0xa1faa113cbe53436df28ff0aee54275c13b40975",
            ),
            TestTokenItem(name = "Invalid", address = "!@#_  _-%%^&&*((){P P2iOWsdfFQLA"),
        )

        val SOLANA_TOKENS = persistentListOf(
            TestTokenItem(name = "USDT (full)", address = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8BenwNYB"),
            TestTokenItem(
                name = "USDT (valid - 2/3 of address)",
                address = "Es9vMFrzaCERmJfrF4H2FYD4KCoNkY11McCe8Ben",
            ),
            TestTokenItem(name = "USDT (invalid - 1/3 of address)", address = "Es9vMFrzaCERmJ"),
            TestTokenItem(name = "ETH (full)", address = "2FPyTwcZLUg1MDrwsyoP4D6s1tM7hAkHYRjkNb5w6Pxk"),
        )
    }
}

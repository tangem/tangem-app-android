package com.tangem.tap.features.customtoken.impl.presentation.viewmodels

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.crypto.hdWallet.HDWalletError
import com.tangem.domain.AddCustomTokenError
import com.tangem.domain.common.extensions.*
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.customtoken.impl.domain.CustomTokenInteractor
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.tap.features.customtoken.impl.presentation.models.*
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet.TestTokenItem
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenChooseTokenBottomSheet.TokensCategoryBlock
import com.tangem.tap.features.customtoken.impl.presentation.models.AddCustomTokenSelectorField.SelectorItem
import com.tangem.tap.features.customtoken.impl.presentation.routers.CustomTokenRouter
import com.tangem.tap.features.customtoken.impl.presentation.states.AddCustomTokenStateHolder
import com.tangem.tap.features.customtoken.impl.presentation.validators.ContactAddressValidator
import com.tangem.tap.features.customtoken.impl.presentation.validators.ContractAddressValidatorResult
import com.tangem.tap.features.details.ui.cardsettings.TextReference
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.store
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import com.tangem.utils.coroutines.runCatching
import com.tangem.wallet.BuildConfig
import com.tangem.wallet.R
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel for add custom token screen
 *
 * @param analyticsEventHandler analytics event handler
 * @param featureRouter         feature router
 * @property featureInteractor  feature interactor
 * @property dispatchers        coroutine dispatchers provider
 * @property reduxStateHolder   redux state holder
 *
* [REDACTED_AUTHOR]
 */
@Suppress("LargeClass")
@HiltViewModel
internal class AddCustomTokenViewModel @Inject constructor(
    analyticsEventHandler: AnalyticsEventHandler,
    featureRouter: CustomTokenRouter,
    private val featureInteractor: CustomTokenInteractor,
    private val dispatchers: AppCoroutineDispatcherProvider,
    private val reduxStateHolder: AppStateHolder,
) : ViewModel(), DefaultLifecycleObserver {

    private val analyticsSender = AddCustomTokenAnalyticsSender(analyticsEventHandler)
    private val actionsHandler = ActionsHandler(featureRouter)
    private val testActionsHandler = TestActionsHandler()
    private val formStateBuilder = FormStateBuilder()

    /** Screen state */
    var uiState by mutableStateOf(getInitialUiState())
        private set

    private var foundToken: FoundToken? = null

    override fun onCreate(owner: LifecycleOwner) {
        analyticsSender.sendWhenScreenOpened()
    }

    private fun getInitialUiState(): AddCustomTokenStateHolder {
        return if (BuildConfig.TEST_ACTION_ENABLED) {
            AddCustomTokenStateHolder.TestContent(
                onBackButtonClick = actionsHandler::onBackButtonClick,
                toolbar = createToolbar(),
                form = formStateBuilder.createForm(),
                warnings = emptySet(),
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
                form = formStateBuilder.createForm(),
                warnings = emptySet(),
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

    private fun createFloatingButton(): AddCustomTokenFloatingButton {
        return AddCustomTokenFloatingButton(isEnabled = false, onClick = actionsHandler::onAddCustomTokenClick)
    }

    private inner class FormStateBuilder {

        fun createForm(): AddCustomTokenForm {
            return AddCustomTokenForm(
                contractAddressInputField = createContractAddressInputField(),
                networkSelectorField = createNetworkSelectorField(),
                tokenNameInputField = createTokenNameInputField(),
                tokenSymbolInputField = createTokenSymbolInputField(),
                decimalsInputField = createDecimalsInputField(),
                derivationPathSelectorField = createDerivationPathsSelectorField(),
                derivationPathInputField = createDerivationPathInputField(),
            )
        }

        fun createDerivationPathSelectorAdditionalItem(
            blockchain: Blockchain,
            type: DerivationPathSelectorType = DerivationPathSelectorType.BLOCKCHAIN,
        ): SelectorItem.TitleWithSubtitle {
            return when (type) {
                DerivationPathSelectorType.DEFAULT -> SelectorItem.TitleWithSubtitle(
                    title = TextReference.Res(R.string.custom_token_derivation_path_default),
                    subtitle = TextReference.Res(R.string.custom_token_derivation_path_default),
                    blockchain = Blockchain.Unknown,
                    type = DerivationPathSelectorType.DEFAULT,
                )
                DerivationPathSelectorType.CUSTOM -> SelectorItem.TitleWithSubtitle(
                    title = TextReference.Res(R.string.custom_token_custom_derivation),
                    subtitle = TextReference.Res(R.string.custom_token_custom_derivation),
                    blockchain = Blockchain.Unknown,
                    type = DerivationPathSelectorType.CUSTOM,
                )
                DerivationPathSelectorType.BLOCKCHAIN -> SelectorItem.TitleWithSubtitle(
                    title = blockchain.derivationPath(DerivationStyle.LEGACY)?.rawPath?.let(TextReference::Str)
                        ?: TextReference.Res(R.string.custom_token_derivation_path_default),
                    subtitle = TextReference.Str(blockchain.fullName),
                    blockchain = blockchain,
                )
            }
        }

        fun createNetworkSelectorItem(blockchain: Blockchain): SelectorItem.Title {
            return if (blockchain == Blockchain.Unknown) {
                SelectorItem.Title(
                    title = TextReference.Res(R.string.custom_token_network_input_not_selected),
                    blockchain = Blockchain.Unknown,
                )
            } else {
                SelectorItem.Title(
                    title = TextReference.Str(blockchain.fullName),
                    blockchain = blockchain,
                )
            }
        }

        private fun createContractAddressInputField(): AddCustomTokenInputField.ContactAddress {
            return AddCustomTokenInputField.ContactAddress(
                value = "",
                onValueChange = actionsHandler::onContactAddressValueChange,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_contract_address_input_title),
                placeholder = TextReference.Str(value = CONTRACT_ADDRESS_PLACEHOLDER),
                isLoading = false,
                isError = false,
                error = null,
            )
        }

        private fun createNetworkSelectorField(): AddCustomTokenSelectorField.Network {
            val selectorItems = getNetworkSelectorItems()
            return AddCustomTokenSelectorField.Network(
                label = TextReference.Res(R.string.custom_token_network_input_title),
                selectedItem = requireNotNull(selectorItems.firstOrNull()),
                items = selectorItems,
                onMenuItemClick = actionsHandler::onNetworkSelectorItemClick,
            )
        }

        private fun getNetworkSelectorItems(): List<SelectorItem.Title> {
            val defaultNetwork = createNetworkSelectorItem(blockchain = Blockchain.Unknown)
            val scanResponse = reduxStateHolder.scanResponse
            return listOf(defaultNetwork) + Blockchain.values()
                .filter { blockchain ->
                    scanResponse?.card?.supportedBlockchains(scanResponse.cardTypesResolver)
                        ?.contains(blockchain) == true
                }
                .sortedBy(Blockchain::fullName)
                .map(::createNetworkSelectorItem)
        }

        private fun createTokenNameInputField(): AddCustomTokenInputField.TokenName {
            return AddCustomTokenInputField.TokenName(
                value = "",
                onValueChange = actionsHandler::onTokenNameValueChange,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_name_input_title),
                placeholder = TextReference.Res(id = R.string.custom_token_name_input_placeholder),
                isEnabled = false,
            )
        }

        private fun createTokenSymbolInputField(): AddCustomTokenInputField.TokenSymbol {
            return AddCustomTokenInputField.TokenSymbol(
                value = "",
                onValueChange = actionsHandler::onTokenSymbolValueChange,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_token_symbol_input_title),
                placeholder = TextReference.Res(id = R.string.custom_token_token_symbol_input_placeholder),
                isEnabled = false,
            )
        }

        private fun createDecimalsInputField(): AddCustomTokenInputField.Decimals {
            return AddCustomTokenInputField.Decimals(
                value = "",
                onValueChange = actionsHandler::onDecimalsValueChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                label = TextReference.Res(R.string.custom_token_decimals_input_title),
                placeholder = TextReference.Str(value = DECIMALS_PLACEHOLDER),
                isEnabled = false,
            )
        }

        private fun createDerivationPathsSelectorField(): AddCustomTokenSelectorField.DerivationPath? {
            if (reduxStateHolder.scanResponse?.card?.settings?.isHDWalletAllowed == false) return null

            val selectorItems = getDerivationPathsSelectorItems()
            return AddCustomTokenSelectorField.DerivationPath(
                label = TextReference.Res(R.string.custom_token_derivation_path_input_title),
                selectedItem = requireNotNull(selectorItems.firstOrNull()),
                items = selectorItems,
                onMenuItemClick = actionsHandler::onDerivationPathSelectorItemClick,
                isEnabled = true,
            )
        }

        private fun getDerivationPathsSelectorItems(): List<SelectorItem.TitleWithSubtitle> {
            return listOf(
                createDerivationPathSelectorAdditionalItem(
                    blockchain = Blockchain.Unknown,
                    type = DerivationPathSelectorType.DEFAULT,
                ),
                createDerivationPathSelectorAdditionalItem(
                    blockchain = Blockchain.Unknown,
                    type = DerivationPathSelectorType.CUSTOM,
                ),
            ) + Blockchain.values()
                .filter { blockchain ->
                    blockchain.isSupportedInApp() && !blockchain.isTestnet()
                }
                .sortedBy(Blockchain::fullName)
                .map(::createDerivationPathSelectorAdditionalItem)
        }

        private fun createDerivationPathInputField(): AddCustomTokenInputField.DerivationPath? {
            if (reduxStateHolder.scanResponse?.card?.settings?.isHDWalletAllowed == false) return null

            return AddCustomTokenInputField.DerivationPath(
                value = "",
                onValueChange = actionsHandler::onDerivationPathValueChange,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                label = TextReference.Res(R.string.custom_token_custom_derivation),
                placeholder = TextReference.Str(value = DERIVATION_PATH_PLACEHOLDER),
            )
        }
    }

    private fun updateForm(address: String, selectedNetwork: Blockchain) {
        viewModelScope.launch(dispatchers.main) {
            runCatching(dispatchers.io) {
                featureInteractor.findToken(address = address, blockchain = selectedNetwork)
            }
                .onSuccess { token ->
                    foundToken = token
                    uiState = uiState.copySealed(
                        form = uiState.form.copy(
                            contractAddressInputField = uiState.form.contractAddressInputField.copy(
                                isLoading = false,
                            ),
                            networkSelectorField = uiState.form.networkSelectorField.copy(
                                selectedItem = formStateBuilder.createNetworkSelectorItem(
                                    blockchain = Blockchain.fromNetworkId(token.network.id)
                                        ?: Blockchain.Unknown,
                                ),
                            ),
                            tokenNameInputField = uiState.form.tokenNameInputField.copy(
                                value = token.name,
                                isEnabled = false,
                            ),
                            tokenSymbolInputField = uiState.form.tokenSymbolInputField.copy(
                                value = token.symbol,
                                isEnabled = false,
                            ),
                            decimalsInputField = uiState.form.decimalsInputField.copy(
                                value = token.network.decimalCount,
                                isEnabled = false,
                            ),
                        ),
                    )
                }
                .onFailure {
                    foundToken = null
                    uiState = uiState.copySealed(
                        form = uiState.form.copy(
                            contractAddressInputField = uiState.form.contractAddressInputField.copy(
                                isLoading = false,
                            ),
                            tokenNameInputField = uiState.form.tokenNameInputField.copy(isEnabled = true),
                            tokenSymbolInputField = uiState.form.tokenSymbolInputField.copy(isEnabled = true),
                            decimalsInputField = uiState.form.decimalsInputField.copy(isEnabled = true),
                        ),
                    )
                    Timber.e(it)
                }

            updateWarnings()
            updateFloatingButton()
        }
    }

    private fun isDerivationPathSelected(): Boolean {
        return with(uiState.form.derivationPathSelectorField?.selectedItem?.blockchain) {
            this != null && this != Blockchain.Unknown ||
                uiState.form.derivationPathSelectorField?.selectedItem?.type == DerivationPathSelectorType.CUSTOM &&
                !uiState.warnings.contains(AddCustomTokenWarning.WrongDerivationPath)
        }
    }

    private fun updateWarnings() {
        uiState = uiState.copySealed(
            warnings = buildSet {
                when (getCustomTokenType()) {
                    CustomTokenType.TOKEN -> {
                        addAll(getTokenWarningSet())
                    }

                    CustomTokenType.BLOCKCHAIN -> {
                        if (isCustomTokenAlreadyAdded()) add(AddCustomTokenWarning.TokenAlreadyAdded)
                        if (isDerivationPathSelected()) add(AddCustomTokenWarning.PotentialScamToken)
                    }
                }
            },
        )
    }

    private fun getCustomTokenType(): CustomTokenType {
        return if (isAnyTokenFieldsFilled() || isAllTokenFieldsFilled()) {
            CustomTokenType.TOKEN
        } else {
            CustomTokenType.BLOCKCHAIN
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

    private fun getTokenWarningSet(): Set<AddCustomTokenWarning> {
        val networkSelectorValue = uiState.form.networkSelectorField.selectedItem.blockchain

        val isContractAddressFieldEmpty = ContactAddressValidator.validate(
            address = uiState.form.contractAddressInputField.value,
            blockchain = networkSelectorValue,
        ).let {
            it is ContractAddressValidatorResult.Error && it.type == AddCustomTokenError.FieldIsEmpty
        }

        val isSupportedToken = if (!isNetworkSelected()) {
            true
        } else {
            val scanResponse = reduxStateHolder.scanResponse
            scanResponse?.card?.canHandleToken(
                blockchain = networkSelectorValue,
                cardTypesResolver = scanResponse.cardTypesResolver,
            ) ?: false
        }

        return buildSet {
            if (!isSupportedToken && !isContractAddressFieldEmpty) add(AddCustomTokenWarning.UnsupportedSolanaToken)
            if (isCustomTokenAlreadyAdded()) add(AddCustomTokenWarning.TokenAlreadyAdded)
            if (foundToken == null && isAnyTokenFieldsFilled() || foundToken?.isActive == false) {
                add(AddCustomTokenWarning.PotentialScamToken)
            }
        }
    }

    private fun isNetworkSelected(): Boolean {
        return uiState.form.networkSelectorField.selectedItem.blockchain != Blockchain.Unknown
    }

    private fun updateFloatingButton() {
        uiState = updateStateWithDerivationError(uiState)
        if (isCustomTokenAlreadyAdded()) {
            uiState = uiState.copySealed(
                warnings = uiState.warnings + AddCustomTokenWarning.TokenAlreadyAdded,
                floatingButton = uiState.floatingButton.copy(isEnabled = false),
            )
            return
        }

        val isCorrectDerivationInput = !uiState.warnings.contains(AddCustomTokenWarning.WrongDerivationPath)
        val state = when {
            isAllTokenFieldsFilled() && isNetworkSelected() -> {
                val networkSelectorValue = uiState.form.networkSelectorField.selectedItem.blockchain
                val error = ContactAddressValidator.validate(
                    address = uiState.form.contractAddressInputField.value,
                    blockchain = networkSelectorValue,
                )
                val scanResponse = reduxStateHolder.scanResponse
                val isSupportedToken = scanResponse?.card
                    ?.canHandleToken(
                        blockchain = networkSelectorValue,
                        cardTypesResolver = scanResponse.cardTypesResolver,
                    )
                    ?: false

                uiState.copySealed(
                    floatingButton = uiState.floatingButton.copy(
                        isEnabled = error is ContractAddressValidatorResult.Success &&
                            isSupportedToken && isCorrectDerivationInput,
                    ),
                )
            }

            isAnyTokenFieldsFilled() -> {
                uiState.copySealed(floatingButton = uiState.floatingButton.copy(isEnabled = false))
            }

            else -> {
                uiState.copySealed(
                    floatingButton = uiState.floatingButton.copy(
                        isEnabled = if (isNetworkSelected()) {
                            !isBlockchainAlreadyAdded() && isCorrectDerivationInput
                        } else {
                            false
                        },
                    ),
                )
            }
        }

        uiState = state.copySealed(
            warnings = state.warnings - AddCustomTokenWarning.TokenAlreadyAdded,
        )
    }

    private fun updateStateWithDerivationError(uiState: AddCustomTokenStateHolder): AddCustomTokenStateHolder {
        val updatedWarnings = if (isWrongDerivationPathEntered(
                derivationPathSelectorType = uiState.form.derivationPathSelectorField?.selectedItem?.type,
                derivationPath = getDerivationPath(),
            )
        ) {
            uiState.warnings + AddCustomTokenWarning.WrongDerivationPath
        } else {
            uiState.warnings - AddCustomTokenWarning.WrongDerivationPath
        }
        return uiState.copySealed(
            warnings = updatedWarnings,
        )
    }

    private fun isWrongDerivationPathEntered(
        derivationPathSelectorType: DerivationPathSelectorType?,
        derivationPath: DerivationPath?,
    ): Boolean {
        return derivationPathSelectorType == DerivationPathSelectorType.CUSTOM && derivationPath == null
    }

    private fun isCustomTokenAlreadyAdded(): Boolean {
        return when (getCustomTokenType()) {
            CustomTokenType.TOKEN -> isTokenAlreadyAdded()
            CustomTokenType.BLOCKCHAIN -> isBlockchainAlreadyAdded()
        }
    }

    private fun isTokenAlreadyAdded(): Boolean {
        return store.state.walletState.walletsStores
            .map { walletStore -> walletStore.walletsData.map(WalletDataModel::currency) }
            .flatten()
            .filterIsInstance<Currency.Token>()
            .any { wrappedCurrency ->
                val contractAddress = uiState.form.contractAddressInputField.value
                val networkSelectorValue = uiState.form.networkSelectorField.selectedItem.blockchain
                val sameId = foundToken?.id == wrappedCurrency.token.id
                val sameAddress = contractAddress == wrappedCurrency.token.contractAddress
                val sameBlockchain =
                    Blockchain.fromNetworkId(networkSelectorValue.toNetworkId()) == wrappedCurrency.blockchain
                val isSameDerivationPath = getDerivationPath().isSameDerivationPath(wrappedCurrency.derivationPath)
                sameId && sameAddress && sameBlockchain && isSameDerivationPath
            }
    }

    private fun isBlockchainAlreadyAdded(): Boolean {
        return store.state.walletState.walletsStores
            .map { walletStore -> walletStore.walletsData.map(WalletDataModel::currency) }
            .flatten()
            .filterIsInstance<Currency.Blockchain>()
            .any {
                val networkSelectorValue = uiState.form.networkSelectorField.selectedItem.blockchain
                networkSelectorValue == it.blockchain &&
                    getDerivationPath().isSameDerivationPath(it.derivationPath)
            }
    }

    private fun DerivationPath?.isSameDerivationPath(rawDerivationPath: String?): Boolean {
        return this == rawDerivationPath?.let { DerivationPath(it) }
    }

    private fun handleContractAddressErrorValidation(type: AddCustomTokenError) {
        when {
            isNetworkSelected() && type == AddCustomTokenError.InvalidContractAddress -> {
                val isAnotherTokenFieldsFilled = isAnyTokenFieldsFilled()
                uiState = uiState.copySealed(
                    form = uiState.form.copy(
                        contractAddressInputField = uiState.form.contractAddressInputField.copy(
                            isError = true,
                            error = TextReference.Res(
                                id = R.string.custom_token_creation_error_invalid_contract_address,
                            ),
                        ),
                        tokenNameInputField = uiState.form.tokenNameInputField.copy(
                            isEnabled = isAnotherTokenFieldsFilled,
                        ),
                        tokenSymbolInputField = uiState.form.tokenSymbolInputField.copy(
                            isEnabled = isAnotherTokenFieldsFilled,
                        ),
                        decimalsInputField = uiState.form.decimalsInputField.copy(
                            isEnabled = isAnotherTokenFieldsFilled,
                        ),
                    ),
                )
            }

            !isNetworkSelected() || type == AddCustomTokenError.FieldIsEmpty -> {
                uiState = uiState.copySealed(
                    form = uiState.form.copy(
                        contractAddressInputField = uiState.form.contractAddressInputField.copy(isError = false),
                        tokenNameInputField = uiState.form.tokenNameInputField.copy(value = "", isEnabled = false),
                        tokenSymbolInputField = uiState.form.tokenSymbolInputField.copy(
                            value = "",
                            isEnabled = false,
                        ),
                        decimalsInputField = uiState.form.decimalsInputField.copy(value = "", isEnabled = false),
                    ),
                )
            }

            else -> Unit
        }
    }

    private fun getDerivationPath(): DerivationPath? {
        return when (uiState.form.derivationPathSelectorField?.selectedItem?.type) {
            DerivationPathSelectorType.CUSTOM ->
                createDerivationPathOrNull(uiState.form.derivationPathInputField?.value ?: "")
            else ->
                getDerivationPathForBlockchain(uiState.form.derivationPathSelectorField?.selectedItem?.blockchain)
        }
    }

    private fun createDerivationPathOrNull(rawPath: String): DerivationPath? {
        return try {
            DerivationPath(rawPath)
        } catch (error: HDWalletError) {
            null
        }
    }

    private fun getDerivationPathForBlockchain(blockchain: Blockchain?): DerivationPath? {
        if (blockchain == null) return null

        val derivationStyle = if (!isDerivationPathSelected()) {
            reduxStateHolder.scanResponse?.derivationStyleProvider?.getDerivationStyle()
        } else {
            DerivationStyle.LEGACY
        }
        val derivationNetwork = if (blockchain == Blockchain.Unknown) {
            uiState.form.networkSelectorField.selectedItem.blockchain
        } else {
            blockchain
        }
        return derivationNetwork.derivationPath(derivationStyle)
    }

    private inner class ActionsHandler(private val featureRouter: CustomTokenRouter) {

        fun onBackButtonClick() {
            viewModelScope.launch(dispatchers.main) {
                // need delay before close, cause crashed in compose PopUpMenu as
                delay(timeMillis = 100)
                featureRouter.popBackStack()
            }
        }

        fun onContactAddressValueChange(enteredValue: String) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    contractAddressInputField = uiState.form.contractAddressInputField.copy(value = enteredValue),
                ),
            )

            val selectedNetwork = uiState.form.networkSelectorField.selectedItem.blockchain
            val validatorResult = ContactAddressValidator.validate(
                address = enteredValue,
                blockchain = selectedNetwork,
            )

            when (validatorResult) {
                is ContractAddressValidatorResult.Success -> {
                    uiState = uiState.copySealed(
                        form = uiState.form.copy(
                            contractAddressInputField = uiState.form.contractAddressInputField.copy(
                                isError = false,
                                isLoading = true,
                            ),
                        ),
                    )
                    updateForm(address = enteredValue, selectedNetwork = selectedNetwork)
                }

                is ContractAddressValidatorResult.Error -> {
                    handleContractAddressErrorValidation(type = validatorResult.type)
                    updateWarnings()
                    updateFloatingButton()
                }
            }
        }

        fun onNetworkSelectorItemClick(index: Int) {
            val selectedItem = requireNotNull(uiState.form.networkSelectorField.items.getOrNull(index))
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    networkSelectorField = uiState.form.networkSelectorField.copy(
                        selectedItem = selectedItem,
                    ),
                    showTokenFields = selectedItem.blockchain.canHandleTokens(),
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
            updateFloatingButton()
        }

        fun onTokenSymbolValueChange(enteredValue: String) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    tokenSymbolInputField = uiState.form.tokenSymbolInputField.copy(value = enteredValue),
                ),
            )
            updateFloatingButton()
        }

        fun onDecimalsValueChange(enteredValue: String) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    decimalsInputField = uiState.form.decimalsInputField.copy(value = enteredValue),
                ),
            )
            updateFloatingButton()
        }

        fun onDerivationPathSelectorItemClick(index: Int) {
            val derivationSelector = requireNotNull(uiState.form.derivationPathSelectorField)
            val selected = requireNotNull(derivationSelector.items.getOrNull(index))
            val derivationInputField = requireNotNull(uiState.form.derivationPathInputField)
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    derivationPathSelectorField = derivationSelector.copy(
                        selectedItem = selected,
                    ),
                    derivationPathInputField = derivationInputField.copy(
                        showField = selected.type == DerivationPathSelectorType.CUSTOM,
                    ),
                ),
            )
            updateFloatingButton()
        }

        fun onDerivationPathValueChange(enteredValue: String) {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    derivationPathInputField = uiState.form.derivationPathInputField?.copy(value = enteredValue),
                ),
            )
            updateFloatingButton()
        }

        fun onAddCustomTokenClick() {
            if (!isNetworkSelected()) return

            val currency = when (getCustomTokenType()) {
                CustomTokenType.TOKEN -> {
                    CustomCurrency.CustomToken(
                        token = Token(
                            name = uiState.form.tokenNameInputField.value,
                            symbol = uiState.form.tokenSymbolInputField.value,
                            contractAddress = uiState.form.contractAddressInputField.value,
                            decimals = requireNotNull(uiState.form.decimalsInputField.value.toIntOrNull()),
                            id = foundToken?.id,
                        ),
                        network = uiState.form.networkSelectorField.selectedItem.blockchain,
                        derivationPath = getDerivationPath(),
                    )
                }
                CustomTokenType.BLOCKCHAIN -> {
                    CustomCurrency.CustomBlockchain(
                        network = uiState.form.networkSelectorField.selectedItem.blockchain,
                        derivationPath = getDerivationPath(),
                    )
                }
            }

            analyticsSender.sendWhenAddTokenButtonClicked(currency)

            viewModelScope.launch(dispatchers.io) {
                runCatching { featureInteractor.saveToken(currency) }
                    .onSuccess { featureRouter.openWalletScreen() }
                    .onFailure(Timber::e)
            }
        }
    }

    private inner class TestActionsHandler {

        fun onClearAddressButtonClick() {
            uiState = uiState.copySealed(
                form = uiState.form.copy(
                    contractAddressInputField = uiState.form.contractAddressInputField.copy(
                        value = "",
                        isLoading = false,
                        isError = false,
                        error = null,
                    ),
                ),
            )
        }

        fun onResetButtonClick() {
            with(uiState.form) {
                uiState = uiState.copySealed(
                    form = uiState.form.copy(
                        contractAddressInputField = contractAddressInputField.copy(
                            value = "",
                            isLoading = false,
                            isError = false,
                            error = null,
                        ),
                        networkSelectorField = networkSelectorField.copy(
                            selectedItem = formStateBuilder.createNetworkSelectorItem(blockchain = Blockchain.Unknown),
                        ),
                        tokenNameInputField = tokenNameInputField.copy(value = "", isEnabled = false),
                        tokenSymbolInputField = tokenSymbolInputField.copy(value = "", isEnabled = false),
                        decimalsInputField = decimalsInputField.copy(value = "", isEnabled = false),
                        derivationPathSelectorField = derivationPathSelectorField?.copy(
                            isEnabled = true,
                            selectedItem = formStateBuilder
                                .createDerivationPathSelectorAdditionalItem(
                                    blockchain = Blockchain.Unknown,
                                    type = DerivationPathSelectorType.DEFAULT,
                                ),
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
        const val DERIVATION_PATH_PLACEHOLDER = "m/44'/0'/0'/0/0"
        const val DECIMALS_PLACEHOLDER = "8"
        const val CONTRACT_ADDRESS_PLACEHOLDER = "0x0000000000000000000000000000000000000000"
    }
}

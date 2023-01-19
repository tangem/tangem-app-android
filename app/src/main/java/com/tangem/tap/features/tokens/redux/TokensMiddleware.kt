package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.common.flatMap
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.domain.DomainWrapped
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.redux.domainStore
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.assetReader
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.tokens.LoadAvailableCoinsService
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

@Suppress("LargeClass")
class TokensMiddleware {

    val tokensMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is TokensAction.LoadCurrencies -> handleLoadCurrencies(action.scanResponse)
                    is TokensAction.SaveChanges -> handleSaveChanges(action)
                    is TokensAction.PrepareAndNavigateToAddCustomToken -> handleAddingCustomToken()
                    is TokensAction.SetSearchInput -> {
                        Analytics.send(ManageTokens.TokenSearched())
                        handleLoadCurrencies(
                            scanResponse = store.state.globalState.scanResponse,
                            newSearchInput = action.searchInput,
                        )
                    }
                    is TokensAction.LoadMore -> {
                        if (store.state.tokensState.needToLoadMore &&
                            store.state.tokensState.currencies.isNotEmpty()
                        ) {
                            handleLoadCurrencies(action.scanResponse)
                        }
                    }
                }
                next(action)
            }
        }
    }

    private fun handleLoadCurrencies(scanResponse: ScanResponse?, newSearchInput: String? = null) {
        val tokensState = store.state.tokensState

        val isTestcard = scanResponse?.card?.isTestCard ?: false

        val supportedBlockchains: List<Blockchain> =
            scanResponse?.card?.supportedBlockchains() ?: Blockchain.values().toList()
                .filter { !it.isTestnet() }

        val loadCoinsService = LoadAvailableCoinsService(
            tangemTechApi = store.state.domainNetworks.tangemTechService.api,
            dispatchers = AppCoroutineDispatcherProvider(),
            assetReader = assetReader,
        )

        scope.launch {
            val loadCoinsResult = if (newSearchInput == null) {
                loadCoinsService.getSupportedTokens(
                    isTestcard,
                    supportedBlockchains,
                    tokensState.pageToLoad,
                    tokensState.searchInput,
                )
            } else {
                loadCoinsService.getSupportedTokens(
                    isTestNet = isTestcard,
                    supportedBlockchains = supportedBlockchains,
                    page = 0,
                    searchInput = newSearchInput.ifBlank { null },
                )
            }
            when (loadCoinsResult) {
                is Result.Success -> {
                    val currencies = loadCoinsResult.data.currencies
                        .filter(supportedBlockchains.toSet())
                    store.dispatchOnMain(
                        TokensAction.LoadCurrencies.Success(
                            currencies,
                            loadCoinsResult.data.moreAvailable,
                        ),
                    )
                }
                is Result.Failure -> store.dispatchOnMain(TokensAction.LoadCurrencies.Failure)
            }
        }
    }

    private fun handleSaveChanges(action: TokensAction.SaveChanges) = scope.launch {
        val scanResponse = store.state.globalState.scanResponse ?: return@launch

        val currentTokens = store.state.tokensState.addedWallets.toNonCustomTokensWithBlockchains(
            scanResponse.card.derivationStyle,
        )
        val currentBlockchains = store.state.tokensState.addedWallets.toNonCustomBlockchains(
            scanResponse.card.derivationStyle,
        )

        val blockchainsToAdd = action.addedBlockchains.filter { !currentBlockchains.contains(it) }
        val blockchainsToRemove =
            currentBlockchains.filter { !action.addedBlockchains.contains(it) }

        val tokensToAdd = action.addedTokens.filter { !currentTokens.contains(it) }
        val tokensToRemove = currentTokens.filter { token ->
            !action.addedTokens.any { it.token == token.token }
        }
        val derivationStyle = scanResponse.card.derivationStyle

        removeCurrenciesIfNeeded(
            convertToCurrencies(
                blockchains = blockchainsToRemove,
                tokens = tokensToRemove,
                derivationStyle = derivationStyle,
            ),
        )

        @Suppress("ComplexCondition")
        if (tokensToAdd.isEmpty() && tokensToRemove.isEmpty() &&
            blockchainsToAdd.isEmpty() && blockchainsToRemove.isEmpty()
        ) {
            store.dispatchDebugErrorNotification("Nothing to save")
            store.dispatchOnMain(NavigationAction.PopBackTo())
            return@launch
        }

        val currencyList = convertToCurrencies(
            blockchains = blockchainsToAdd,
            tokens = tokensToAdd,
            derivationStyle = derivationStyle,
        )
        if (scanResponse.supportsHdWallet()) {
            deriveMissingBlockchains(scanResponse, currencyList) {
                submitAdd(it, currencyList)
                store.dispatchOnMain(NavigationAction.PopBackTo())
            }
        } else {
            submitAdd(scanResponse, currencyList)
            store.dispatchOnMain(NavigationAction.PopBackTo())
        }
    }

    private fun convertToCurrencies(
        blockchains: List<Blockchain>,
        tokens: List<TokenWithBlockchain>,
        derivationStyle: DerivationStyle?,
    ): List<Currency> {
        return blockchains.map {
            Currency.Blockchain(it, it.derivationPath(derivationStyle)?.rawPath)
        } + tokens.map {
            Currency.Token(
                it.token,
                it.blockchain,
                it.blockchain.derivationPath(derivationStyle)?.rawPath,
            )
        }
    }

    fun deriveMissingBlockchains(
        scanResponse: ScanResponse,
        currencyList: List<Currency>,
        onSuccess: (ScanResponse) -> Unit,
    ) {
        val derivationDataList = listOfNotNull(
            getDerivations(EllipticCurve.Secp256k1, scanResponse, currencyList),
            getDerivations(EllipticCurve.Ed25519, scanResponse, currencyList),
        )
        val derivations = derivationDataList.associate { it.derivations }
        if (derivations.isEmpty()) {
            onSuccess(scanResponse)
            return
        }

        scope.launch {
            val card = scanResponse.card
            val result = tangemSdkManager.derivePublicKeys(
                cardId = card.cardId,
                derivations = derivations,
            )
            when (result) {
                is CompletionResult.Success -> {
                    val newDerivedKeys = result.data.entries
                    val oldDerivedKeys = scanResponse.derivedKeys

                    val walletKeys = (newDerivedKeys.keys + oldDerivedKeys.keys).toSet()

                    val updatedDerivedKeys = walletKeys.associateWith { walletKey ->
                        val oldDerivations = ExtendedPublicKeysMap(oldDerivedKeys[walletKey] ?: emptyMap())
                        val newDerivations = newDerivedKeys[walletKey] ?: ExtendedPublicKeysMap(emptyMap())
                        ExtendedPublicKeysMap(oldDerivations + newDerivations)
                    }
                    val updatedScanResponse = scanResponse.copy(
                        derivedKeys = updatedDerivedKeys,
                    )
                    store.dispatchOnMain(GlobalAction.SaveScanResponse(updatedScanResponse))
                    delay(DELAY_SDK_DIALOG_CLOSE)

                    onSuccess(updatedScanResponse)
                }

                is CompletionResult.Failure -> {
                    store.dispatchDebugErrorNotification(TapError.CustomError("Error adding tokens"))
                }
            }
        }
    }

    private fun getDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currencyList: List<Currency>,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val manageTokensCandidates = currencyList.map { it.blockchain }.distinct().filter {
            it.getSupportedCurves().contains(curve)
        }.mapNotNull {
            it.derivationPath(scanResponse.card.derivationStyle)
        }

        val customTokensCandidates = currencyList.filter {
            it.blockchain.getSupportedCurves().contains(curve)
        }.mapNotNull { it.derivationPath }.map { DerivationPath(it) }

        val bothCandidates = (manageTokensCandidates + customTokensCandidates).distinct()
        if (bothCandidates.isEmpty()) return null

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys: ExtendedPublicKeysMap =
            scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = bothCandidates.filterNot { alreadyDerivedPaths.contains(it) }
        if (toDerive.isEmpty()) return null

        return DerivationData(
            derivations = mapKeyOfWalletPublicKey to toDerive,
            alreadyDerivedKeys = alreadyDerivedKeys,
            mapKeyOfWalletPublicKey = mapKeyOfWalletPublicKey,
        )
    }

    private class DerivationData(
        val derivations: Pair<ByteArrayKey, List<DerivationPath>>,
        val alreadyDerivedKeys: ExtendedPublicKeysMap,
        val mapKeyOfWalletPublicKey: ByteArrayKey,
    )

    private fun submitAdd(scanResponse: ScanResponse, currencyList: List<Currency>) {
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync
        if (selectedUserWallet != null) {
            scope.launch {
                userWalletsListManager.update(
                    userWalletId = selectedUserWallet.walletId,
                    update = { userWallet ->
                        userWallet.copy(scanResponse = scanResponse)
                    },
                )
                    .flatMap { updatedUserWallet ->
                        walletCurrenciesManager.addCurrencies(
                            userWallet = updatedUserWallet,
                            currenciesToAdd = currencyList,
                        )
                    }
            }
        } else {
            val factory = store.state.globalState.tapWalletManager.walletManagerFactory
            val derivationStyle = scanResponse.card.derivationStyle

            val addActions = currencyList.mapIndexedNotNull { index, currency ->
                when (currency) {
                    is Currency.Blockchain -> {
                        val derivationPath = currency.derivationPath?.let { DerivationPath(it) }
                        val derivationParams = derivationStyle?.let {
                            when (derivationPath) {
                                null -> DerivationParams.Default(derivationStyle)
                                else -> DerivationParams.Custom(derivationPath)
                            }
                        }
                        val walletManager = factory.makeWalletManagerForApp(
                            scanResponse = scanResponse,
                            blockchain = currency.blockchain,
                            derivationParams = derivationParams,
                        ) ?: return@mapIndexedNotNull null
                        WalletAction.MultiWallet.AddBlockchain(
                            blockchain = BlockchainNetwork.fromWalletManager(walletManager),
                            walletManager = walletManager,
                            save = index == currencyList.lastIndex,
                        )
                    }
                    is Currency.Token -> {
                        val rawDerivationPath = currency.derivationPath
                            ?: currency.blockchain.derivationPath(derivationStyle)?.rawPath
                        val blockchainNetwork =
                            BlockchainNetwork(currency.blockchain, rawDerivationPath, listOf(currency.token))
                        WalletAction.MultiWallet.AddToken(
                            token = currency.token,
                            blockchain = blockchainNetwork,
                            save = index == currencyList.lastIndex,
                        )
                    }
                }
            }
            addActions.forEach { store.dispatchOnMain(it) }
        }
    }

    private suspend fun removeCurrenciesIfNeeded(currencies: List<Currency>) {
        when {
            currencies.isEmpty() -> Unit
            userWalletsListManager.hasSavedUserWallets -> {
                walletCurrenciesManager.removeCurrencies(
                    userWallet = userWalletsListManager.selectedUserWalletSync!!,
                    currenciesToRemove = currencies,
                )
            }
            else -> {
                store.dispatch(WalletAction.MultiWallet.RemoveWallets(currencies))
            }
        }
    }

    private fun isNeedToDerive(scanResponse: ScanResponse, currency: Currency): Boolean {
        return currency.derivationPath?.let {
            !scanResponse.hasDerivation(currency.blockchain, it)
        } ?: false
    }

    private fun handleAddingCustomToken() = scope.launch {
        val onAddCustomToken = fun(customCurrency: CustomCurrency) {
            val scanResponse = store.state.globalState.scanResponse ?: return

            fun submitAndPopBack(scanResponse: ScanResponse, currencyList: List<Currency>) {
                submitAdd(scanResponse, currencyList)
                // pop from the AddCustomTokenScreen
                store.dispatchOnMain(NavigationAction.PopBackTo())
                store.dispatchOnMain(NavigationAction.PopBackTo())
            }

            Analytics.send(ManageTokens.CustomToken.TokenWasAdded(customCurrency))
            val currency = Currency.fromCustomCurrency(customCurrency)
            val isNeedToDerive = isNeedToDerive(scanResponse, currency)
            val currencyList = listOf(currency)
            if (isNeedToDerive) {
                deriveMissingBlockchains(scanResponse, currencyList) {
                    submitAndPopBack(it, currencyList)
                }
            } else {
                submitAndPopBack(scanResponse, currencyList)
            }
        }

        val addedCurrencies = store.state.walletState.walletsStores.map { walletStore ->
            walletStore.walletsData.map { walletData -> walletData.currency }
        }.flatten().map {
            when (it) {
                is Currency.Blockchain -> DomainWrapped.Currency.Blockchain(
                    it.blockchain,
                    it.derivationPath,
                )
                is Currency.Token -> DomainWrapped.Currency.Token(
                    it.token,
                    it.blockchain,
                    it.derivationPath,
                )
            }
        }
        domainStore.dispatch(AddCustomTokenAction.Init.SetAddedCurrencies(addedCurrencies))
        domainStore.dispatch(AddCustomTokenAction.Init.SetOnAddTokenCallback(onAddCustomToken))
        store.dispatch(NavigationAction.NavigateTo(AppScreen.AddCustomToken))
    }
}

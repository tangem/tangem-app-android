package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.common.flatMap
import com.tangem.common.services.Result
import com.tangem.core.analytics.Analytics
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.DomainWrapped
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.common.extensions.supportedBlockchains
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.common.util.supportsHdWallet
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.redux.domainStore
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.analytics.events.ManageTokens
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.domain.tokens.LoadAvailableCoinsService
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import com.tangem.utils.coroutines.AppCoroutineDispatcherProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

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

        val supportedBlockchains: List<Blockchain> = scanResponse?.card?.supportedBlockchains()
            ?: Blockchain.values().toList().filterNot(Blockchain::isTestnet)

        val loadCoinsService = LoadAvailableCoinsService(
            tangemTechApi = store.state.domainNetworks.tangemTechService.api,
            dispatchers = AppCoroutineDispatcherProvider(),
            assetReader = store.state.daggerGraphState.get(DaggerGraphState::assetReader),
        )

        scope.launch {
            val result = loadCoinsService.getSupportedTokens(
                isTestNet = scanResponse?.card?.isTestCard ?: false,
                supportedBlockchains = supportedBlockchains,
                page = if (newSearchInput == null) tokensState.pageToLoad else 0,
                searchInput = if (newSearchInput == null) tokensState.searchInput else newSearchInput.ifBlank { null },
            )

            store.dispatchOnMain(
                action = when (result) {
                    is Result.Success -> {
                        TokensAction.LoadCurrencies.Success(
                            currencies = result.data.currencies.filter(supportedBlockchains.toSet()),
                            loadMore = result.data.moreAvailable,
                        )
                    }
                    is Result.Failure -> {
                        TokensAction.LoadCurrencies.Failure
                    }
                },
            )
        }
    }

    private fun handleSaveChanges(action: TokensAction.SaveChanges) = scope.launch {
        val scanResponse = store.state.globalState.scanResponse ?: return@launch

        val currentTokens = store.state.tokensState.addedWallets.toNonCustomTokensWithBlockchains(
            derivationStyle = scanResponse.card.derivationStyle,
        )
        val currentBlockchains = store.state.tokensState.addedWallets.toNonCustomBlockchains(
            derivationStyle = scanResponse.card.derivationStyle,
        )

        val blockchainsToAdd = action.addedBlockchains.filterNot(currentBlockchains::contains)
        val blockchainsToRemove = currentBlockchains.filterNot(action.addedBlockchains::contains)

        val tokensToAdd = action.addedTokens.filterNot(currentTokens::contains)
        val tokensToRemove = currentTokens.filterNot { token -> action.addedTokens.any { it.token == token.token } }

        removeCurrenciesIfNeeded(
            currencies = convertToCurrencies(
                blockchains = blockchainsToRemove,
                tokens = tokensToRemove,
                derivationStyle = scanResponse.card.derivationStyle,
            ),
        )

        val isNothingToDoWithTokens = tokensToAdd.isEmpty() && tokensToRemove.isEmpty()
        val isNothingToDoWithBlockchain = blockchainsToAdd.isEmpty() && blockchainsToRemove.isEmpty()
        if (isNothingToDoWithTokens && isNothingToDoWithBlockchain) {
            store.dispatchDebugErrorNotification(message = "Nothing to save")
            store.dispatchOnMain(NavigationAction.PopBackTo())
            return@launch
        }

        val currencyList = convertToCurrencies(
            blockchains = blockchainsToAdd,
            tokens = tokensToAdd,
            derivationStyle = scanResponse.card.derivationStyle,
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

    private fun deriveMissingBlockchains(
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
            val result = tangemSdkManager.derivePublicKeys(
                cardId = null,
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

        return DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    class DerivationData(val derivations: Pair<ByteArrayKey, List<DerivationPath>>)

    private fun submitAdd(scanResponse: ScanResponse, currencyList: List<Currency>) {
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to add currencies, no user wallet selected")
            return
        }
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
    }

    private suspend fun removeCurrenciesIfNeeded(currencies: List<Currency>) {
        if (currencies.isEmpty()) return
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to remove currencies, no user wallet selected")
            return
        }
        walletCurrenciesManager.removeCurrencies(selectedUserWallet, currencies)
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

        val addedCurrencies = store.state.walletState.walletsStores
            .map { walletStore -> walletStore.walletsData.map(WalletDataModel::currency) }
            .flatten()
            .map { currency ->
                when (currency) {
                    is Currency.Blockchain -> DomainWrapped.Currency.Blockchain(
                        currency.blockchain,
                        currency.derivationPath,
                    )

                    is Currency.Token -> DomainWrapped.Currency.Token(
                        currency.token,
                        currency.blockchain,
                        currency.derivationPath,
                    )
                }
            }
        domainStore.dispatch(AddCustomTokenAction.Init.SetAddedCurrencies(addedCurrencies))
        domainStore.dispatch(AddCustomTokenAction.Init.SetOnAddTokenCallback(onAddCustomToken))
        store.dispatch(NavigationAction.NavigateTo(AppScreen.AddCustomToken))
    }
}
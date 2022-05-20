package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationParams
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.DomainWrapped
import com.tangem.domain.common.KeyWalletPublicKey
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.TapWorkarounds.isTestCard
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.features.addCustomToken.redux.AddCustomTokenAction
import com.tangem.domain.redux.domainStore
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.extensions.makeWalletManagerForApp
import com.tangem.tap.domain.tokens.BlockchainNetwork
import com.tangem.tap.features.wallet.redux.Currency
import com.tangem.tap.features.wallet.redux.WalletAction
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

class TokensMiddleware {

    val tokensMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is TokensAction.LoadCurrencies -> handleLoadCurrencies(action)
                    is TokensAction.SaveChanges -> handleSaveChanges(action)
                    is TokensAction.PrepareAndNavigateToAddCustomToken -> handleAddingCustomToken(action)
                }
                next(action)
            }
        }
    }

    private fun handleLoadCurrencies(action: TokensAction.LoadCurrencies) {
        val scanResponse = store.state.globalState.scanResponse
        val isTestcard = scanResponse?.card?.isTestCard ?: false

        scope.launch {
            val currencies = currenciesRepository.getSupportedTokens(isTestcard)
                .filter(action.supportedBlockchains?.toSet())
            delay(600)
            store.dispatchOnMain(TokensAction.LoadCurrencies.Success(currencies))
        }
    }

    private fun handleSaveChanges(action: TokensAction.SaveChanges) {
        val scanResponse = store.state.globalState.scanResponse ?: return

        val currentTokens = store.state.tokensState.addedWallets.toNonCustomTokensWithBlockchains(
            scanResponse.card.derivationStyle
        )
        val currentBlockchains = store.state.tokensState.addedWallets.toNonCustomBlockchains(
            scanResponse.card.derivationStyle
        )

        val blockchainsToAdd = action.addedBlockchains.filter { !currentBlockchains.contains(it) }
        val blockchainsToRemove = currentBlockchains.filter { !action.addedBlockchains.contains(it) }

        val tokensToAdd = action.addedTokens.filter { !currentTokens.contains(it) }
        val tokensToRemove = currentTokens.filter { token ->
            !action.addedTokens.any { it.token == token.token }
        }
        val derivationStyle = scanResponse.card.derivationStyle

        removeCurrenciesIfNeeded(convertToCurrencies(
            blockchains = blockchainsToRemove,
            tokens = tokensToRemove,
            derivationStyle = derivationStyle
        ))

        if (tokensToAdd.isEmpty() && blockchainsToAdd.isEmpty()) {
            store.dispatchDebugErrorNotification("Nothing to save")
            store.dispatch(NavigationAction.PopBackTo())
            return
        }

        val currencyList = convertToCurrencies(
            blockchains = blockchainsToAdd,
            tokens = tokensToAdd,
            derivationStyle = derivationStyle
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
        derivationStyle: DerivationStyle?
    ): List<Currency> {
        return blockchains.map {
            Currency.Blockchain(it, it.derivationPath(derivationStyle)?.rawPath)
        } + tokens.map {
            Currency.Token(
                it.token,
                it.blockchain,
                it.blockchain.derivationPath(derivationStyle)?.rawPath
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
            getDerivations(EllipticCurve.Ed25519, scanResponse, currencyList)
        )
        val derivations = derivationDataList.associate { it.derivations }
        if (derivations.isEmpty()) {
            onSuccess(scanResponse)
            return
        }

        scope.launch {
            val result = tangemSdkManager.derivePublicKeys(
                scanResponse.card.cardId,
                derivations
            )
            when (result) {
                is CompletionResult.Success -> {
                    val newDerivedKeys = result.data.entries
                    val updatedDerivedKeys =
                        mutableMapOf<KeyWalletPublicKey, ExtendedPublicKeysMap>()

                    newDerivedKeys.forEach { entry ->
                        val derivationData = derivationDataList.find {
                            it.mapKeyOfWalletPublicKey == entry.key
                        } ?: return@forEach
                        updatedDerivedKeys[entry.key] =
                            ExtendedPublicKeysMap(derivationData.alreadyDerivedKeys + entry.value)
                    }

                    val updatedScanResponse = scanResponse.copy(
                        derivedKeys = updatedDerivedKeys
                    )
                    store.dispatchOnMain(GlobalAction.SaveScanNoteResponse(updatedScanResponse))
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
            mapKeyOfWalletPublicKey = mapKeyOfWalletPublicKey
        )
    }

    private class DerivationData(
        val derivations: Pair<ByteArrayKey, List<DerivationPath>>,
        val alreadyDerivedKeys: ExtendedPublicKeysMap,
        val mapKeyOfWalletPublicKey: ByteArrayKey
    )

    private fun submitAdd(
        scanResponse: ScanResponse,
        currencyList: List<Currency>,
    ) {
        val factory = store.state.globalState.tapWalletManager.walletManagerFactory
        val derivationStyle = scanResponse.card.derivationStyle

        val addActions = currencyList.mapNotNull { currency ->
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
                        derivationParams = derivationParams
                    ) ?: return@mapNotNull null
                    val blockchainNetwork = BlockchainNetwork.fromWalletManager(walletManager)
                    WalletAction.MultiWallet.AddBlockchain(blockchainNetwork, walletManager)
                }
                is Currency.Token -> {
                    val rawDerivationPath = currency.derivationPath
                        ?: currency.blockchain.derivationPath(derivationStyle)?.rawPath

                    val blockchainNetwork = BlockchainNetwork(currency.blockchain, rawDerivationPath, emptyList())
                    WalletAction.MultiWallet.AddToken(currency.token, blockchainNetwork)
                }
            }
        }
        addActions.forEach { store.dispatchOnMain(it) }
    }

    private fun removeCurrenciesIfNeeded(currencies: List<Currency>) {
        if (currencies.isNotEmpty()) {
            currencies.forEach { currency ->
                store.state.walletState.getWalletData(currency)?.let {
                    store.dispatch(WalletAction.MultiWallet.RemoveWallet(it))
                }
            }
        }
    }

    private fun isNeedToDerive(scanResponse: ScanResponse, currency: Currency): Boolean {
        return currency.derivationPath?.let {
            !scanResponse.hasDerivation(currency.blockchain, it)
        } ?: false
    }

    private fun handleAddingCustomToken(action: TokensAction.PrepareAndNavigateToAddCustomToken) {
        val onAddCustomToken = fun(customCurrency: CustomCurrency) {
            val scanResponse = store.state.globalState.scanResponse ?: return

            fun submitAndPopBack(scanResponse: ScanResponse, currencyList: List<Currency>) {
                submitAdd(scanResponse, currencyList)
                // pop from the AddCustomTokenScreen
                store.dispatchOnMain(NavigationAction.PopBackTo())
                store.dispatchOnMain(NavigationAction.PopBackTo())
            }

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

        val addedCurrencies = store.state.walletState.wallets.map { walletStore ->
            walletStore.walletsData.map { walletData -> walletData.currency }
        }.flatten().map {
            when (it) {
                is Currency.Blockchain -> DomainWrapped.Currency.Blockchain(it.blockchain, it.derivationPath)
                is Currency.Token -> DomainWrapped.Currency.Token(it.token, it.blockchain, it.derivationPath)
            }
        }
        domainStore.dispatch(AddCustomTokenAction.Init.SetAddedCurrencies(addedCurrencies))
        domainStore.dispatch(AddCustomTokenAction.Init.SetOnAddTokenCallback(onAddCustomToken))
        store.dispatch(NavigationAction.NavigateTo(AppScreen.AddCustomToken))
    }
}
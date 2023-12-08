package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.derivation.DerivationStyle
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.common.flatMap
import com.tangem.core.navigation.NavigationAction
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.common.util.supportsHdWallet
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.TokenWithBlockchain
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.walletconnect.WalletConnectActions
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Middleware
import timber.log.Timber

@Suppress("LargeClass")
object TokensMiddleware {
// [REDACTED_TODO_COMMENT]
    private val addCryptoCurrenciesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        val currenciesRepository = store.state.daggerGraphState.get(DaggerGraphState::currenciesRepository)
        val networksRepository = store.state.daggerGraphState.get(DaggerGraphState::networksRepository)

        AddCryptoCurrenciesUseCase(currenciesRepository, networksRepository)
    }

    val tokensMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is TokensAction.LegacySaveChanges -> handleLegacySaveChanges(action)
                    is TokensAction.NewSaveChanges -> handleNewSaveChanges(action)
                }
                next(action)
            }
        }
    }

    private fun handleNewSaveChanges(action: TokensAction.NewSaveChanges) {
        scope.launch {
            val scanResponse = action.userWallet.scanResponse

            val currentTokens = action.currentTokens
            val currentBlockchains = action.currentCoins

            val blockchainsToAdd = action.changedCoins.filterNot(currentBlockchains::contains)
            val blockchainsToRemove = currentBlockchains.filterNot(action.changedCoins::contains)

            val tokensToAdd = action.changedTokens.filterNot(currentTokens::contains)
            val tokensToRemove = currentTokens.filterNot { token -> action.changedTokens.any { it == token } }

            removeNewCurrenciesIfNeeded(
                userWalletId = action.userWallet.walletId,
                currencies = blockchainsToRemove + tokensToRemove,
            )

            val isNothingToDoWithTokens = tokensToAdd.isEmpty() && tokensToRemove.isEmpty()
            val isNothingToDoWithBlockchain = blockchainsToAdd.isEmpty() && blockchainsToRemove.isEmpty()
            if (isNothingToDoWithTokens && isNothingToDoWithBlockchain) {
                store.dispatchDebugErrorNotification(message = "Nothing to save")
                store.dispatchOnMain(NavigationAction.PopBackTo())
                return@launch
            }

            val currencyList = blockchainsToAdd + tokensToAdd

            if (scanResponse.supportsHdWallet()) {
                deriveMissingCoins(scanResponse = scanResponse, currencyList = currencyList) {
                    submitNewAdd(
                        userWallet = action.userWallet,
                        updatedScanResponse = it,
                        currencyList = currencyList,
                    )
                }
            } else {
                submitNewAdd(
                    userWallet = action.userWallet,
                    updatedScanResponse = scanResponse,
                    currencyList = currencyList,
                )
            }
        }
    }

    private fun handleLegacySaveChanges(action: TokensAction.LegacySaveChanges) {
        scope.launch {
            val scanResponse = action.scanResponse

            val currentTokens = action.currentTokens
            val currentBlockchains = action.currentBlockchains

            val blockchainsToAdd = action.changedBlockchains.filterNot(currentBlockchains::contains)
            val blockchainsToRemove = currentBlockchains.filterNot(action.changedBlockchains::contains)

            val tokensToAdd = action.changedTokens.filterNot(currentTokens::contains)
            val tokensToRemove =
                currentTokens.filterNot { token -> action.changedTokens.any { it.token == token.token } }

            removeLegacyCurrenciesIfNeeded(
                currencies = convertToCurrencies(
                    blockchains = blockchainsToRemove,
                    tokens = tokensToRemove,
                    derivationStyle = scanResponse.derivationStyleProvider.getDerivationStyle(),
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
                derivationStyle = scanResponse.derivationStyleProvider.getDerivationStyle(),
            )

            if (scanResponse.supportsHdWallet()) {
                deriveMissingBlockchains(scanResponse, currencyList) {
                    submitLegacyAdd(it, currencyList)
                    store.dispatchOnMain(NavigationAction.PopBackTo())
                }
            } else {
                submitLegacyAdd(scanResponse, currencyList)
                store.dispatchOnMain(NavigationAction.PopBackTo())
            }
        }
    }

    private fun convertToCurrencies(
        blockchains: List<Blockchain>,
        tokens: List<TokenWithBlockchain>,
        derivationStyle: DerivationStyle?,
    ): List<Currency> {
        return blockchains.map { Currency.Blockchain(it, it.derivationPath(derivationStyle)?.rawPath) } +
            tokens.map {
                Currency.Token(
                    token = it.token,
                    blockchain = it.blockchain,
                    derivationPath = it.blockchain.derivationPath(derivationStyle)?.rawPath,
                )
            }
    }

    private fun deriveMissingBlockchains(
        scanResponse: ScanResponse,
        currencyList: List<Currency>,
        onSuccess: (ScanResponse) -> Unit,
    ) {
        val config = CardConfig.createConfig(scanResponse.card)
        val derivationDataList = currencyList.mapNotNull { currency ->
            val curve = config.primaryCurve(currency.blockchain)
            curve?.let { getLegacyDerivations(curve, scanResponse, currency) }
        }
        val derivations = buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            derivationDataList.forEach {
                val current = this[it.derivations.first]
                if (current != null) {
                    current.addAll(it.derivations.second)
                    current.distinct()
                } else {
                    this[it.derivations.first] = it.derivations.second.toMutableList()
                }
            }
        }

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

    private fun deriveMissingCoins(
        scanResponse: ScanResponse,
        currencyList: List<CryptoCurrency>,
        onSuccess: (ScanResponse) -> Unit,
    ) {
        val config = CardConfig.createConfig(scanResponse.card)
        val derivationDataList = currencyList.mapNotNull { currency ->
            val curve = config.primaryCurve(blockchain = Blockchain.fromId(currency.network.id.value))
            curve?.let { getNewDerivations(curve, scanResponse, currency) }
        }
        val derivations = buildMap<ByteArrayKey, MutableList<DerivationPath>> {
            derivationDataList.forEach {
                val current = this[it.derivations.first]
                if (current != null) {
                    current.addAll(it.derivations.second)
                    current.distinct()
                } else {
                    this[it.derivations.first] = it.derivations.second.toMutableList()
                }
            }
        }

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
                    val updatedScanResponse = scanResponse.copy(derivedKeys = updatedDerivedKeys)

                    store.dispatchOnMain(GlobalAction.SaveScanResponse(updatedScanResponse))

                    onSuccess(updatedScanResponse)
                }
                is CompletionResult.Failure -> {
                    store.dispatchDebugErrorNotification(TapError.CustomError("Error adding tokens"))
                }
            }
        }
    }

    private fun getLegacyDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currency: Currency,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val supportedCurves = currency.blockchain.getSupportedCurves()
        val path = currency.blockchain.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle())
            .takeIf { supportedCurves.contains(curve) }

        val customPath = currency.derivationPath?.let {
            DerivationPath(it)
        }.takeIf { supportedCurves.contains(curve) }

        val bothCandidates = listOfNotNull(path, customPath).distinct().toMutableList()
        if (bothCandidates.isEmpty()) return null

        if (currency is Currency.Blockchain && currency.blockchain == Blockchain.Cardano) {
            currency.derivationPath?.let {
                bothCandidates.add(CardanoUtils.extendedDerivationPath(DerivationPath(it)))
            }
        }

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys: ExtendedPublicKeysMap =
            scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = bothCandidates.filterNot { alreadyDerivedPaths.contains(it) }
        if (toDerive.isEmpty()) return null

        return DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    private fun getNewDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currency: CryptoCurrency,
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val blockchain = Blockchain.fromId(currency.network.id.value)
        val supportedCurves = blockchain.getSupportedCurves()
        val path = blockchain.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle())
            .takeIf { supportedCurves.contains(curve) }

        val customPath = currency.network.derivationPath.value?.let {
            DerivationPath(it)
        }.takeIf { supportedCurves.contains(curve) }

        val bothCandidates = listOfNotNull(path, customPath).distinct().toMutableList()
        if (bothCandidates.isEmpty()) return null

        if (currency is CryptoCurrency.Coin && blockchain == Blockchain.Cardano) {
            currency.network.derivationPath.value?.let {
                bothCandidates.add(CardanoUtils.extendedDerivationPath(DerivationPath(it)))
            }
        }

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys: ExtendedPublicKeysMap =
            scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = bothCandidates.filterNot { alreadyDerivedPaths.contains(it) }
        if (toDerive.isEmpty()) return null

        return DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    class DerivationData(val derivations: Pair<ByteArrayKey, List<DerivationPath>>)

    private fun submitLegacyAdd(scanResponse: ScanResponse, currencyList: List<Currency>) {
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

    private fun submitNewAdd(
        userWallet: UserWallet,
        updatedScanResponse: ScanResponse,
        currencyList: List<CryptoCurrency>,
    ) {
        scope.launch {
            userWalletsListManager.update(
                userWalletId = userWallet.walletId,
                update = { it.copy(scanResponse = updatedScanResponse) },
            ).doOnSuccess {
                addCryptoCurrenciesUseCase(userWallet.walletId, currencyList).onRight {
                    store.dispatch(action = WalletConnectActions.New.SetupUserChains(userWallet = userWallet))
                }
            }
        }
        store.dispatchOnMain(NavigationAction.PopBackTo())
    }

    private suspend fun removeLegacyCurrenciesIfNeeded(currencies: List<Currency>) {
        if (currencies.isEmpty()) return
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to remove currencies, no user wallet selected")
            return
        }
        walletCurrenciesManager.removeCurrencies(selectedUserWallet, currencies)
    }

    private suspend fun removeNewCurrenciesIfNeeded(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
        if (currencies.isEmpty()) return
        val currenciesRepository = store.state.daggerGraphState.get(DaggerGraphState::currenciesRepository)
        val walletManagersFacade = store.state.daggerGraphState.get(DaggerGraphState::walletManagersFacade)

        currenciesRepository.removeCurrencies(userWalletId = userWalletId, currencies = currencies)

        walletManagersFacade.remove(
            userWalletId = userWalletId,
            networks = currencies
                .filterIsInstance<CryptoCurrency.Coin>()
                .mapTo(hashSetOf(), CryptoCurrency::network),
        )
        walletManagersFacade.removeTokens(
            userWalletId = userWalletId,
            tokens = currencies.filterIsInstance<CryptoCurrency.Token>().toSet(),
        )
    }
}

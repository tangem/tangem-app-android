package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.doOnSuccess
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.core.navigation.NavigationAction
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.common.util.supportsHdWallet
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.TokensAction
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

@Suppress("LargeClass")
object TokensMiddleware {

    // TODO: Move to DI
    private val addCryptoCurrenciesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        val currenciesRepository = store.state.daggerGraphState.get(DaggerGraphState::currenciesRepository)
        val networksRepository = store.state.daggerGraphState.get(DaggerGraphState::networksRepository)

        AddCryptoCurrenciesUseCase(currenciesRepository, networksRepository)
    }

    val tokensMiddleware: Middleware<AppState> = { _, _ ->
        { next ->
            { action ->
                when (action) {
                    is TokensAction.SaveChanges -> handleSaveChanges(action)
                }
                next(action)
            }
        }
    }

    private fun handleSaveChanges(action: TokensAction.SaveChanges) {
        scope.launch {
            val scanResponse = action.userWallet.scanResponse

            val currentTokens = action.currentTokens
            val currentBlockchains = action.currentCoins

            val blockchainsToAdd = action.changedCoins.filterNot(currentBlockchains::contains)
            val blockchainsToRemove = currentBlockchains.filterNot(action.changedCoins::contains)

            val tokensToAdd = action.changedTokens.filterNot(currentTokens::contains)
            val tokensToRemove = currentTokens.filterNot { token -> action.changedTokens.any { it == token } }

            removeCurrenciesIfNeeded(
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
                    submitAdd(
                        userWallet = action.userWallet,
                        updatedScanResponse = it,
                        currencyList = currencyList,
                    )
                }
            } else {
                submitAdd(action.userWallet, scanResponse, currencyList)
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
            curve?.let { getDerivations(curve, scanResponse, currency) }
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

    private fun getDerivations(
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

    private fun submitAdd(
        userWallet: UserWallet,
        updatedScanResponse: ScanResponse,
        currencyList: List<CryptoCurrency>,
    ) {
        scope.launch {
            userWalletsListManager.update(
                userWalletId = userWallet.walletId,
                update = { it.copy(scanResponse = updatedScanResponse) },
            ).doOnSuccess {
                addCryptoCurrenciesUseCase(userWallet.walletId, currencyList)
            }
        }
        store.dispatchOnMain(NavigationAction.PopBackTo())
    }

    private suspend fun removeCurrenciesIfNeeded(userWalletId: UserWalletId, currencies: List<CryptoCurrency>) {
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
package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.AppScreen
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.TapWorkarounds.isTestCard
import com.tangem.tap.domain.tasks.product.KeyWalletPublicKey
import com.tangem.tap.domain.tasks.product.ScanResponse
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.scope
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.rekotlin.Middleware

class TokensMiddleware {

    val tokensMiddleware: Middleware<AppState> = { dispatch, state ->
        { next ->
            { action ->
                when (action) {
                    is TokensAction.LoadCurrencies -> handleLoadCurrencies()
                    is TokensAction.SaveChanges -> handleSaveChanges(action)
                }
                next(action)
            }
        }
    }

    private fun handleLoadCurrencies() {
        val scanResponse = store.state.globalState.scanResponse
        val isTestcard = scanResponse?.card?.isTestCard ?: false

        val currencies = currenciesRepository.getSupportedTokens(isTestcard)

        store.dispatch(TokensAction.LoadCurrencies.Success(currencies))
    }

    private fun handleSaveChanges(action: TokensAction.SaveChanges) {
        val scanResponse = store.state.globalState.scanResponse ?: return

        val currentTokens = store.state.tokensState.addedWallets.toTokens()
        val currentBlockchains = store.state.tokensState.addedWallets.toBlockchains()

        val blockchainsToAdd = action.addedBlockchains.filter { !currentBlockchains.contains(it) }
        val blockchainsToRemove = currentBlockchains.filter { !action.addedBlockchains.contains(it) }

        val tokensToAdd = action.addedTokens.filter { !currentTokens.contains(it) }
        val tokensToRemove = currentTokens.filter { !action.addedTokens.contains(it) }

        removeCurrenciesIfNeeded(blockchainsToRemove, tokensToRemove)

        if (tokensToAdd.isEmpty() && blockchainsToAdd.isEmpty()) {
            store.dispatch(NavigationAction.PopBackTo())
            return
        }

        if (scanResponse.supportsHdWallet()) {
            deriveMissingBlockchains(scanResponse, blockchainsToAdd, tokensToAdd)
        } else {
            submitAdd(blockchainsToAdd, tokensToAdd)
            store.dispatch(NavigationAction.PopBackTo())
        }
    }

    private fun deriveMissingBlockchains(
        scanResponse: ScanResponse,
        blockchains: List<Blockchain>,
        tokens: List<Token>
    ) {
        val derivationDataList = listOfNotNull(
            getDerivations(EllipticCurve.Secp256k1, scanResponse, blockchains, tokens),
            getDerivations(EllipticCurve.Ed25519, scanResponse, blockchains, tokens)
        )
        val derivations = derivationDataList.map { it.derivations }.toMap()

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
                    submitAdd(blockchains, tokens)

                    delay(DELAY_SDK_DIALOG_CLOSE)
                    store.dispatchOnMain(NavigationAction.PopBackTo())
                }
                is CompletionResult.Failure -> {
                    store.dispatchErrorNotification(TapError.CustomError("Error adding tokens"))
                }
            }
        }
    }

    private fun getDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        blockchains: List<Blockchain>,
        tokens: List<Token>
    ): DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val tokenBlockchains = tokens.map { it.blockchain }
        val derivationPathsCandidates = (blockchains + tokenBlockchains).distinct()
            .mapNotNull { it.derivationPath() }

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys: ExtendedPublicKeysMap =
            scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = derivationPathsCandidates.filterNot { alreadyDerivedPaths.contains(it) }
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

    private fun submitAdd(blockchains: List<Blockchain>, tokens: List<Token>) {
        (blockchains.map {
            WalletAction.MultiWallet.AddBlockchain(it)
        } + tokens.map {
            WalletAction.MultiWallet.AddToken(it)
        }).forEach { store.dispatchOnMain(it) }
    }

    private fun removeCurrenciesIfNeeded(blockchains: List<Blockchain>, tokens: List<Token>) {
        if (tokens.isNotEmpty()) {
            tokens.forEach { token ->
                store.state.walletState.getWalletData(token)?.let {
                    store.dispatch(WalletAction.MultiWallet.RemoveWallet(it))
                }
            }
        }
        if (blockchains.isNotEmpty()) {
            blockchains.forEach { blockchain ->
                store.state.walletState.getWalletData(blockchain)?.let {
                    store.dispatch(WalletAction.MultiWallet.RemoveWallet(it))
                }
            }
        }
    }

}
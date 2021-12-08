package com.tangem.tap.features.tokens.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.tap.common.extensions.dispatchErrorNotification
import com.tangem.tap.common.extensions.toMapKey
import com.tangem.tap.common.redux.AppState
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.common.redux.navigation.NavigationAction
import com.tangem.tap.currenciesRepository
import com.tangem.tap.domain.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.TapWorkarounds.isTestCard
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
                    is TokensAction.LoadCurrencies -> handleLoadCurrencies(action)
                    is TokensAction.SaveChanges -> handleSaveChanges(action)
                }
                next(action)
            }
        }
    }

    private fun handleLoadCurrencies(action: TokensAction.LoadCurrencies) {
        val scanResponse = store.state.globalState.scanResponse ?: return
        val isTestcard = scanResponse.card.isTestCard

        val tokens = currenciesRepository.getPopularTokens(isTestcard)
        val blockchains = currenciesRepository.getBlockchains(
            cardFirmware = scanResponse.card.firmwareVersion,
            isTestNet = isTestcard
        )
        val currencies = CurrencyListItem.createListOfCurrencies(blockchains, tokens).toMutableList()
        if (scanResponse.isTangemWallet()) {
            currencies.forEach {
                when (it) {
                    is CurrencyListItem.TitleListItem -> {
                    }
                    is CurrencyListItem.BlockchainListItem -> {
                        val firstCurve = it.blockchain.getSupportedCurves()?.firstOrNull()
                        if (firstCurve == EllipticCurve.Ed25519) {
                            it.isLock = true
                        }
                    }
                    is CurrencyListItem.TokenListItem -> {
                        val firstCurve = it.token.blockchain.getSupportedCurves()?.firstOrNull()
                        if (firstCurve == EllipticCurve.Ed25519) {
                            it.isLock = true
                        }
                    }
                }
            }
        }
        store.dispatch(TokensAction.LoadCurrencies.Success(currencies))
    }

    private fun handleSaveChanges(action: TokensAction.SaveChanges) {
        val scanResponse = store.state.globalState.scanResponse ?: return

        val candidatesToAdd = action.addedItems
        if (candidatesToAdd.isEmpty()) return

        val blockchains = candidatesToAdd.filterIsInstance<CurrencyListItem.BlockchainListItem>()
            .map { it.blockchain }
        val tokens = candidatesToAdd.filterIsInstance<CurrencyListItem.TokenListItem>()
            .map { it.token }
        if (blockchains.isEmpty() && tokens.isEmpty()) return

        if (scanResponse.isTangemWallet()) {
            deriveMissingBlockchains(scanResponse, blockchains, tokens)
        } else {
            submitAdd(blockchains, tokens)
            store.dispatch(NavigationAction.PopBackTo())
        }
    }

    private fun deriveMissingBlockchains(
        scanResponse: ScanResponse,
        blockchains: List<Blockchain>,
        tokens: List<Token>
    ) {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == EllipticCurve.Secp256k1 } ?: return

        val tokenBlockchains = tokens.map { it.blockchain }
        val derivationPathsCandidates = (blockchains + tokenBlockchains).distinct()
            .mapNotNull { it.derivationPath() }

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys = scanResponse.derivedKeys[mapKeyOfWalletPublicKey]?.toMutableList() ?: mutableListOf()
        val alreadyDerivedPaths = alreadyDerivedKeys.map { it.derivationPath }

        val toDerive = derivationPathsCandidates.filterNot { alreadyDerivedPaths.contains(it) }

        scope.launch {
            val result = tangemSdkManager.derivePublicKeys(scanResponse.card.cardId, wallet.publicKey, toDerive)
            when (result) {
                is CompletionResult.Success -> {
                    val newDerivedKeys = result.data
                    alreadyDerivedKeys.addAll(newDerivedKeys)
                    val updatedScanResponse = scanResponse.copy(
                        derivedKeys = mapOf(mapKeyOfWalletPublicKey to alreadyDerivedKeys.toList())
                    )
                    store.dispatch(GlobalAction.SaveScanNoteResponse(updatedScanResponse))
                    submitAdd(blockchains, tokens)

                    delay(DELAY_SDK_DIALOG_CLOSE)
                    store.dispatch(NavigationAction.PopBackTo())
                }
                is CompletionResult.Failure -> {
                    store.dispatchErrorNotification(TapError.CustomError("Error adding tokens"))
                }
            }
        }
    }

    private fun submitAdd(blockchains: List<Blockchain>, tokens: List<Token>) {
        (blockchains.map {
            WalletAction.MultiWallet.AddBlockchain(it)
        } + tokens.map {
            WalletAction.MultiWallet.AddToken(it)
        }).forEach { store.dispatch(it) }
    }
}
package com.tangem.tap.features.tokens.domain

import androidx.paging.PagingData
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.DerivationStyle
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.common.flatMap
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.model.WalletDataModel
import com.tangem.tap.features.tokens.domain.models.Token
import com.tangem.tap.features.tokens.redux.TokenWithBlockchain
import com.tangem.tap.features.tokens.redux.TokensMiddleware
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.AppStateHolder
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import com.tangem.tap.walletCurrenciesManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import timber.log.Timber

/**
 * Default implementation of tokens list interactor
 * FIXME("Necessary to avoid using redux actions")
 *
 * @property repository       repository of tokens list feature
 * @property reduxStateHolder redux state holder
 */
internal class DefaultTokensListInteractor(
    private val repository: TokensListRepository,
    private val reduxStateHolder: AppStateHolder,
) : TokensListInteractor {

    override fun getTokensList(searchText: String): Flow<PagingData<Token>> {
        return repository.getAvailableTokens(searchText = searchText.ifBlank(defaultValue = { null }))
    }

    override suspend fun saveChanges(tokens: List<TokenWithBlockchain>, blockchains: List<Blockchain>) {
        val scanResponse = requireNotNull(reduxStateHolder.scanResponse)

        val currentTokens = store.state.tokensState.addedWallets
            .toNonCustomTokensWithBlockchains(style = scanResponse.card.derivationStyle)

        val currentBlockchains = store.state.tokensState.addedWallets
            .toNonCustomBlockchains(derivationStyle = scanResponse.card.derivationStyle)

        val blockchainsToAdd = blockchains.filterNot(currentBlockchains::contains)
        val blockchainsToRemove = currentBlockchains.filterNot(blockchains::contains)

        val tokensToAdd = tokens.filterNot(currentTokens::contains)
        val tokensToRemove = currentTokens.filterNot { token -> tokens.any { it.token == token.token } }

        val isNothingToDoWithTokens = tokensToAdd.isEmpty() && tokensToRemove.isEmpty()
        val isNothingToDoWithBlockchain = blockchainsToAdd.isEmpty() && blockchainsToRemove.isEmpty()
        if (isNothingToDoWithTokens && isNothingToDoWithBlockchain) {
            store.dispatchDebugErrorNotification(message = "Nothing to save")
            return
        }

        remove(
            tokens = tokensToRemove,
            blockchains = blockchainsToRemove,
            derivationStyle = scanResponse.card.derivationStyle,
        )

        add(tokens = tokensToAdd, blockchains = blockchainsToAdd, scanResponse = scanResponse)
    }

    private fun List<WalletDataModel>.toNonCustomTokensWithBlockchains(
        style: DerivationStyle?,
    ): List<TokenWithBlockchain> {
        return this.map(WalletDataModel::currency)
            .mapNotNull { currency ->
                if (currency !is Currency.Token || currency.isCustomCurrency(style)) return@mapNotNull null
                TokenWithBlockchain(token = currency.token, blockchain = currency.blockchain)
            }
            .distinct()
    }

    private fun List<WalletDataModel>.toNonCustomBlockchains(derivationStyle: DerivationStyle?): List<Blockchain> {
        return this.map(WalletDataModel::currency)
            .mapNotNull { currency ->
                if (currency.isCustomCurrency(derivationStyle)) return@mapNotNull null
                (currency as? Currency.Blockchain)?.blockchain
            }
            .distinct()
    }

    private suspend fun remove(
        tokens: List<TokenWithBlockchain>,
        blockchains: List<Blockchain>,
        derivationStyle: DerivationStyle?,
    ) {
        val currencies = convertToCurrencies(tokens, blockchains, derivationStyle)
        if (currencies.isEmpty()) return

        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to remove currencies, no user wallet selected")
            return
        }

        walletCurrenciesManager.removeCurrencies(userWallet = selectedUserWallet, currenciesToRemove = currencies)
    }

    private suspend fun add(
        tokens: List<TokenWithBlockchain>,
        blockchains: List<Blockchain>,
        scanResponse: ScanResponse,
    ) {
        val currenciesToAdd = convertToCurrencies(
            tokens = tokens,
            blockchains = blockchains,
            derivationStyle = scanResponse.card.derivationStyle,
        )

        // TODO("[REDACTED_TASK_KEY] use DerivationManager")
        if (scanResponse.supportsHdWallet()) {
            deriveMissingBlockchains(scanResponse, currenciesToAdd)
        } else {
            submitAdd(scanResponse, currenciesToAdd)
            return
        }
    }

    private suspend fun deriveMissingBlockchains(scanResponse: ScanResponse, currencies: List<Currency>) {
        val derivations = listOfNotNull(
            getDerivations(EllipticCurve.Secp256k1, scanResponse, currencies),
            getDerivations(EllipticCurve.Ed25519, scanResponse, currencies),
        ).associate(transform = TokensMiddleware.DerivationData::derivations)

        if (derivations.isEmpty()) {
            submitAdd(scanResponse, currencies)
            return
        }

        when (val result = tangemSdkManager.derivePublicKeys(cardId = null, derivations = derivations)) {
            is CompletionResult.Success -> {
                val newDerivedKeys = result.data.entries
                val oldDerivedKeys = scanResponse.derivedKeys

                val walletKeys = (newDerivedKeys.keys + oldDerivedKeys.keys).toSet()

                val updatedDerivedKeys = walletKeys.associateWith { walletKey ->
                    val oldDerivations = ExtendedPublicKeysMap(map = oldDerivedKeys[walletKey] ?: emptyMap())
                    val newDerivations = newDerivedKeys[walletKey] ?: ExtendedPublicKeysMap(map = emptyMap())
                    ExtendedPublicKeysMap(map = oldDerivations + newDerivations)
                }

                val updatedScanResponse = scanResponse.copy(derivedKeys = updatedDerivedKeys)

                store.dispatchOnMain(GlobalAction.SaveScanResponse(updatedScanResponse))
                delay(DELAY_SDK_DIALOG_CLOSE)

                submitAdd(scanResponse, currencies)
                return
            }
            is CompletionResult.Failure -> {
                store.dispatchDebugErrorNotification(TapError.CustomError(customMessage = "Error adding tokens"))
            }
        }
    }

    private fun getDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currencyList: List<Currency>,
    ): TokensMiddleware.DerivationData? {
        val wallet = scanResponse.card.wallets.firstOrNull { it.curve == curve } ?: return null

        val manageTokensCandidates = currencyList
            .map(Currency::blockchain)
            .distinct()
            .filter { it.getSupportedCurves().contains(curve) }
            .mapNotNull { it.derivationPath(scanResponse.card.derivationStyle) }

        val customTokensCandidates = currencyList
            .filter { it.blockchain.getSupportedCurves().contains(curve) }
            .mapNotNull(Currency::derivationPath)
            .map(::DerivationPath)

        val bothCandidates = (manageTokensCandidates + customTokensCandidates).distinct()
        if (bothCandidates.isEmpty()) return null

        val mapKeyOfWalletPublicKey = wallet.publicKey.toMapKey()
        val alreadyDerivedKeys = scanResponse.derivedKeys[mapKeyOfWalletPublicKey] ?: ExtendedPublicKeysMap(emptyMap())
        val alreadyDerivedPaths = alreadyDerivedKeys.keys.toList()

        val toDerive = bothCandidates.filterNot(alreadyDerivedPaths::contains)
        if (toDerive.isEmpty()) return null

        return TokensMiddleware.DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    private suspend fun submitAdd(scanResponse: ScanResponse, currencies: List<Currency>) {
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to add currencies, no user wallet selected")
            return
        }

        userWalletsListManager
            .update(
                userWalletId = selectedUserWallet.walletId,
                update = { userWallet -> userWallet.copy(scanResponse = scanResponse) },
            )
            .flatMap { updatedUserWallet ->
                walletCurrenciesManager.addCurrencies(
                    userWallet = updatedUserWallet,
                    currenciesToAdd = currencies,
                )
            }
    }

    private fun convertToCurrencies(
        tokens: List<TokenWithBlockchain>,
        blockchains: List<Blockchain>,
        derivationStyle: DerivationStyle?,
    ): List<Currency> {
        return tokens.map { tokenWithBlockchain ->
            Currency.Token(
                token = tokenWithBlockchain.token,
                blockchain = tokenWithBlockchain.blockchain,
                derivationPath = tokenWithBlockchain.blockchain.derivationPath(derivationStyle)?.rawPath,
            )
        }.plus(
            blockchains.map { blockchain ->
                Currency.Blockchain(
                    blockchain = blockchain,
                    derivationPath = blockchain.derivationPath(derivationStyle)?.rawPath,
                )
            },
        )
    }
}
package com.tangem.tap.features.customtoken.impl.domain

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.guard
import com.tangem.common.extensions.toMapKey
import com.tangem.common.flatMap
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.tap.features.tokens.legacy.redux.TokensMiddleware
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.AppStateHolder
import kotlinx.coroutines.delay
import timber.log.Timber

/**
 * Default implementation of custom token interactor
 *
 * @property featureRepository feature repository
 * @property reduxStateHolder  redux state holder
 *
[REDACTED_AUTHOR]
 */
class DefaultCustomTokenInteractor(
    private val featureRepository: CustomTokenRepository,
    private val reduxStateHolder: AppStateHolder,
) : CustomTokenInteractor {

    override suspend fun findToken(address: String, blockchain: Blockchain): FoundToken {
        return featureRepository.findToken(
            address = address,
            networkId = if (blockchain != Blockchain.Unknown) blockchain.toNetworkId() else null,
        )
    }

    override suspend fun saveToken(customCurrency: CustomCurrency) {
        val scanResponse = reduxStateHolder.scanResponse ?: return

        val currency = Currency.fromCustomCurrency(customCurrency)
        val isNeedToDerive = isNeedToDerive(scanResponse, currency)
        if (isNeedToDerive) {
            deriveMissingBlockchains(scanResponse = scanResponse, currencyList = listOf(currency)) {
                submitAdd(scanResponse = it, currency = currency)
            }
        } else {
            submitAdd(scanResponse, currency)
        }
    }

    private fun isNeedToDerive(scanResponse: ScanResponse, currency: Currency): Boolean {
        return currency.derivationPath?.let { !scanResponse.hasDerivation(currency.blockchain, it) } ?: false
    }

    private suspend fun deriveMissingBlockchains(
        scanResponse: ScanResponse,
        currencyList: List<Currency>,
        onSuccess: suspend (ScanResponse) -> Unit,
    ) {
        val config = CardConfig.createConfig(scanResponse.card)
        val derivationDataList = currencyList.mapNotNull { currency ->
            val curve = config.primaryCurve(currency.blockchain)
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

        when (val result = tangemSdkManager.derivePublicKeys(cardId = null, derivations = derivations)) {
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
                delay(DELAY_SDK_DIALOG_CLOSE)

                onSuccess(updatedScanResponse)
            }
            is CompletionResult.Failure -> {
                store.dispatchDebugErrorNotification(TapError.CustomError("Error adding tokens"))
            }
        }
    }

    private fun getDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currency: Currency,
    ): TokensMiddleware.DerivationData? {
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

        return TokensMiddleware.DerivationData(derivations = mapKeyOfWalletPublicKey to toDerive)
    }

    private suspend fun submitAdd(scanResponse: ScanResponse, currency: Currency) {
        val selectedUserWallet = userWalletsListManager.selectedUserWalletSync.guard {
            Timber.e("Unable to add currencies, no user wallet selected")
            return
        }

        userWalletsListManager.update(
            userWalletId = selectedUserWallet.walletId,
            update = { userWallet -> userWallet.copy(scanResponse = scanResponse) },
        )
            .flatMap { updatedUserWallet ->
                walletCurrenciesManager.addCurrencies(
                    userWallet = updatedUserWallet,
                    currenciesToAdd = listOf(currency),
                )
            }
    }
}
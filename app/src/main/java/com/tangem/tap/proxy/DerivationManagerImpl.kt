package com.tangem.tap.proxy

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.common.hdWallet.DerivationPath
import com.tangem.domain.common.ScanResponse
import com.tangem.domain.common.TapWorkarounds.derivationStyle
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.Currency.NonNativeToken
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.tokens.models.BlockchainNetwork
import com.tangem.tap.scope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine

class DerivationManagerImpl(
    private val appStateHolder: AppStateHolder,
) : DerivationManager {

    override suspend fun deriveMissingBlockchains(currency: Currency) = suspendCoroutine<Boolean> { continuation ->
        val blockchain = Blockchain.fromNetworkId(currency.networkId)
        val card = appStateHolder.getActualCard()
        if (blockchain != null && card != null) {
            val appToken = if (currency is NonNativeToken) {
                Token(
                    symbol = currency.symbol,
                    contractAddress = currency.contractAddress,
                    decimals = currency.decimalCount,
                )
            } else null
            val blockchainNetwork = BlockchainNetwork(blockchain, card)
            val appCurrency = com.tangem.tap.features.wallet.models.Currency.fromBlockchainNetwork(
                blockchainNetwork,
                appToken,
            )
            val scanResponse = appStateHolder.scanResponse
            if (scanResponse != null) {
                deriveMissingBlockchains(
                    scanResponse = scanResponse,
                    listOf(appCurrency),
                ) {
                    continuation.resumeWith(Result.success(true))
                }
            }
        } else {
            continuation.resumeWith(Result.failure(IllegalStateException("no blockchain or card found")))
        }
    }

    override fun hasDerivation(networkId: String): Boolean {
        val scanResponse = appStateHolder.scanResponse
        val blockchain = Blockchain.fromNetworkId(networkId)
        if (scanResponse != null && blockchain != null) {
            val derivationPath = blockchain.derivationPath(appStateHolder.getActualCard()?.derivationStyle)?.rawPath
            if (derivationPath.isNullOrEmpty()) return false
            return scanResponse.hasDerivation(
                blockchain,
                derivationPath,
            )
        }
        return false
    }

    private fun deriveMissingBlockchains(
        scanResponse: ScanResponse,
        currencyList: List<com.tangem.tap.features.wallet.models.Currency>,
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
            val selectedWallet = appStateHolder.userWalletsListManager?.selectedUserWalletSync

            val result = appStateHolder.tangemSdkManager?.derivePublicKeys(
                scanResponse.card.cardId,
                derivations,
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
                    if (selectedWallet != null) {
                        val userWallet = selectedWallet.copy(
                            scanResponse = updatedScanResponse,
                        )

                        appStateHolder.userWalletsListManager?.save(userWallet, canOverride = true)
                        appStateHolder.walletStoresManager?.fetch(userWallet, true)
                    }
                    appStateHolder.mainStore?.dispatchOnMain(GlobalAction.SaveScanResponse(updatedScanResponse))
                    delay(DELAY_SDK_DIALOG_CLOSE)
                    onSuccess(updatedScanResponse)
                }
                is CompletionResult.Failure -> {
                    appStateHolder.mainStore?.dispatchDebugErrorNotification(
                        TapError.CustomError(
                            "Error derivation",
                        ),
                    )
                }
                else -> {
                    error("result result is null")
                }
            }
        }
    }

    private fun getDerivations(
        curve: EllipticCurve,
        scanResponse: ScanResponse,
        currencyList: List<com.tangem.tap.features.wallet.models.Currency>,
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
}

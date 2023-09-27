package com.tangem.tap.proxy

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.BlockchainNetwork
import com.tangem.domain.common.DerivationStyleProvider
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.extensions.fromNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.lib.crypto.DerivationManager
import com.tangem.lib.crypto.models.Currency
import com.tangem.lib.crypto.models.Currency.NonNativeToken
import com.tangem.lib.crypto.models.errors.UserCancelledException
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.DELAY_SDK_DIALOG_CLOSE
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.tokens.legacy.redux.TokensMiddleware
import com.tangem.tap.scope
import com.tangem.tap.userWalletsListManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.suspendCoroutine
import com.tangem.tap.features.wallet.models.Currency as WalletModelCurrency

class DerivationManagerImpl(
    private val appStateHolder: AppStateHolder,
    private val currenciesRepository: CurrenciesRepository,
) : DerivationManager {

    override suspend fun deriveMissingBlockchains(currency: Currency) = suspendCoroutine { continuation ->
        val blockchain = Blockchain.fromNetworkId(currency.networkId)
        val card = appStateHolder.getActualCard()
        if (blockchain != null && card != null) {
            val appToken = getAppToken(currency)
            val scanResponse = appStateHolder.scanResponse
            if (scanResponse != null) {
                val blockchainNetwork = BlockchainNetwork(blockchain, scanResponse.derivationStyleProvider)
                val appCurrency = WalletModelCurrency.fromBlockchainNetwork(
                    blockchainNetwork,
                    appToken,
                )
                deriveMissingBlockchains(
                    scanResponse = scanResponse,
                    currencyList = listOf(appCurrency),
                    onSuccess = { continuation.resumeWith(Result.success(true)) },
                ) {
                    continuation.resumeWith(Result.failure(it))
                }
            }
        } else {
            continuation.resumeWith(Result.failure(IllegalStateException("no blockchain or card found")))
        }
    }

    override suspend fun deriveAndAddTokens(currency: Currency) = suspendCoroutine { continuation ->
        val selectedUserWallet = requireNotNull(
            userWalletsListManager.selectedUserWalletSync,
        ) { "selectedUserWallet shouldn't be null" }
        val scanResponse = selectedUserWallet.scanResponse
        val blockchain = requireNotNull(
            Blockchain.fromNetworkId(currency.networkId),
        ) { "unsupported blockchain" }
        val derivationStyleProvider = scanResponse.derivationStyleProvider
        val derivationPath = requireNotNull(
            blockchain.derivationPath(derivationStyleProvider.getDerivationStyle())?.rawPath,
        ) { "derivationPath shouldn't be null" }
        val hasDerivation = scanResponse.hasDerivation(
            blockchain,
            derivationPath,
        )
        if (hasDerivation) {
            scope.launch {
                addToken(
                    userWalletId = selectedUserWallet.walletId,
                    blockchain = blockchain,
                    currency = currency,
                    derivationPath = derivationPath,
                    derivationStyleProvider = derivationStyleProvider,
                )
            }
        } else {
            val blockchainNetwork = BlockchainNetwork(blockchain, scanResponse.derivationStyleProvider)
            val appCurrency = WalletModelCurrency.fromBlockchainNetwork(
                blockchainNetwork,
                getAppToken(currency),
            )
            deriveMissingBlockchains(
                scanResponse = scanResponse,
                currencyList = listOf(appCurrency),
                onSuccess = { updatedScanResponse ->
                    scope.launch {
                        userWalletsListManager.update(
                            userWalletId = selectedUserWallet.walletId,
                            update = { it.copy(scanResponse = updatedScanResponse) },
                        )
                        addToken(
                            userWalletId = selectedUserWallet.walletId,
                            blockchain = blockchain,
                            currency = currency,
                            derivationPath = derivationPath,
                            derivationStyleProvider = derivationStyleProvider,
                        )
                        continuation.resumeWith(Result.success(derivationPath))
                    }
                },
                onFailure = {
                    continuation.resumeWith(Result.failure(it))
                },
            )
        }
    }

    override fun getDerivationPathForBlockchain(networkId: String): String? {
        val scanResponse = appStateHolder.scanResponse
        val blockchain = Blockchain.fromNetworkId(networkId)
        if (scanResponse != null && blockchain != null) {
            return blockchain.derivationPath(scanResponse.derivationStyleProvider.getDerivationStyle())?.rawPath
        }
        return null
    }

    override fun hasDerivation(networkId: String, derivationPath: String): Boolean {
        val scanResponse = appStateHolder.scanResponse
        val blockchain = Blockchain.fromNetworkId(networkId)
        if (scanResponse != null && blockchain != null) {
            return scanResponse.hasDerivation(
                blockchain,
                derivationPath,
            )
        }
        return false
    }

    private suspend fun addToken(
        userWalletId: UserWalletId,
        blockchain: Blockchain,
        currency: Currency,
        derivationPath: String,
        derivationStyleProvider: DerivationStyleProvider,
    ) {
        currenciesRepository.addCurrencies(
            userWalletId,
            listOf(
                convertCurrency(
                    blockchain = blockchain,
                    currency = currency,
                    derivationPath = derivationPath,
                    derivationStyleProvider = derivationStyleProvider,
                ),
            ),
        )
    }

    private fun convertCurrency(
        blockchain: Blockchain,
        currency: Currency,
        derivationPath: String,
        derivationStyleProvider: DerivationStyleProvider,
    ): CryptoCurrency {
        val cryptoCurrencyFactory = CryptoCurrencyFactory()
        return when (currency) {
            is Currency.NativeToken -> {
                cryptoCurrencyFactory.createCoin(
                    blockchain = blockchain,
                    extraDerivationPath = derivationPath,
                    derivationStyleProvider = derivationStyleProvider,
                )
            }
            is NonNativeToken -> {
                val sdkToken = Token(
                    symbol = currency.symbol,
                    contractAddress = currency.contractAddress,
                    decimals = currency.decimalCount,
                )
                cryptoCurrencyFactory.createToken(
                    sdkToken = sdkToken,
                    blockchain = blockchain,
                    extraDerivationPath = derivationPath,
                    derivationStyleProvider = derivationStyleProvider,
                )
            }
        } as CryptoCurrency
    }

    private fun deriveMissingBlockchains(
        scanResponse: ScanResponse,
        currencyList: List<WalletModelCurrency>,
        onSuccess: (ScanResponse) -> Unit,
        onFailure: (Exception) -> Unit,
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

        scope.launch {
            val selectedUserWallet = appStateHolder.userWalletsListManager?.selectedUserWalletSync

            val result = appStateHolder.tangemSdkManager?.derivePublicKeys(
                cardId = null, // always ignore cardId in derive task
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
                    if (selectedUserWallet != null) {
                        val userWallet = selectedUserWallet.copy(
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
                    onFailure.invoke(handleTangemError(result.error))
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
        currency: com.tangem.tap.features.wallet.models.Currency,
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

        if (currency is WalletModelCurrency.Blockchain &&
            currency.blockchain == Blockchain.Cardano
        ) {
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

    private fun getAppToken(currency: Currency): Token? {
        return if (currency is NonNativeToken) {
            Token(
                symbol = currency.symbol,
                contractAddress = currency.contractAddress,
                decimals = currency.decimalCount,
            )
        } else {
            null
        }
    }
    /**
     * Simple error handler
     * for now specifically handle only UserCancelled
     *
     * @param error [TangemError]
     */
    private fun handleTangemError(error: TangemError): Exception {
        if (error is TangemSdkError.UserCancelled) {
            return UserCancelledException()
        }
        return IllegalStateException(error.customMessage)
    }

    private class DerivationData(
        val derivations: Pair<ByteArrayKey, List<DerivationPath>>,
    )
}

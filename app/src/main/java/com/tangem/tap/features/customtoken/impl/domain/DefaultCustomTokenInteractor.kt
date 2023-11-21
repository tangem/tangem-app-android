package com.tangem.tap.features.customtoken.impl.domain

import com.tangem.blockchain.blockchains.cardano.CardanoUtils
import com.tangem.blockchain.common.Blockchain
import com.tangem.common.CompletionResult
import com.tangem.common.card.EllipticCurve
import com.tangem.common.core.TangemError
import com.tangem.common.extensions.ByteArrayKey
import com.tangem.common.extensions.toMapKey
import com.tangem.crypto.hdWallet.DerivationPath
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.tap.features.tokens.legacy.redux.TokensMiddleware
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
import com.tangem.tap.tangemSdkManager
import com.tangem.tap.userWalletsListManager
import com.tangem.utils.extensions.DELAY_SDK_DIALOG_CLOSE
import kotlinx.coroutines.delay

/**
 * Default implementation of custom token interactor
 *
 * @property featureRepository feature repository
 *
[REDACTED_AUTHOR]
 */
class DefaultCustomTokenInteractor(
    private val featureRepository: CustomTokenRepository,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
) : CustomTokenInteractor {

    // TODO: Move to DI
    private val addCryptoCurrenciesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        val currenciesRepository = store.state.daggerGraphState.get(DaggerGraphState::currenciesRepository)
        val networksRepository = store.state.daggerGraphState.get(DaggerGraphState::networksRepository)

        AddCryptoCurrenciesUseCase(currenciesRepository, networksRepository)
    }

    override suspend fun findToken(address: String, blockchain: Blockchain): FoundToken {
        return featureRepository.findToken(
            address = address,
            networkId = if (blockchain != Blockchain.Unknown) blockchain.toNetworkId() else null,
        )
    }

    override suspend fun saveToken(customCurrency: CustomCurrency) {
        val userWallet = getSelectedWalletSyncUseCase().fold(ifLeft = { return }, ifRight = { it })

        val currency = Currency.fromCustomCurrency(customCurrency)
        val isNeedToDerive = isNeedToDerive(userWallet, currency)
        if (isNeedToDerive) {
            deriveMissingBlockchains(
                userWallet = userWallet,
                currencyList = listOf(currency),
                onSuccess = { submitAdd(userWallet = userWallet.copy(scanResponse = it), currency = currency) },
            ) {
                throw it
            }
        } else {
            submitAdd(userWallet, currency)
        }
    }

    private fun isNeedToDerive(userWallet: UserWallet, currency: Currency): Boolean {
        val scanResponse = userWallet.scanResponse
        return currency.derivationPath?.let { !scanResponse.hasDerivation(currency.blockchain, it) } ?: false
    }

    private suspend fun deriveMissingBlockchains(
        userWallet: UserWallet,
        currencyList: List<Currency>,
        onSuccess: suspend (ScanResponse) -> Unit,
        onFailure: suspend (TangemError) -> Unit,
    ) {
        val scanResponse = userWallet.scanResponse
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
                onFailure.invoke(result.error)
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

    private suspend fun submitAdd(userWallet: UserWallet, currency: Currency) {
        val cryptoCurrencyFactory = CryptoCurrencyFactory()

        val scanResponse = userWallet.scanResponse
        val userWalletId = userWallet.walletId

        val currencyList = listOfNotNull(
            when (currency) {
                is Currency.Blockchain -> {
                    cryptoCurrencyFactory.createCoin(
                        blockchain = currency.blockchain,
                        extraDerivationPath = currency.derivationPath,
                        derivationStyleProvider = scanResponse.derivationStyleProvider,
                    )
                }
                is Currency.Token -> {
                    cryptoCurrencyFactory.createToken(
                        sdkToken = currency.token,
                        blockchain = currency.blockchain,
                        extraDerivationPath = currency.derivationPath,
                        derivationStyleProvider = scanResponse.derivationStyleProvider,
                    )
                }
            },
        )

        userWalletsListManager.update(userWalletId) {
            it.copy(scanResponse = scanResponse)
        }

        addCryptoCurrenciesUseCase(userWalletId, currencyList)
    }
}
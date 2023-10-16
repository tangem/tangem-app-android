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
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.common.configs.CardConfig
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.common.util.hasDerivation
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.operations.derivation.ExtendedPublicKeysMap
import com.tangem.tap.*
import com.tangem.tap.common.extensions.dispatchDebugErrorNotification
import com.tangem.tap.common.extensions.dispatchOnMain
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.TapError
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.tap.features.tokens.legacy.redux.TokensMiddleware
import com.tangem.tap.features.wallet.models.Currency
import com.tangem.tap.proxy.redux.DaggerGraphState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

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
            deriveMissingBlockchains(userWallet = userWallet, currencyList = listOf(currency)) {
                submitAdd(userWallet = userWallet, currency = currency)
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
        val scanResponse = userWallet.scanResponse
        val walletFeatureToggles = store.state.daggerGraphState.get(DaggerGraphState::walletFeatureToggles)

        if (walletFeatureToggles.isRedesignedScreenEnabled) {
            val cryptoCurrencyFactory = CryptoCurrencyFactory()

            submitNewAdd(
                userWalletId = userWallet.walletId,
                updatedScanResponse = scanResponse,
                currencyList = listOfNotNull(
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
                ),
            )
        } else {
            submitLegacyAdd(scanResponse = scanResponse, currency = currency)
        }
    }

    private suspend fun submitLegacyAdd(scanResponse: ScanResponse, currency: Currency) {
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

    private fun submitNewAdd(
        userWalletId: UserWalletId,
        updatedScanResponse: ScanResponse,
        currencyList: List<CryptoCurrency>,
    ) {
        val currenciesRepository = store.state.daggerGraphState.get(DaggerGraphState::currenciesRepository)
        val networksRepository = store.state.daggerGraphState.get(DaggerGraphState::networksRepository)
        scope.launch {
            userWalletsListManager.update(
                userWalletId = userWalletId,
                update = { it.copy(scanResponse = updatedScanResponse) },
            )

            currenciesRepository.addCurrencies(userWalletId = userWalletId, currencies = currencyList)
            val networks = currencyList.map { it.network }.toSet()
            networksRepository.getNetworkStatusesSync(
                userWalletId = userWalletId,
                networks = networks,
                refresh = true,
            )
        }
    }
}
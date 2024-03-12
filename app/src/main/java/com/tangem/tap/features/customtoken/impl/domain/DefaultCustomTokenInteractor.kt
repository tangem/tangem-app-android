package com.tangem.tap.features.customtoken.impl.domain

import com.tangem.blockchain.common.Blockchain
import com.tangem.data.tokens.utils.CryptoCurrencyFactory
import com.tangem.domain.card.DerivePublicKeysUseCase
import com.tangem.domain.common.extensions.toNetworkId
import com.tangem.domain.common.util.derivationStyleProvider
import com.tangem.domain.features.addCustomToken.CustomCurrency
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.tokens.AddCryptoCurrenciesUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.tap.common.extensions.inject
import com.tangem.tap.domain.model.Currency
import com.tangem.tap.features.customtoken.impl.domain.models.FoundToken
import com.tangem.tap.proxy.redux.DaggerGraphState
import com.tangem.tap.store
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
    private val derivePublicKeysUseCase: DerivePublicKeysUseCase,
) : CustomTokenInteractor {

    // TODO: Move to DI
    private val addCryptoCurrenciesUseCase by lazy(LazyThreadSafetyMode.NONE) {
        val currenciesRepository = store.inject(DaggerGraphState::currenciesRepository)
        val networksRepository = store.inject(DaggerGraphState::networksRepository)

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

        val currencies = listOfNotNull(element = currency.toCryptoCurrency(userWallet.scanResponse))
        derivePublicKeysUseCase(userWalletId = userWallet.walletId, currencies = currencies)
            .onRight {
                addCryptoCurrenciesUseCase(userWalletId = userWallet.walletId, currencies = currencies)
            }
            .onLeft { Timber.e("Failed to derive public keys: $it") }
    }

    private fun Currency.toCryptoCurrency(scanResponse: ScanResponse): CryptoCurrency? {
        val cryptoCurrencyFactory = CryptoCurrencyFactory()

        return when (this) {
            is Currency.Blockchain -> {
                cryptoCurrencyFactory.createCoin(
                    blockchain = blockchain,
                    extraDerivationPath = derivationPath,
                    derivationStyleProvider = scanResponse.derivationStyleProvider,
                )
            }
            is Currency.Token -> {
                cryptoCurrencyFactory.createToken(
                    sdkToken = token,
                    blockchain = blockchain,
                    extraDerivationPath = derivationPath,
                    derivationStyleProvider = scanResponse.derivationStyleProvider,
                )
            }
        }
    }
}
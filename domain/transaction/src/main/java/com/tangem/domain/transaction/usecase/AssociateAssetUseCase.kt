package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.error.AssociateAssetError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.firstOrNull

class AssociateAssetUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<AssociateAssetError, Unit> {
        return either {
            val networkCoin = if (tokensFeatureToggles.isWalletBalanceFetcherEnabled) {
                multiWalletCryptoCurrenciesSupplier.getSyncOrNull(
                    params = MultiWalletCryptoCurrenciesProducer.Params(userWalletId),
                )
                    ?.firstOrNull {
                        val network = currency.network
                        it.network.id == network.id && it.network.derivationPath == network.derivationPath
                    }
                    ?: error("Unable to create network coin for currencyID: ${currency.id}")
            } else {
                currenciesRepository.getNetworkCoin(
                    userWalletId = userWalletId,
                    networkId = currency.network.id,
                    derivationPath = currency.network.derivationPath,
                )
            }
            if (isBalanceZero(userWalletId, networkCoin)) {
                raise(AssociateAssetError.NotEnoughBalance(networkCoin))
            }
            val signer = cardSdkConfigRepository.getCommonSigner(
                cardId = null,
                twinKey = null, // use null here because no assets support for Twin cards
            )

            catch(
                block = {
                    when (val result = walletManagersFacade.fulfillRequirements(userWalletId, currency, signer)) {
                        is SimpleResult.Failure -> raise(AssociateAssetError.DataError(result.error.customMessage))
                        SimpleResult.Success -> Unit
                    }
                },
                catch = { error -> AssociateAssetError.DataError(error.message) },
            )
        }
    }

    private suspend fun isBalanceZero(userWalletId: UserWalletId, currency: CryptoCurrency): Boolean {
        val networkStatus = singleNetworkStatusSupplier(
            params = SingleNetworkStatusProducer.Params(
                userWalletId = userWalletId,
                network = currency.network,
            ),
        )
            .firstOrNull()

        val networkCoinAmountStatus = (networkStatus?.value as? NetworkStatus.Verified)
            ?.amounts
            ?.get(currency.id)

        return networkCoinAmountStatus is NetworkStatus.Amount.Loaded &&
            networkCoinAmountStatus.value.isNullOrZero()
    }
}
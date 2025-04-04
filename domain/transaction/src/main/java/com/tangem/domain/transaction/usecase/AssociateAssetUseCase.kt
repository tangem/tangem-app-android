package com.tangem.domain.transaction.usecase

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import com.tangem.blockchain.extensions.SimpleResult
import com.tangem.domain.card.repository.CardSdkConfigRepository
import com.tangem.domain.networks.single.SingleNetworkStatusProducer
import com.tangem.domain.networks.single.SingleNetworkStatusSupplier
import com.tangem.domain.tokens.TokensFeatureToggles
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyAmountStatus
import com.tangem.domain.tokens.model.NetworkStatus
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.tokens.repository.NetworksRepository
import com.tangem.domain.transaction.error.AssociateAssetError
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.utils.isNullOrZero
import kotlinx.coroutines.flow.firstOrNull

class AssociateAssetUseCase(
    private val cardSdkConfigRepository: CardSdkConfigRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val currenciesRepository: CurrenciesRepository,
    private val networksRepository: NetworksRepository,
    private val singleNetworkStatusSupplier: SingleNetworkStatusSupplier,
    private val tokensFeatureToggles: TokensFeatureToggles,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Either<AssociateAssetError, Unit> {
        return either {
            val networkCoin = currenciesRepository.getNetworkCoin(
                userWalletId = userWalletId,
                networkId = currency.network.id,
                derivationPath = currency.network.derivationPath,
            )
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
        val networkStatus = if (tokensFeatureToggles.isNetworksLoadingRefactoringEnabled) {
            singleNetworkStatusSupplier(
                params = SingleNetworkStatusProducer.Params(
                    userWalletId = userWalletId,
                    network = currency.network,
                ),
            )
                .firstOrNull()
        } else {
            networksRepository.getNetworkStatusesSync(
                userWalletId = userWalletId,
                networks = setOf(currency.network),
            ).find { it.network == currency.network }
        }

        val networkCoinAmountStatus = (networkStatus?.value as? NetworkStatus.Verified)
            ?.amounts
            ?.get(currency.id)

        return networkCoinAmountStatus is CryptoCurrencyAmountStatus.Loaded &&
            networkCoinAmountStatus.value.isNullOrZero()
    }
}
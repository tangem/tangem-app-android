package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.DerivationIndex
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

/**
 * Default implementation of [MultiWalletCryptoCurrenciesProducer]
 *
 * @property params                          params
 * @property flowProducerTools               tools for producing flows
 * @property userWalletsListRepository       repository for getting user wallets
 * @property userTokensResponseStore         store of `UserTokensResponse`
 * @property responseCryptoCurrenciesFactory factory for creating [CryptoCurrency] from `UserTokensResponse`
 * @property dispatchers                     dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiWalletCryptoCurrenciesProducer @AssistedInject constructor(
    @Assisted val params: MultiWalletCryptoCurrenciesProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletCryptoCurrenciesProducer {

    override val fallback: Option<Set<CryptoCurrency>> = emptySet<CryptoCurrency>().some()

    override fun produce(): Flow<Set<CryptoCurrency>> {
        val userWallet = userWalletsListRepository.getSyncStrict(id = params.userWalletId)

        if (!userWallet.isMultiCurrency) {
            error("${this::class.simpleName ?: this::class.toString()} supports only multi-currency wallet")
        }

        return userTokensResponseStore.get(userWalletId = params.userWalletId)
            .distinctUntilChanged()
            .map { response ->
                if (response == null) return@map emptySet()

                responseCryptoCurrenciesFactory.createCurrencies(
                    response = response,
                    userWallet = userWallet,
                    accountIndex = DerivationIndex.Main,
                ).toSet()
            }
            .onEmpty { emit(emptySet()) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiWalletCryptoCurrenciesProducer.Factory {
        override fun create(
            params: MultiWalletCryptoCurrenciesProducer.Params,
        ): DefaultMultiWalletCryptoCurrenciesProducer
    }
}
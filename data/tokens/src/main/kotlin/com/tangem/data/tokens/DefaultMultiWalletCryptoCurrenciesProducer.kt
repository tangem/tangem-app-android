package com.tangem.data.tokens

import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.local.token.UserTokensResponseStore
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.domain.wallets.models.requireColdWallet
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.*

/**
 * Default implementation of [MultiWalletCryptoCurrenciesProducer]
 *
 * @property params                          params
 * @property userWalletsStore                UserWallet's store
 * @property userTokensResponseStore         store of `UserTokensResponse`
 * @property responseCryptoCurrenciesFactory factory for creating [CryptoCurrency] from `UserTokensResponse`
 * @property dispatchers                     dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class DefaultMultiWalletCryptoCurrenciesProducer @AssistedInject constructor(
    @Assisted val params: MultiWalletCryptoCurrenciesProducer.Params,
    private val userWalletsStore: UserWalletsStore,
    private val userTokensResponseStore: UserTokensResponseStore,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletCryptoCurrenciesProducer {

    override val fallback: Set<CryptoCurrency>
        get() = emptySet()

    override fun produce(): Flow<Set<CryptoCurrency>> {
        val userWallet = userWalletsStore.getSyncStrict(key = params.userWalletId).requireColdWallet() // TODO [REDACTED_TASK_KEY]

        if (!userWallet.isMultiCurrency) {
            error("${this::class.simpleName} supports only multi-currency wallet")
        }

        return userTokensResponseStore.get(userWalletId = params.userWalletId)
            .distinctUntilChanged()
            .map { response ->
                if (response == null) return@map emptySet()

                responseCryptoCurrenciesFactory.createCurrencies(
                    response = response,
                    scanResponse = userWallet.scanResponse,
                ).toSet()
            }
            .onEmpty { emit(fallback) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiWalletCryptoCurrenciesProducer.Factory {
        override fun create(
            params: MultiWalletCryptoCurrenciesProducer.Params,
        ): DefaultMultiWalletCryptoCurrenciesProducer
    }
}
package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.data.account.store.AccountsResponseStoreFactory
import com.tangem.data.common.currency.ResponseCryptoCurrenciesFactory
import com.tangem.datasource.local.userwallet.UserWalletsStore
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
 * Implementation of [MultiWalletCryptoCurrenciesProducer] that produces crypto currencies of all accounts
 *
 * @property params                          params
 * @property userWalletsStore                UserWallet's store
 * @property accountsResponseStoreFactory    factory to create store with accounts response
 * @property responseCryptoCurrenciesFactory factory for creating [CryptoCurrency] from `UserTokensResponse`
 * @property dispatchers                     dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class AccountListCryptoCurrenciesProducer @AssistedInject constructor(
    @Assisted val params: MultiWalletCryptoCurrenciesProducer.Params,
    private val userWalletsStore: UserWalletsStore,
    private val accountsResponseStoreFactory: AccountsResponseStoreFactory,
    private val responseCryptoCurrenciesFactory: ResponseCryptoCurrenciesFactory,
    override val flowProducerTools: FlowProducerTools,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletCryptoCurrenciesProducer {

    override val fallback: Option<Set<CryptoCurrency>> = emptySet<CryptoCurrency>().some()

    @Suppress("NullableToStringCall")
    override fun produce(): Flow<Set<CryptoCurrency>> {
        val userWallet = userWalletsStore.getSyncStrict(key = params.userWalletId)

        if (!userWallet.isMultiCurrency) {
            error("${this::class.simpleName ?: this::class.toString()} supports only multi-currency wallet")
        }

        return accountsResponseStoreFactory.create(userWalletId = userWallet.walletId).data
            .distinctUntilChanged()
            .map { response ->
                if (response == null) return@map emptySet()

                response.accounts.flatMapTo(hashSetOf()) { accountDTO ->
                    val accountIndex = DerivationIndex(accountDTO.derivationIndex).getOrNull()
                        ?: return@map emptySet()

                    responseCryptoCurrenciesFactory.createCurrencies(
                        tokens = accountDTO.tokens.orEmpty(),
                        userWallet = userWallet,
                        accountIndex = accountIndex,
                    )
                }
            }
            .onEmpty { emit(emptySet()) }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiWalletCryptoCurrenciesProducer.Factory {
        override fun create(params: MultiWalletCryptoCurrenciesProducer.Params): AccountListCryptoCurrenciesProducer
    }
}
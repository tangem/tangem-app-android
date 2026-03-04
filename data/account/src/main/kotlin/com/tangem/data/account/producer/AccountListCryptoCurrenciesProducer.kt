package com.tangem.data.account.producer

import arrow.core.Option
import arrow.core.some
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesProducer
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

/**
 * Implementation of [MultiWalletCryptoCurrenciesProducer] that produces crypto currencies of all accounts
 *
 * @property params                    params
 * @property userWalletsListRepository repository for getting user wallets
 * @property dispatchers               dispatchers
 *
[REDACTED_AUTHOR]
 */
internal class AccountListCryptoCurrenciesProducer @AssistedInject constructor(
    @Assisted val params: MultiWalletCryptoCurrenciesProducer.Params,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val userWalletsListRepository: UserWalletsListRepository,
    override val flowProducerTools: FlowProducerTools,
    private val dispatchers: CoroutineDispatcherProvider,
) : MultiWalletCryptoCurrenciesProducer {

    override val fallback: Option<Set<CryptoCurrency>> = emptySet<CryptoCurrency>().some()

    override fun produce(): Flow<Set<CryptoCurrency>> {
        val userWallet = userWalletsListRepository.getSyncStrict(id = params.userWalletId)

        if (!userWallet.isMultiCurrency) {
            error("${this::class.simpleName ?: this::class.toString()} supports only multi-currency wallet")
        }

        return singleAccountListSupplier.invoke(params.userWalletId)
            .map { accountList ->
                accountList.accounts
                    .filterIsInstance<Account.CryptoPortfolio>()
                    .flatMapTo(hashSetOf(), Account.CryptoPortfolio::cryptoCurrencies)
            }
            .flowOn(dispatchers.default)
    }

    @AssistedFactory
    interface Factory : MultiWalletCryptoCurrenciesProducer.Factory {
        override fun create(params: MultiWalletCryptoCurrenciesProducer.Params): AccountListCryptoCurrenciesProducer
    }
}
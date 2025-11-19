package com.tangem.domain.account.status.producer

import arrow.core.Option
import arrow.core.none
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.producer.SingleAccountListProducer
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.status.utils.CryptoCurrencyStatusesFlowFactory
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TokensGroupType
import com.tangem.domain.models.TokensSortType
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.tokens.operations.PriceChangeCalculator
import com.tangem.domain.tokens.operations.TokenListFactory
import com.tangem.domain.tokens.operations.TotalFiatBalanceCalculator
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import java.math.BigDecimal

/**
 * Produces a flow of [AccountStatusList] for a single user wallet.
 *
 * @property params Parameters containing the user wallet ID.
 * @property accountsCRUDRepository Repository for accessing account data.
 * @property singleAccountListSupplier Supplier to get the list of accounts for the user wallet.
 * @property cryptoCurrencyStatusesFlowFactory Factory to create flows of cryptocurrency statuses.
 * @property dispatchers Coroutine dispatcher provider for managing threading.
 *
[REDACTED_AUTHOR]
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSingleAccountStatusListProducer @AssistedInject constructor(
    @Assisted private val params: SingleAccountStatusListProducer.Params,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val cryptoCurrencyStatusesFlowFactory: CryptoCurrencyStatusesFlowFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : SingleAccountStatusListProducer {

    override val fallback: Option<AccountStatusList> = none()

    override fun produce(): Flow<AccountStatusList> {
        val accountListFlow = singleAccountListSupplier(
            params = SingleAccountListProducer.Params(params.userWalletId),
        )

        return accountListFlow.flatMapLatest { accountList ->
            val accountStatusFlows = accountList.accounts.mapNotNull { account ->
                if (account !is Account.CryptoPortfolio) return@mapNotNull null

                if (account.cryptoCurrencies.isEmpty()) {
                    createEmptyAccountStatusFlow(account)
                } else {
                    val userWallet = accountsCRUDRepository.getUserWallet(userWalletId = params.userWalletId)

                    getAccountStatusFlow(
                        userWallet = userWallet,
                        account = account,
                        groupType = accountList.groupType,
                        sortType = accountList.sortType,
                    )
                }
                    .distinctUntilChanged()
            }

            combine(accountStatusFlows) { accountStatuses ->
                val balances = accountStatuses.map { it.tokenList.totalFiatBalance }

                AccountStatusList(
                    userWalletId = accountList.userWalletId,
                    accountStatuses = accountStatuses.toList(),
                    totalAccounts = accountList.totalAccounts,
                    totalFiatBalance = TotalFiatBalanceCalculator.calculate(balances),
                    sortType = accountList.sortType,
                    groupType = accountList.groupType,
                )
            }
        }
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
    }

    private fun createEmptyAccountStatusFlow(account: Account.CryptoPortfolio): Flow<AccountStatus.CryptoPortfolio> {
        return flowOf(
            AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenList.Empty,
                priceChangeLce = PriceChange(
                    value = BigDecimal.ZERO.movePointLeft(2),
                    source = StatusSource.ACTUAL,
                ).lceContent(),
            ),
        )
    }

    private fun getAccountStatusFlow(
        userWallet: UserWallet,
        account: Account.CryptoPortfolio,
        groupType: TokensGroupType,
        sortType: TokensSortType,
    ): Flow<AccountStatus.CryptoPortfolio> {
        val statusesFlows = account.cryptoCurrencies.map { currency ->
            cryptoCurrencyStatusesFlowFactory.create(userWallet = userWallet, currency = currency)
                .distinctUntilChanged()
        }

        return combine(statusesFlows) { statuses ->
            val statusList = statuses.toList()

            AccountStatus.CryptoPortfolio(
                account = account,
                tokenList = TokenListFactory.create(
                    statuses = statusList,
                    groupType = groupType,
                    sortType = sortType,
                ),
                priceChangeLce = PriceChangeCalculator.calculate(statuses = statusList),
            )
        }
            .distinctUntilChanged()
    }

    @AssistedFactory
    interface Factory : SingleAccountStatusListProducer.Factory {
        override fun create(params: SingleAccountStatusListProducer.Params): DefaultSingleAccountStatusListProducer
    }
}
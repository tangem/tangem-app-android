package com.tangem.domain.account.status.producer

import arrow.core.Option
import arrow.core.none
import arrow.core.toOption
import com.tangem.core.analytics.api.AnalyticsExceptionHandler
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.repository.AccountsCRUDRepository
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.core.flow.FlowProducerTools
import com.tangem.domain.core.flow.FlowProducerTools.Companion.shareInProducer
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.network.getAddress
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.quotes.multi.MultiQuoteStatusSupplier
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceProducer
import com.tangem.domain.staking.multi.MultiStakingBalanceSupplier
import com.tangem.domain.staking.single.SingleStakingBalanceProducer.Companion.selectStakingBalance
import com.tangem.domain.tokens.operations.CryptoCurrencyStatusFactory
import com.tangem.domain.tokens.operations.PriceChangeCalculator
import com.tangem.domain.tokens.operations.TokenListFactory
import com.tangem.domain.tokens.operations.TotalFiatBalanceCalculator
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.ProducerScope
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
@Suppress("LongParameterList")
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSingleAccountStatusListProducer @AssistedInject constructor(
    @Assisted private val params: SingleAccountStatusListProducer.Params,
    private val accountsCRUDRepository: AccountsCRUDRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val flowProducerTools: FlowProducerTools,
    private val networkStatusSupplier: MultiNetworkStatusSupplier,
    private val quoteStatusSupplier: MultiQuoteStatusSupplier,
    private val stakingBalanceSupplier: MultiStakingBalanceSupplier,
    private val stakingIdFactory: StakingIdFactory,
    private val analyticsExceptionHandler: AnalyticsExceptionHandler,
) : SingleAccountStatusListProducer {

    override val fallback: Option<AccountStatusList> = none()

    override fun produce(): Flow<AccountStatusList> {
        return flattenFlow()
            .distinctUntilChanged()
            .flowOn(dispatchers.default)
            .shareInProducer(flowProducerTools, this)
    }

    @Suppress("LongMethod")
    private fun flattenFlow(): Flow<AccountStatusList> = channelFlow {
        val walletId = params.userWalletId
        val userWallet = accountsCRUDRepository.getUserWallet(userWalletId = params.userWalletId)

        val flattenCurrency: MutableSharedFlow<Map<CryptoCurrency.ID, CryptoCurrency>> = MutableSharedFlow(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        val accountListFlow: StateFlow<AccountList> = singleAccountListSupplier(walletId)
            .onEach { accountList -> flattenCurrency.tryEmit(accountList.flattenMapCurrencies()) }
            .stateIn(this)

        val hasCachedNetworks = networksRepository.hasCachedStatuses(walletId)
        if (!hasCachedNetworks) {
            send(createLoadingAccountStatusList(accountListFlow.value))
        }

        val cryptoCurrencyStatusFlow: Flow<Map<CryptoCurrency.ID, CryptoCurrencyStatus>> = flattenCurrencyStatusFlow(
            userWallet = userWallet,
            flattenCurrency = flattenCurrency,
        )

        combine(
            flow = accountListFlow,
            flow2 = cryptoCurrencyStatusFlow,
            transform = { accountList, currencyStatusMap ->
                val accountStatuses: List<AccountStatus.CryptoPortfolio> = accountList.accounts.map { acc ->
                    val account: Account.CryptoPortfolio = when (acc) {
                        is Account.CryptoPortfolio -> acc
                    }
                    if (account.cryptoCurrencies.isEmpty()) {
                        account.toEmptyAccountStatus()
                    } else {
                        val statuses: List<CryptoCurrencyStatus> = account.cryptoCurrencies.map { currency ->
                            currencyStatusMap[currency.id] ?: currency.toLoadingCurrencyStatus()
                        }
                        AccountStatus.CryptoPortfolio(
                            account = account,
                            tokenList = TokenListFactory.create(
                                statuses = statuses,
                                groupType = accountList.groupType,
                                sortType = accountList.sortType,
                            ),
                            priceChangeLce = PriceChangeCalculator.calculate(statuses = statuses),
                        )
                    }
                }
                val balances = accountStatuses.flattenTotalFiatBalance()

                AccountStatusList(
                    userWalletId = accountList.userWalletId,
                    accountStatuses = accountStatuses,
                    totalAccounts = accountList.totalAccounts,
                    totalFiatBalance = TotalFiatBalanceCalculator.calculate(balances),
                    totalArchivedAccounts = accountList.totalArchivedAccounts,
                    sortType = accountList.sortType,
                    groupType = accountList.groupType,
                )
            },
        )
            .collect { accountStatusList -> channel.send(accountStatusList) }
    }

    private fun ProducerScope<AccountStatusList>.flattenCurrencyStatusFlow(
        userWallet: UserWallet,
        flattenCurrency: MutableSharedFlow<Map<CryptoCurrency.ID, CryptoCurrency>>,
    ): Flow<Map<CryptoCurrency.ID, CryptoCurrencyStatus>> {
        val walletId = userWallet.walletId
        val networkStatusFlow: SharedFlow<Map<Network.ID, NetworkStatus>> = networkStatusFlow(walletId, flattenCurrency)
            .shareIn(this, started = SharingStarted.Eagerly, replay = 1)
        val stakingBalanceFlow: SharedFlow<Map<StakingID, Set<StakingBalance>>> = stakingFlow(userWallet)
            .shareIn(this, started = SharingStarted.Eagerly, replay = 1)
        val quoteStatusFlow: SharedFlow<Map<CryptoCurrency.RawID, QuoteStatus>> = quoteStatusFlow()
            .shareIn(this, started = SharingStarted.Eagerly, replay = 1)

        return flattenCurrency
            .distinctUntilChanged()
            .flatMapLatest { a ->
                combine(
                    flow = networkStatusFlow,
                    flow2 = stakingBalanceFlow,
                    flow3 = quoteStatusFlow,
                    transform = { b, c, d -> Box(
                        flattenCurrencyMap = a,
                        networkStatusMap = b,
                        stakingBalanceMap = c,
                        quoteStatusMap = d,
                    ) },
                )
            }
            .distinctUntilChanged()
            .map { box ->
                val flattenCurrencyMap: Map<CryptoCurrency.ID, CryptoCurrency> = box.flattenCurrencyMap
                val networkStatusMap: Map<Network.ID, NetworkStatus> = box.networkStatusMap
                val stakingBalanceMap: Map<StakingID, Set<StakingBalance>> = box.stakingBalanceMap
                val quoteStatusMap: Map<CryptoCurrency.RawID, QuoteStatus> = box.quoteStatusMap

                flattenCurrencyMap.mapValues { (id: CryptoCurrency.ID, currency) ->
                    val networkStatus: NetworkStatus? = networkStatusMap[currency.network.id]
                    val quoteStatus: QuoteStatus? = id.rawCurrencyId?.let { rawID -> quoteStatusMap[rawID] }
                    val stakingBalance: StakingBalance? = findStakingBalance(
                        networkStatus = networkStatus,
                        id = id,
                        wallet = userWallet,
                        stakingBalanceMap = stakingBalanceMap,
                    )
                    CryptoCurrencyStatusFactory.create(
                        currency = currency,
                        maybeNetworkStatus = networkStatus.toOption(),
                        maybeQuoteStatus = quoteStatus.toOption(),
                        maybeStakingBalance = stakingBalance.toOption(),
                    )
                }
            }
    }

    private fun networkStatusFlow(
        walletId: UserWalletId,
        flattenCurrency: MutableSharedFlow<Map<CryptoCurrency.ID, CryptoCurrency>>,
    ): Flow<Map<Network.ID, NetworkStatus>> = channelFlow {
        val currencyCount = flattenCurrency
            .map { map -> map.size }
            .stateIn(this, SharingStarted.Eagerly, 0)

        networkStatusSupplier(MultiNetworkStatusProducer.Params(walletId))
            // todo accounts high frequency, investigate better debounce
            .debounce {
                val count = currencyCount.value
                @Suppress("MagicNumber") when {
                    count in 10..25 -> 50L
                    count > 25 -> 100L
                    else -> 0
                }
            }
            .mapLatest { statuses -> statuses.associateBy { status -> status.network.id } }
            .distinctUntilChanged().collect { result -> channel.send(result) }
    }

    private fun stakingFlow(wallet: UserWallet): Flow<Map<StakingID, Set<StakingBalance>>> =
        if (!wallet.isMultiCurrency) {
            flowOf(emptyMap())
        } else {
            stakingBalanceSupplier(MultiStakingBalanceProducer.Params(wallet.walletId))
                .map { balances ->
                    val result = mutableMapOf<StakingID, MutableSet<StakingBalance>>()
                    balances.forEach { balance ->
                        val set = result[balance.stakingId] ?: mutableSetOf()
                        set.add(balance)
                        result[balance.stakingId] = set
                    }
                    result
                }
                .distinctUntilChanged()
        }

    private fun quoteStatusFlow(): Flow<Map<CryptoCurrency.RawID, QuoteStatus>> = quoteStatusSupplier(Unit)
        .distinctUntilChanged()

    private fun findStakingBalance(
        networkStatus: NetworkStatus?,
        id: CryptoCurrency.ID,
        wallet: UserWallet,
        stakingBalanceMap: Map<StakingID, Set<StakingBalance>>,
    ): StakingBalance? {
        if (!wallet.isMultiCurrency) {
            return null
        }
        val stakingId = stakingIdFactory.create(
            currencyId = id,
            defaultAddress = networkStatus.getAddress(),
        ).getOrNull() ?: return null
        val stakingBalance = stakingBalanceMap[stakingId] ?: return null

        return selectStakingBalance(
            currentStakingId = stakingId,
            currentBalances = stakingBalance.toList(),
            analyticsExceptionHandler = analyticsExceptionHandler,
        )
    }

    private fun Account.CryptoPortfolio.toEmptyAccountStatus() = AccountStatus.CryptoPortfolio(
        account = this,
        tokenList = TokenList.Empty,
        priceChangeLce = PriceChange(
            value = BigDecimal.ZERO.movePointLeft(2),
            source = StatusSource.ACTUAL,
        ).lceContent(),
    )

    private fun CryptoCurrency.toLoadingCurrencyStatus() = CryptoCurrencyStatus(
        currency = this,
        value = CryptoCurrencyStatus.Loading,
    )

    private fun List<AccountStatus>.flattenTotalFiatBalance(): List<TotalFiatBalance> {
        return map { accountStatus ->
            when (accountStatus) {
                is AccountStatus.CryptoPortfolio -> accountStatus.tokenList.totalFiatBalance
            }
        }
    }

    private fun createLoadingAccountStatusList(accountList: AccountList): AccountStatusList {
        return AccountStatusList(
            userWalletId = accountList.userWalletId,
            accountStatuses = accountList.accounts.map { account ->
                when (account) {
                    is Account.CryptoPortfolio -> {
                        val currencyStatuses = account.cryptoCurrencies.map {
                            CryptoCurrencyStatus(currency = it, value = CryptoCurrencyStatus.Loading)
                        }

                        AccountStatus.CryptoPortfolio(
                            account = account,
                            tokenList = TokenListFactory.create(
                                statuses = currencyStatuses,
                                groupType = accountList.groupType,
                                sortType = accountList.sortType,
                            ),
                            priceChangeLce = lceLoading(),
                        )
                    }
                }
            },
            totalAccounts = accountList.totalAccounts,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
    }

    private data class Box(
        val flattenCurrencyMap: Map<CryptoCurrency.ID, CryptoCurrency>,
        val networkStatusMap: Map<Network.ID, NetworkStatus>,
        val stakingBalanceMap: Map<StakingID, Set<StakingBalance>>,
        val quoteStatusMap: Map<CryptoCurrency.RawID, QuoteStatus>,
    )

    @AssistedFactory
    interface Factory : SingleAccountStatusListProducer.Factory {
        override fun create(params: SingleAccountStatusListProducer.Params): DefaultSingleAccountStatusListProducer
    }
}
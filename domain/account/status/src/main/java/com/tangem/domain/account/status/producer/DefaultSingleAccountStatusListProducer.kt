package com.tangem.domain.account.status.producer

import arrow.core.Option
import arrow.core.none
import arrow.core.toOption
import com.tangem.domain.account.models.AccountCurrencyId
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.models.AccountStatusList
import com.tangem.domain.account.status.contribution.BalanceContributionProvider
import com.tangem.domain.account.status.contribution.ContributionResolver
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.flow.FlowProducerTools
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.jointaccount.model.JointAccount
import com.tangem.domain.jointaccount.converter.JointAccountStatusValueConverter
import com.tangem.domain.jointaccount.supplier.SingleJointAccountListSupplier
import com.tangem.domain.core.utils.lceContent
import com.tangem.domain.core.utils.lceLoading
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.account.*
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.network.NetworkStatus
import com.tangem.domain.models.quote.PriceChange
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusProducer
import com.tangem.domain.networks.multi.MultiNetworkStatusSupplier
import com.tangem.domain.networks.repository.NetworksRepository
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.domain.polymarket.flow.PredictionAccountStatusSupplier
import com.tangem.domain.polymarket.isPredictionAccountSupported
import com.tangem.domain.quotes.multi.MultiQuoteStatusSupplier
import com.tangem.domain.tokens.operations.CryptoCurrencyStatusFactory
import com.tangem.domain.tokens.operations.PriceChangeCalculator
import com.tangem.domain.tokens.operations.TokenListFactory
import com.tangem.domain.tokens.operations.TotalFiatBalanceCalculator
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.domain.collectibles.account.flow.CollectiblesAccountStatusSupplier
import com.tangem.features.collectibles.api.CollectiblesFeatureToggles
import com.tangem.utils.logging.TangemLogger
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
 * @property flowProducerTools Tools for managing the flow producer.
 * @property userWalletsListRepository Repository for getting user wallet by id.
 * @property singleAccountListSupplier Supplier to get the list of accounts for the user wallet.
 * @property singleJointAccountListSupplier Supplier to get the joint accounts the wallet participates in.
 * @property networksRepository Repository for checking network statuses in a cache.
 * @property dispatchers Coroutine dispatcher provider for managing threading.
 * @property networkStatusSupplier Supplier for getting network statuses.
 * @property quoteStatusSupplier Supplier for getting quote statuses.
 *
[REDACTED_AUTHOR]
 */
// TODO: Move to :data:account:status [REDACTED_JIRA]
@Suppress("LongParameterList", "LargeClass")
@OptIn(ExperimentalCoroutinesApi::class)
internal class DefaultSingleAccountStatusListProducer @AssistedInject constructor(
    @Assisted private val params: SingleAccountStatusListProducer.Params,
    override val flowProducerTools: FlowProducerTools,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
    private val singleJointAccountListSupplier: SingleJointAccountListSupplier,
    private val paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
    private val predictionAccountStatusSupplier: PredictionAccountStatusSupplier,
    private val polymarketFeatureToggles: PolymarketFeatureToggles,
    private val collectiblesAccountStatusSupplier: CollectiblesAccountStatusSupplier,
    private val collectiblesFeatureToggles: CollectiblesFeatureToggles,
    private val networksRepository: NetworksRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val networkStatusSupplier: MultiNetworkStatusSupplier,
    private val quoteStatusSupplier: MultiQuoteStatusSupplier,
    private val balanceContributionProviders: Set<@JvmSuppressWildcards BalanceContributionProvider>,
) : SingleAccountStatusListProducer {

    private val logger = TangemLogger.withTag(TAG)
    private val Account.Payment.errorPaymentAccountStatus: AccountStatus.Payment
        get() = AccountStatus.Payment(this, PaymentAccountStatusValue.Error.Unavailable)
    private val Account.Money.errorMoneyAccountStatus: AccountStatus.Money
        get() = AccountStatus.Money(this, MoneyAccountStatusValue.Error.Unavailable)
    private val Account.Prediction.errorPredictionAccountStatus: AccountStatus.Prediction
        get() = AccountStatus.Prediction(this, PredictionAccountStatusValue.Error.Unavailable)
    private val Account.Collectibles.errorCollectiblesAccountStatus: AccountStatus.Collectibles
        get() = AccountStatus.Collectibles(this, CollectiblesAccountStatusValue.Error.Unavailable)
    private val Account.Collectibles.loadingCollectiblesAccountStatus: AccountStatus.Collectibles
        get() = AccountStatus.Collectibles(this, CollectiblesAccountStatusValue.Loading)
    private val emptyPriceChangeLce: Lce<Unit, PriceChange>
        get() = PriceChange(value = BigDecimal.ZERO.movePointLeft(2), source = StatusSource.ACTUAL).lceContent()
    private val Account.Joint.errorJointAccountStatus: AccountStatus.Joint
        get() = AccountStatus.Joint(
            account = this,
            value = JointAccountStatusValue.Error.Unavailable,
            tokenList = TokenList.Empty,
            priceChangeLce = emptyPriceChangeLce,
        )
    private val Account.Joint.loadingJointAccountStatus: AccountStatus.Joint
        get() = AccountStatus.Joint(
            account = this,
            value = JointAccountStatusValue.Loading,
            tokenList = TokenList.Empty,
            priceChangeLce = lceLoading(),
        )

    override val fallback: Option<AccountStatusList> = none()

    override fun produce(): Flow<AccountStatusList> {
        logger.i("produce() called for ${params.userWalletId}")
        return flattenFlow()
            .onEach { list ->
                logger.i(
                    "produce()[${params.userWalletId}] emit: accounts=${list.accountStatuses.size}, " +
                        "currencies=${list.flattenCurrencies().size}, " +
                        "totalFiatType=${list.totalFiatBalance::class.simpleName}",
                )
            }
            .flowOn(dispatchers.default)
    }

    @Suppress("LongMethod")
    private fun flattenFlow(): Flow<AccountStatusList> = channelFlow {
        val walletId = params.userWalletId
        logger.i("flattenFlow[$walletId]: start")
        val userWallet = userWalletsListRepository.getSyncStrict(id = params.userWalletId)
        logger.i("flattenFlow[$walletId]: userWallet resolved (type=${userWallet::class.simpleName})")

        val flattenCurrency: MutableSharedFlow<Map<AccountCurrencyId, CryptoCurrency>> = MutableSharedFlow(
            replay = 1,
            onBufferOverflow = BufferOverflow.DROP_OLDEST,
        )

        val accountListFlow: StateFlow<AccountList> = singleAccountListSupplier(walletId)
            .onEach { accountList ->
                logger.i(
                    "flattenFlow[$walletId]: accountList emitted accounts=${accountList.accounts.size}, " +
                        "currencies=${accountList.flattenMapCurrencies().size}",
                )
                flattenCurrency.tryEmit(accountList.flattenMapCurrencies())
            }
            .stateIn(this)

        val hasCachedNetworks = networksRepository.hasCachedStatuses(walletId)
        logger.i("flattenFlow[$walletId]: hasCachedNetworks=$hasCachedNetworks")
        if (!hasCachedNetworks) {
            val initialAccounts = accountListFlow.value.accounts.size
            logger.i("flattenFlow[$walletId]: sending Loading placeholder (accounts=$initialAccounts)")
            send(createLoadingAccountStatusList(accountListFlow.value))
        }

        val cryptoCurrencyStatusFlow: Flow<Map<AccountCurrencyId, CryptoCurrencyStatus>> = flattenCurrencyStatusFlow(
            userWallet = userWallet,
            flattenCurrency = flattenCurrency,
        )

        val specialStatusesFlow: Flow<Map<AccountId, AccountStatus>> = combine(
            flow = paymentAccountStatusSupplier.invoke(userWalletId = walletId),
            flow2 = moneyAccountStatusSupplier.invoke(userWalletId = walletId),
            flow3 = predictionStatusFlow(userWallet = userWallet),
            flow4 = collectiblesStatusFlow(walletId = walletId),
        ) { paymentStatus, moneyAccountStatus, predictionValue, collectiblesValue ->
            val predictionStatus = AccountStatus.Prediction(
                account = Account.Prediction(userWalletId = walletId),
                value = predictionValue,
            )
            val collectiblesStatus = AccountStatus.Collectibles(
                account = Account.Collectibles(userWalletId = walletId),
                value = collectiblesValue,
            )

            mapOf(
                paymentStatus.accountId to paymentStatus,
                moneyAccountStatus.accountId to moneyAccountStatus,
                predictionStatus.accountId to predictionStatus,
                collectiblesStatus.accountId to collectiblesStatus,
            )
        }

        combine(
            flow = accountListFlow,
            flow2 = cryptoCurrencyStatusFlow,
            flow3 = specialStatusesFlow,
            flow4 = jointAccountsFlow(walletId = walletId),
        ) { accountList, currencyStatusMap, specialStatuses, jointAccounts ->
            logger.i(
                "combine[$walletId] transform:" +
                    "accounts=${accountList.accounts.size}, " +
                    "currencyStatusMap=${currencyStatusMap.size}, " +
                    "specialStatuses=${specialStatuses.size}, " +
                    "jointAccounts=${jointAccounts?.size}",
            )
            val accountStatuses = accountList.accounts.map { account ->
                when (account) {
                    is Account.Personal -> buildCryptoPortfolioStatus(account, currencyStatusMap, accountList)
                    is Account.Payment -> specialStatuses[account.accountId] ?: account.errorPaymentAccountStatus
                    is Account.Money -> specialStatuses[account.accountId] ?: account.errorMoneyAccountStatus
                    is Account.Prediction ->
                        specialStatuses[account.accountId] ?: account.errorPredictionAccountStatus
                    is Account.Collectibles ->
                        specialStatuses[account.accountId] ?: account.errorCollectiblesAccountStatus
                    is Account.Joint -> buildJointStatus(
                        account = account,
                        currencyStatusMap = currencyStatusMap,
                        jointAccounts = jointAccounts,
                        accountList = accountList,
                    )
                }
            }
            buildAccountStatusList(accountList = accountList, accountStatuses = accountStatuses)
        }.collect { accountStatusList -> channel.send(accountStatusList) }
    }

    private fun jointAccountsFlow(walletId: UserWalletId): Flow<Map<String, JointAccount>?> {
        return singleJointAccountListSupplier(userWalletId = walletId)
            .map<List<JointAccount>, Map<String, JointAccount>?> { accounts ->
                accounts.associateBy(JointAccount::cryptoAccountId)
            }
            .onStart { emit(null) }
    }

    /**
     * The lifecycle state comes from the joint accounts, the money from the network statuses — read by the Safe
     * address, so only an active account has any. A pending one, or one this device has no record of yet
     */
    private fun buildJointStatus(
        account: Account.Joint,
        currencyStatusMap: Map<AccountCurrencyId, CryptoCurrencyStatus>,
        jointAccounts: Map<String, JointAccount>?,
        accountList: AccountList,
    ): AccountStatus.Joint {
        val loadedAccounts = jointAccounts ?: return account.loadingJointAccountStatus
        val jointAccount = loadedAccounts[account.accountId.value] ?: return account.errorJointAccountStatus
        val value = JointAccountStatusValueConverter.convert(value = jointAccount)

        if (value !is JointAccountStatusValue.Active || account.cryptoCurrencies.isEmpty()) {
            return AccountStatus.Joint(
                account = account,
                value = value,
                tokenList = TokenList.Empty,
                priceChangeLce = emptyPriceChangeLce,
            )
        }

        val statuses: List<CryptoCurrencyStatus> = account.cryptoCurrencies.map { currency ->
            currencyStatusMap[account.accountId to currency.id] ?: currency.toLoadingCurrencyStatus()
        }
        return AccountStatus.Joint(
            account = account,
            value = value,
            tokenList = TokenListFactory.create(
                statuses = statuses,
                groupType = accountList.groupType,
                sortType = accountList.sortType,
            ),
            priceChangeLce = PriceChangeCalculator.calculate(statuses = statuses),
        )
    }

    /**
     * Subscribed only for a wallet that can hold the account, and seeded before the join: this flow gates every
     * account on the screen, so a supplier that goes quiet must cost one loading row rather than all of them.
     */
    private fun collectiblesStatusFlow(walletId: UserWalletId): Flow<CollectiblesAccountStatusValue> {
        if (!collectiblesFeatureToggles.isCollectiblesEnabled) {
            return flowOf(CollectiblesAccountStatusValue.Error.Unavailable)
        }

        return collectiblesAccountStatusSupplier(userWalletId = walletId)
            .onStart { emit(CollectiblesAccountStatusValue.Loading) }
    }

    private fun predictionStatusFlow(userWallet: UserWallet): Flow<PredictionAccountStatusValue> {
        if (!polymarketFeatureToggles.isPolymarketEnabled || !userWallet.isPredictionAccountSupported) {
            return flowOf(PredictionAccountStatusValue.Error.Unavailable)
        }

        return predictionAccountStatusSupplier.invoke(userWalletId = userWallet.walletId)
            .onStart { emit(PredictionAccountStatusValue.Loading) }
    }

    private fun buildCryptoPortfolioStatus(
        account: Account.Personal,
        currencyStatusMap: Map<AccountCurrencyId, CryptoCurrencyStatus>,
        accountList: AccountList,
    ): AccountStatus.Personal {
        if (account.cryptoCurrencies.isEmpty()) return account.toEmptyAccountStatus()

        val statuses: List<CryptoCurrencyStatus> = account.cryptoCurrencies.map { currency ->
            val acId = account.accountId to currency.id
            currencyStatusMap[acId] ?: currency.toLoadingCurrencyStatus()
        }
        return AccountStatus.Personal(
            account = account,
            tokenList = TokenListFactory.create(
                statuses = statuses,
                groupType = accountList.groupType,
                sortType = accountList.sortType,
            ),
            priceChangeLce = PriceChangeCalculator.calculate(statuses = statuses),
        )
    }

    private fun buildAccountStatusList(
        accountList: AccountList,
        accountStatuses: List<AccountStatus>,
    ): AccountStatusList {
        val balances = accountStatuses.flattenTotalFiatBalance()
        return AccountStatusList(
            userWalletId = accountList.userWalletId,
            accountStatuses = accountStatuses,
            totalAccounts = accountList.totalAccounts,
            totalFiatBalance = TotalFiatBalanceCalculator.calculate(balances),
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalJointAccounts = accountList.totalJointAccounts,
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
    }

    private fun ProducerScope<AccountStatusList>.flattenCurrencyStatusFlow(
        userWallet: UserWallet,
        flattenCurrency: MutableSharedFlow<Map<AccountCurrencyId, CryptoCurrency>>,
    ): Flow<Map<AccountCurrencyId, CryptoCurrencyStatus>> {
        val walletId = userWallet.walletId
        val networkStatusFlow: SharedFlow<Map<Network.ID, NetworkStatus>> = networkStatusFlow(walletId)
            .onEach { logger.i("flattenCurrencyStatusFlow[$walletId]: networkStatuses emitted size=${it.size}") }
            .shareIn(this, started = SharingStarted.Eagerly, replay = 1)
        val quoteStatusFlow: SharedFlow<Map<CryptoCurrency.RawID, QuoteStatus>> = quoteStatusFlow()
            .onEach { logger.i("flattenCurrencyStatusFlow[$walletId]: quoteStatuses emitted size=${it.size}") }
            .shareIn(this, started = SharingStarted.Eagerly, replay = 1)
        val contributionResolversFlow: SharedFlow<List<ContributionResolver>> = contributionResolversFlow(userWallet)
            .onEach { logger.i("flattenCurrencyStatusFlow[$walletId]: contributionResolvers emitted size=${it.size}") }
            .shareIn(this, started = SharingStarted.Eagerly, replay = 1)

        return flattenCurrency
            .distinctUntilChanged()
            .onEach { logger.i("flattenCurrencyStatusFlow[$walletId]: flattenCurrency emitted size=${it.size}") }
            .flatMapLatest { a ->
                combine(
                    flow = networkStatusFlow,
                    flow2 = quoteStatusFlow,
                    flow3 = contributionResolversFlow,
                    transform = { b, c, d -> Box(
                        flattenCurrencyMap = a,
                        networkStatusMap = b,
                        quoteStatusMap = c,
                        contributionResolvers = d,
                    ) },
                )
            }
            .distinctUntilChanged()
            .onEach { box ->
                logger.i(
                    "flattenCurrencyStatusFlow[$walletId]: box emitted " +
                        "currencies=${box.flattenCurrencyMap.size}, " +
                        "networks=${box.networkStatusMap.size}, " +
                        "quotes=${box.quoteStatusMap.size}, " +
                        "contributors=${box.contributionResolvers.size}",
                )
            }
            .map { box -> box.toCurrencyStatuses() }
    }

    private fun Box.toCurrencyStatuses(): Map<AccountCurrencyId, CryptoCurrencyStatus> {
        return flattenCurrencyMap.mapValues { (acId: AccountCurrencyId, currency) ->
            val (_, id) = acId
            val networkStatus: NetworkStatus? = networkStatusMap[currency.network.id]
            val quoteStatus: QuoteStatus? = id.rawCurrencyId?.let { rawID -> quoteStatusMap[rawID] }
            val contributions = contributionResolvers.mapNotNull { resolver ->
                resolver.resolve(currency = currency, networkStatus = networkStatus)
            }

            CryptoCurrencyStatusFactory.create(
                currency = currency,
                maybeNetworkStatus = networkStatus.toOption(),
                maybeQuoteStatus = quoteStatus.toOption(),
                contributions = contributions,
            )
        }
    }

    /** Frames of every registered [BalanceContributionProvider]. */
    private fun contributionResolversFlow(userWallet: UserWallet): Flow<List<ContributionResolver>> {
        if (balanceContributionProviders.isEmpty()) {
            return flowOf(emptyList())
        }

        return combine(
            balanceContributionProviders.map { provider -> provider.contributions(userWallet) },
        ) { resolvers ->
            resolvers.toList()
        }
    }

    private fun networkStatusFlow(walletId: UserWalletId): Flow<Map<Network.ID, NetworkStatus>> =
        networkStatusSupplier(MultiNetworkStatusProducer.Params(walletId))
            .mapLatest { statuses -> statuses.associateBy { status -> status.network.id } }
            .distinctUntilChanged()

    private fun quoteStatusFlow(): Flow<Map<CryptoCurrency.RawID, QuoteStatus>> = quoteStatusSupplier(Unit)
        .distinctUntilChanged()

    private fun Account.Personal.toEmptyAccountStatus() = AccountStatus.Personal(
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
                is AccountStatus.Payment -> accountStatus.value.totalFiatBalance
                is AccountStatus.Money -> accountStatus.value.totalFiatBalance
                is AccountStatus.Prediction -> accountStatus.value.totalFiatBalance
                is AccountStatus.Collectibles -> accountStatus.value.totalFiatBalance
            }
        }
    }

    private fun createLoadingAccountStatusList(accountList: AccountList): AccountStatusList {
        return AccountStatusList(
            userWalletId = accountList.userWalletId,
            accountStatuses = accountList.accounts.mapNotNull { account ->
                when (account) {
                    is Account.Personal -> {
                        val currencyStatuses = account.cryptoCurrencies.map {
                            CryptoCurrencyStatus(currency = it, value = CryptoCurrencyStatus.Loading)
                        }

                        AccountStatus.Personal(
                            account = account,
                            tokenList = TokenListFactory.create(
                                statuses = currencyStatuses,
                                groupType = accountList.groupType,
                                sortType = accountList.sortType,
                            ),
                            priceChangeLce = lceLoading(),
                        )
                    }
                    is Account.Payment -> null
                    is Account.Money -> null
                    is Account.Prediction -> null
                    is Account.Collectibles -> account.loadingCollectiblesAccountStatus
                    is Account.Joint -> account.loadingJointAccountStatus
                }
            },
            totalAccounts = accountList.totalAccounts,
            totalArchivedAccounts = accountList.totalArchivedAccounts,
            totalJointAccounts = accountList.totalJointAccounts,
            totalFiatBalance = TotalFiatBalance.Loading,
            sortType = accountList.sortType,
            groupType = accountList.groupType,
        )
    }

    private data class Box(
        val flattenCurrencyMap: Map<AccountCurrencyId, CryptoCurrency>,
        val networkStatusMap: Map<Network.ID, NetworkStatus>,
        val quoteStatusMap: Map<CryptoCurrency.RawID, QuoteStatus>,
        val contributionResolvers: List<ContributionResolver>,
    )

    @AssistedFactory
    interface Factory : SingleAccountStatusListProducer.Factory {
        override fun create(params: SingleAccountStatusListProducer.Params): DefaultSingleAccountStatusListProducer
    }

    private companion object {
        const val TAG = "SingleAccountStatusListProducer"
    }
}
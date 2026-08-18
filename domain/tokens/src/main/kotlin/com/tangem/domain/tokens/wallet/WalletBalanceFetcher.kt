package com.tangem.domain.tokens.wallet

import arrow.core.Either
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.tokens.CardCryptoCurrencyFactory
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncStrict
import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.express.ExpressServiceFetcher
import com.tangem.domain.express.models.ExpressAsset
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.networks.multi.MultiNetworkStatusFetcher
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.polymarket.flow.PredictionAccountStatusFetcher
import com.tangem.domain.quotes.multi.MultiQuoteStatusFetcher
import com.tangem.domain.staking.StakingIdFactory
import com.tangem.domain.staking.multi.MultiStakingBalanceFetcher
import com.tangem.domain.tokens.BalanceFetchingOperations
import com.tangem.domain.tokens.FetchErrorFormatter
import com.tangem.domain.tokens.MultiWalletAccountListFetcher
import com.tangem.domain.tokens.MultiWalletCryptoCurrenciesSupplier
import com.tangem.domain.tokens.wallet.implementor.MultiWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletBalanceFetcher
import com.tangem.domain.tokens.wallet.implementor.SingleWalletWithTokenBalanceFetcher
import com.tangem.domain.virtualaccount.flow.VirtualAccountStatusFetcher
import com.tangem.features.polymarket.api.PolymarketFeatureToggles
import com.tangem.features.virtualaccount.VirtualAccountFeatureToggles
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/**
 * Fetcher of wallet balance by [UserWalletId]
 *
 * Uses [BalanceFetchingOperations] for shared fetching logic.
 *
 * @property userWalletsListRepository           user wallets list repository
 * @property expressServiceFetcher               express service fetcher
 * @property multiWalletBalanceFetcher           balance fetcher of multi-currency wallet
 * @property singleWalletWithTokenBalanceFetcher balance fetcher of single-currency wallet with token
 * @property singleWalletBalanceFetcher          balance fetcher of single-currency wallet
 * @property balanceFetchingOperations           shared operations for fetching balance data
 * @property paymentAccountStatusFetcher         payment account status fetcher
 * @property dispatchers                         dispatchers
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
class WalletBalanceFetcher internal constructor(
    private val userWalletsListRepository: UserWalletsListRepository,
    private val expressServiceFetcher: ExpressServiceFetcher,
    private val multiWalletBalanceFetcher: BaseWalletBalanceFetcher,
    private val singleWalletWithTokenBalanceFetcher: BaseWalletBalanceFetcher,
    private val singleWalletBalanceFetcher: BaseWalletBalanceFetcher,
    private val balanceFetchingOperations: BalanceFetchingOperations,
    private val paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
    private val virtualAccountStatusFetcher: VirtualAccountStatusFetcher,
    private val predictionAccountStatusFetcher: PredictionAccountStatusFetcher,
    private val virtualAccountsFeatureToggles: VirtualAccountFeatureToggles,
    private val polymarketFeatureToggles: PolymarketFeatureToggles,
    private val dispatchers: CoroutineDispatcherProvider,
) : FlowFetcher<WalletBalanceFetcher.Params> {

    /** Test constructor with direct fetcher dependencies for unit testing */
    internal constructor(
        userWalletsListRepository: UserWalletsListRepository,
        expressServiceFetcher: ExpressServiceFetcher,
        multiWalletBalanceFetcher: BaseWalletBalanceFetcher,
        singleWalletWithTokenBalanceFetcher: BaseWalletBalanceFetcher,
        singleWalletBalanceFetcher: BaseWalletBalanceFetcher,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
        paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        virtualAccountStatusFetcher: VirtualAccountStatusFetcher,
        predictionAccountStatusFetcher: PredictionAccountStatusFetcher,
        stakingIdFactory: StakingIdFactory,
        virtualAccountsFeatureToggles: VirtualAccountFeatureToggles,
        polymarketFeatureToggles: PolymarketFeatureToggles,
        dispatchers: CoroutineDispatcherProvider,
    ) : this(
        userWalletsListRepository = userWalletsListRepository,
        expressServiceFetcher = expressServiceFetcher,
        multiWalletBalanceFetcher = multiWalletBalanceFetcher,
        singleWalletWithTokenBalanceFetcher = singleWalletWithTokenBalanceFetcher,
        singleWalletBalanceFetcher = singleWalletBalanceFetcher,
        balanceFetchingOperations = BalanceFetchingOperations(
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
        ),
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        virtualAccountStatusFetcher = virtualAccountStatusFetcher,
        predictionAccountStatusFetcher = predictionAccountStatusFetcher,
        virtualAccountsFeatureToggles = virtualAccountsFeatureToggles,
        polymarketFeatureToggles = polymarketFeatureToggles,
        dispatchers = dispatchers,
    )

    /** Additional constructor without internal dependencies */
    constructor(
        userWalletsListRepository: UserWalletsListRepository,
        cardCryptoCurrencyFactory: CardCryptoCurrencyFactory,
        expressServiceFetcher: ExpressServiceFetcher,
        multiWalletAccountListFetcher: MultiWalletAccountListFetcher,
        multiWalletCryptoCurrenciesSupplier: MultiWalletCryptoCurrenciesSupplier,
        multiNetworkStatusFetcher: MultiNetworkStatusFetcher,
        multiQuoteStatusFetcher: MultiQuoteStatusFetcher,
        multiStakingBalanceFetcher: MultiStakingBalanceFetcher,
        paymentAccountStatusFetcher: PaymentAccountStatusFetcher,
        virtualAccountStatusFetcher: VirtualAccountStatusFetcher,
        predictionAccountStatusFetcher: PredictionAccountStatusFetcher,
        virtualAccountsFeatureToggles: VirtualAccountFeatureToggles,
        polymarketFeatureToggles: PolymarketFeatureToggles,
        stakingIdFactory: StakingIdFactory,
        dispatchers: CoroutineDispatcherProvider,
    ) : this(
        userWalletsListRepository = userWalletsListRepository,
        expressServiceFetcher = expressServiceFetcher,
        multiWalletBalanceFetcher = MultiWalletBalanceFetcher(
            multiWalletAccountListFetcher = multiWalletAccountListFetcher,
            multiWalletCryptoCurrenciesSupplier = multiWalletCryptoCurrenciesSupplier,
        ),
        singleWalletWithTokenBalanceFetcher = SingleWalletWithTokenBalanceFetcher(
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        ),
        singleWalletBalanceFetcher = SingleWalletBalanceFetcher(
            cardCryptoCurrencyFactory = cardCryptoCurrencyFactory,
        ),
        balanceFetchingOperations = BalanceFetchingOperations(
            multiNetworkStatusFetcher = multiNetworkStatusFetcher,
            multiQuoteStatusFetcher = multiQuoteStatusFetcher,
            multiStakingBalanceFetcher = multiStakingBalanceFetcher,
            stakingIdFactory = stakingIdFactory,
        ),
        paymentAccountStatusFetcher = paymentAccountStatusFetcher,
        virtualAccountStatusFetcher = virtualAccountStatusFetcher,
        predictionAccountStatusFetcher = predictionAccountStatusFetcher,
        virtualAccountsFeatureToggles = virtualAccountsFeatureToggles,
        polymarketFeatureToggles = polymarketFeatureToggles,
        dispatchers = dispatchers,
    )

    override suspend fun invoke(params: Params) = Either.catchOn(dispatchers.default) {
        val userWalletId = params.userWalletId
        val userWallet = userWalletsListRepository.getSyncStrict(userWalletId)

        val fetcher = when (userWallet) {
            is UserWallet.Hot -> multiWalletBalanceFetcher
            is UserWallet.Cold -> {
                val cardTypesResolver = userWallet.cardTypesResolver
                when {
                    cardTypesResolver.isMultiwalletAllowed() -> multiWalletBalanceFetcher
                    cardTypesResolver.isSingleWalletWithToken() -> singleWalletWithTokenBalanceFetcher
                    cardTypesResolver.isSingleWallet() -> singleWalletBalanceFetcher
                    else -> error("Unknown type of wallet: $userWalletId")
                }
            }
        }

        val currencies = fetcher.getCryptoCurrencies(userWallet = userWallet).ifEmpty {
            error("UserWallet doesn't contain crypto-currencies: $userWalletId")
        }

        fetchExpressAssets(userWallet = userWallet, currencies = currencies)

        fetcher.fetch(userWalletId = userWalletId, currencies = currencies)
    }

    private suspend fun BaseWalletBalanceFetcher.fetch(userWalletId: UserWalletId, currencies: Set<CryptoCurrency>) {
        coroutineScope {
            // Fetch balance sources in parallel
            val balanceErrors = fetchingSources.filterIsInstance<WalletFetchingSource.Balance>()
                .map { source ->
                    async {
                        balanceFetchingOperations.fetchAll(
                            userWalletId = userWalletId,
                            currencies = currencies,
                            sources = source.sources,
                        ).mapKeys { (fetchingSource, _) -> fetchingSource.name }
                    }
                }
                .awaitAll()
                .fold(emptyMap<String, Throwable>()) { acc, map -> acc + map }

            check(balanceErrors.isEmpty()) {
                val message = FetchErrorFormatter.formatWalletErrors(userWalletId, balanceErrors)
                TangemLogger.e(message)
                message
            }

            // The special accounts are refreshed after the balance error check and concurrently with each other:
            // they share no data, a wallet can hold all of them, and TangemPay may long-poll — chained, their
            // round-trips would add up on every pull-to-refresh
            if (fetchingSources.any { it is WalletFetchingSource.TangemPay }) {
                launch {
                    balanceFetchingOperations.fetchQuotes(rawCurrencyIds = setOf(TangemPayCurrencyFactory.TOKEN_ID))
                    paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWalletId))
                }
            }

            // Prediction refreshes its own quote inside the fetcher, so unlike TangemPay there is nothing to
            // pre-fetch here
            if (
                fetchingSources.any { it is WalletFetchingSource.Prediction } &&
                polymarketFeatureToggles.isPolymarketEnabled
            ) {
                launch { predictionAccountStatusFetcher.invoke(PredictionAccountStatusFetcher.Params(userWalletId)) }
            }

            if (
                fetchingSources.any { it is WalletFetchingSource.VirtualAccount } &&
                virtualAccountsFeatureToggles.isVirtualAccountsEnabled
            ) {
                launch { virtualAccountStatusFetcher.invoke(VirtualAccountStatusFetcher.Params(userWalletId)) }
            }
        }
    }

    private suspend fun fetchExpressAssets(userWallet: UserWallet, currencies: Set<CryptoCurrency>) {
        val assetIds = currencies.mapTo(hashSetOf()) { currency ->
            ExpressAsset.ID(
                networkId = currency.network.rawId,
                contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
            )
        }
        expressServiceFetcher.fetch(userWallet = userWallet, assetIds = assetIds)
    }

    /**
     * Params of [WalletBalanceFetcher]
     *
     * @property userWalletId user wallet id
     */
    data class Params(val userWalletId: UserWalletId)
}
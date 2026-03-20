package com.tangem.domain.tokens.wallet

import arrow.core.Either
import arrow.core.right
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
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
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
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

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
        stakingIdFactory: StakingIdFactory,
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

        fetcher.fetch(
            userWalletId = userWalletId,
            currencies = currencies,
            paymentAccountRefactorEnabled = params.isPaymentAccountRefactorEnabled,
        )
    }

    private suspend fun BaseWalletBalanceFetcher.fetch(
        userWalletId: UserWalletId,
        currencies: Set<CryptoCurrency>,
        paymentAccountRefactorEnabled: Boolean,
    ) {
        coroutineScope {
            val errorDeferreds = fetchingSources.map { source ->
                async {
                    when (source) {
                        is WalletFetchingSource.Balance -> {
                            balanceFetchingOperations.fetchAll(
                                userWalletId = userWalletId,
                                currencies = currencies,
                                sources = source.sources,
                            ).mapKeys { (fetchingSource, _) -> fetchingSource.name }
                        }
                        is WalletFetchingSource.TangemPay -> {
                            fetchPaymentAccount(userWalletId, paymentAccountRefactorEnabled)
                                .leftOrNull()
                                ?.let { error -> mapOf(FetchErrorFormatter.TANGEM_PAY_SOURCE_NAME to error) }
                                .orEmpty()
                        }
                    }
                }
            }

            val errors = errorDeferreds.awaitAll().fold(emptyMap<String, Throwable>()) { acc, map -> acc + map }

            check(errors.isEmpty()) {
                val message = FetchErrorFormatter.formatWalletErrors(userWalletId, errors)
                Timber.e(message)
                message
            }
        }
    }

    private suspend fun fetchExpressAssets(userWallet: UserWallet, currencies: Set<CryptoCurrency>) {
        val assetIds = currencies.mapTo(hashSetOf()) { currency ->
            ExpressAsset.ID(
                networkId = currency.network.backendId,
                contractAddress = (currency as? CryptoCurrency.Token)?.contractAddress,
            )
        }
        expressServiceFetcher.fetch(userWallet = userWallet, assetIds = assetIds)
    }

    private suspend fun fetchPaymentAccount(
        userWalletId: UserWalletId,
        paymentAccountRefactorEnabled: Boolean,
    ): Either<Throwable, Unit> {
        if (!paymentAccountRefactorEnabled) return Unit.right()

        return paymentAccountStatusFetcher.invoke(PaymentAccountStatusFetcher.Params(userWalletId))
    }

    /**
     * Params of [WalletBalanceFetcher]
     *
     * @property userWalletId user wallet id
     */
    data class Params(val userWalletId: UserWalletId, val isPaymentAccountRefactorEnabled: Boolean)
}
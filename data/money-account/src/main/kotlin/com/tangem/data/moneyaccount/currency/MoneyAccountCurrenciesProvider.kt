package com.tangem.data.moneyaccount.currency

import com.tangem.domain.account.status.provider.NonPortfolioCurrenciesProvider
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusSupplier
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountCryptoCurrencyStatusUseCase
import com.tangem.domain.quotes.single.SingleQuoteStatusFetcher
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import java.math.BigDecimal
import javax.inject.Inject

internal class MoneyAccountCurrenciesProvider @Inject constructor(
    private val moneyAccountStatusSupplier: MoneyAccountStatusSupplier,
    private val getMoneyAccountCryptoCurrencyStatusUseCase: GetMoneyAccountCryptoCurrencyStatusUseCase,
    private val currenciesRepository: CurrenciesRepository,
    private val walletManagersFacade: WalletManagersFacade,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
    private val singleQuoteStatusFetcher: SingleQuoteStatusFetcher,
) : NonPortfolioCurrenciesProvider {

    override fun observe(
        userWalletId: UserWalletId,
        currency: CryptoCurrency,
    ): Flow<Pair<Account, CryptoCurrencyStatus>> {
        return getMoneyAccountCryptoCurrencyStatusUseCase(userWalletId = userWalletId, currency = currency)
    }

    override suspend fun getStatuses(userWalletId: UserWalletId, network: Network): List<CryptoCurrencyStatus> {
        val account = getAccount(userWalletId)?.takeIf { it.cryptoCurrency.network.id == network.id }
            ?: return emptyList()
        val tokenStatus = account.cryptoCurrencyStatus ?: return emptyList()

        val accountNetwork = account.cryptoCurrency.network
        val coin = currenciesRepository.createCoinCurrency(accountNetwork)
        val coinStatus = account.statusOf(
            currency = coin,
            amount = walletManagersFacade.getNativeTokenBalance(
                userWalletId = userWalletId,
                networkId = accountNetwork.rawId,
                derivationPath = accountNetwork.derivationPath.value,
            ),
            fiatRate = getFiatRate(coin),
        )

        return listOfNotNull(coinStatus, tokenStatus)
    }

    override suspend fun findCurrency(userWalletId: UserWalletId, currencyId: CryptoCurrency.ID): CryptoCurrency? {
        return getAccount(userWalletId)?.cryptoCurrency?.takeIf { it.id == currencyId }
    }

    private suspend fun getAccount(userWalletId: UserWalletId): MoneyAccountStatusValue.Account? {
        return moneyAccountStatusSupplier(userWalletId).firstOrNull()?.value as? MoneyAccountStatusValue.Account
    }

    private suspend fun getFiatRate(currency: CryptoCurrency): BigDecimal? {
        val rawCurrencyId = currency.id.rawCurrencyId ?: return null
        val params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId)

        val cachedQuote = singleQuoteStatusSupplier.getSyncOrNull(params)?.value as? QuoteStatus.Data
        if (cachedQuote != null) return cachedQuote.fiatRate

        singleQuoteStatusFetcher(SingleQuoteStatusFetcher.Params(rawCurrencyId = rawCurrencyId, appCurrencyId = null))
            .onLeft { TangemLogger.e("Unable to fetch the ${currency.symbol} quote for the Money account fee", it) }

        return (singleQuoteStatusSupplier.getSyncOrNull(params)?.value as? QuoteStatus.Data)?.fiatRate
    }
}
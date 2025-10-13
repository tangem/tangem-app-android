package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.FeeRepository
import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode

class YieldSupplyMinAmountUseCase(
    private val feeRepository: FeeRepository,
    private val quotesRepository: QuotesRepository,
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(
        userWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, BigDecimal> = catch {
        val feeWithoutGas = feeRepository.getEthereumFeeWithoutGas(userWallet, cryptoCurrencyStatus.currency)

        val fiatRate = cryptoCurrencyStatus.value.fiatRate ?: error("Fiat rate is missing")
        require(fiatRate > BigDecimal.ZERO) { "Fiat rate for token must be > 0" }

        val nativeCryptoCurrency = currenciesRepository.getNetworkCoin(
            userWalletId = userWallet.walletId,
            networkId = cryptoCurrencyStatus.currency.network.id,
            derivationPath = cryptoCurrencyStatus.currency.network.derivationPath,
        )

        val quotes =
            quotesRepository.getMultiQuoteSyncOrNull(setOfNotNull(nativeCryptoCurrency.id.rawCurrencyId))
                ?: error("Quotes for native coin are unavailable")

        val quotesStatus = quotes.firstOrNull() ?: error("Empty quotes list for native coin")

        val nativeFiatRate = (quotesStatus.value as? QuoteStatus.Data)?.fiatRate
            ?: error("Native fiat rate is missing")
        require(nativeFiatRate > BigDecimal.ZERO) { "Native fiat rate must be > 0" }

        val nativeGas = feeWithoutGas.fixFee(nativeCryptoCurrency, ETHEREUM_CONSTANT_GAS_LIMIT)

        val rateRatio = nativeFiatRate.divide(
            fiatRate,
            cryptoCurrencyStatus.currency.decimals,
            RoundingMode.HALF_UP,
        )

        val tokenValue = rateRatio.multiply(nativeGas.amount.value)

        val feeBuffered = tokenValue.multiply(FEE_BUFFER_MULTIPLIER)

        feeBuffered
            .divide(MAX_FEE_PERCENT, cryptoCurrencyStatus.currency.decimals, RoundingMode.HALF_UP)
            .stripTrailingZeros()
    }

    private fun Fee.fixFee(cryptoCurrency: CryptoCurrency, gasLimit: BigInteger) = when (this) {
        is Fee.Ethereum.Legacy -> copy(
            gasLimit = gasLimit,
            amount = amount.copy(
                value = gasPrice.multiply(gasLimit)
                    .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
            ),
        )
        is Fee.Ethereum.EIP1559 -> copy(
            gasLimit = gasLimit,
            amount = amount.copy(
                value = maxFeePerGas.multiply(gasLimit)
                    .toBigDecimal().movePointLeft(cryptoCurrency.decimals),
            ),
        )
        else -> this
    }

    private companion object {
        val FEE_BUFFER_MULTIPLIER: BigDecimal = BigDecimal("1.25")
        val MAX_FEE_PERCENT: BigDecimal = BigDecimal("0.04")
        val ETHEREUM_CONSTANT_GAS_LIMIT = 350_000.toBigInteger()
    }
}
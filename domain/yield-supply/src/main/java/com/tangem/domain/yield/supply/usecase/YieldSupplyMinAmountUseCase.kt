package com.tangem.domain.yield.supply.usecase

import arrow.core.Either
import arrow.core.Either.Companion.catch
import arrow.core.getOrElse
import com.tangem.domain.account.status.utils.CryptoCurrencyOperations.getCoin
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.yield.supply.YieldSupplyConst.YIELD_SUPPLY_EVM_CONSTANT_GAS_LIMIT
import com.tangem.domain.yield.supply.fixFee
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Use case that calculates the minimum amount required for yield supply operations.
 *
 * The calculation is based on the estimated transaction fee converted to the token currency,
 * with a buffer multiplier applied to account for fee fluctuations.
 *
 * @return [BigDecimal] minimum amount in token currency (not native/network currency)
 */
class YieldSupplyMinAmountUseCase(
    private val feeRepository: FeeRepository,
    private val quotesRepository: QuotesRepository,
    private val singleAccountListSupplier: SingleAccountListSupplier,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, BigDecimal> = catch {
        val feeWithoutGas = feeRepository.getEthereumFeeWithoutGas(userWalletId, cryptoCurrencyStatus.currency)

        val fiatRate = cryptoCurrencyStatus.value.fiatRate ?: error("Fiat rate is missing")
        require(fiatRate > BigDecimal.ZERO) { "Fiat rate for token must be > 0" }

        val accountStatusList = singleAccountListSupplier.getSyncOrNull(userWalletId = userWalletId)
            ?: error("Account status list is missing: $userWalletId")

        val nativeCryptoCurrency = accountStatusList.getCoin(cryptoCurrencyStatus.currency)
            .getOrElse {
                error("Unable to find coin for network ID: ${cryptoCurrencyStatus.currency.network.id}")
            }

        val quotes =
            quotesRepository.getMultiQuoteSyncOrNull(setOfNotNull(nativeCryptoCurrency.id.rawCurrencyId))
                ?: error("Quotes for native coin are unavailable")

        val quotesStatus = quotes.firstOrNull() ?: error("Empty quotes list for native coin")

        val nativeFiatRate = (quotesStatus.value as? QuoteStatus.Data)?.fiatRate
            ?: error("Native fiat rate is missing")
        require(nativeFiatRate > BigDecimal.ZERO) { "Native fiat rate must be > 0" }

        val nativeGas = feeWithoutGas.fixFee(
            nativeCryptoCurrency,
            YIELD_SUPPLY_EVM_CONSTANT_GAS_LIMIT,
        )

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

    private companion object {
        val FEE_BUFFER_MULTIPLIER: BigDecimal = BigDecimal("1.25")
        val MAX_FEE_PERCENT: BigDecimal = BigDecimal("0.04")
    }
}
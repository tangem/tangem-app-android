package com.tangem.domain.yield.supply.usecase

import android.icu.util.Calendar
import arrow.core.Either
import arrow.core.Either.Companion.catch
import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.quotes.QuotesRepository
import com.tangem.domain.tokens.repository.CurrenciesRepository
import com.tangem.domain.transaction.FeeRepository
import com.tangem.domain.yield.supply.YieldSupplyConst.YIELD_SUPPLY_EVM_CONSTANT_GAS_LIMIT
import com.tangem.domain.yield.supply.fixFee
import com.tangem.domain.yield.supply.models.YieldSupplyFee
import timber.log.Timber
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Calculates current fee for Yield Supply enter transaction expressed in token units.
 */
class YieldSupplyGetCurrentFeeUseCase(
    private val feeRepository: FeeRepository,
    private val quotesRepository: QuotesRepository,
    private val currenciesRepository: CurrenciesRepository,
) {

    suspend operator fun invoke(
        userWalletId: UserWalletId,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): Either<Throwable, YieldSupplyFee> = catch {
        val feeWithoutGas = feeRepository.getEthereumFeeWithoutGas(userWalletId, cryptoCurrencyStatus.currency)

        val fiatRate = cryptoCurrencyStatus.value.fiatRate ?: error("Fiat rate is missing")
        require(fiatRate > BigDecimal.ZERO) { "Fiat rate for token must be > 0" }

        val nativeCryptoCurrency = currenciesRepository.getNetworkCoin(
            userWalletId = userWalletId,
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

        val isEthereum = cryptoCurrencyStatus.currency
            .network.id.rawId.value == Blockchain.Ethereum.id

        val isHighFee = if (isEthereum) {
            val maxFeePerGas = (feeWithoutGas as? Fee.Ethereum.EIP1559)?.maxFeePerGas ?: 0.toBigInteger()
            val increasedMaxFee = if (Calendar.getInstance().get(Calendar.MINUTE) % 2 == 0) {
                Timber.tag("isHighFee").d("fee 5x")
                maxFeePerGas * 10.toBigInteger()
            } else {
                Timber.tag("isHighFee").d("fee reduce")
                maxFeePerGas / 5.toBigInteger()
            }
            increasedMaxFee >= HIGH_ETHEREUM_FEE
        } else {
            false
        }

        Timber.tag("isHighFee").d("isHighFee $isHighFee")

        YieldSupplyFee(
            value = tokenValue.stripTrailingZeros(),
            isHighFee = isHighFee,
        )
    }

    companion object {
        private val HIGH_ETHEREUM_FEE = 400_000_000.toBigInteger()
    }
}
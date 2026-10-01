package com.tangem.data.jointaccount.gas

import arrow.core.Either
import com.tangem.blockchainsdk.utils.toBlockchain
import com.tangem.blockchainsdk.utils.toCoinId
import com.tangem.domain.jointaccount.gas.JointGasTokenResolver
import com.tangem.domain.jointaccount.model.SafeGasToken
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.quote.QuoteStatus
import com.tangem.domain.quotes.single.SingleQuoteStatusProducer
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.transaction.GaslessTransactionRepository
import java.math.BigDecimal
import java.math.MathContext
import javax.inject.Inject

/** Smart Gas tokens are the gasless service's list, and the rate is the one the regular gasless fee is priced with */
internal class DefaultJointGasTokenResolver @Inject constructor(
    private val gaslessTransactionRepository: GaslessTransactionRepository,
    private val singleQuoteStatusSupplier: SingleQuoteStatusSupplier,
) : JointGasTokenResolver {

    override suspend fun resolve(
        network: Network,
        address: String,
        rawCurrencyId: CryptoCurrency.RawID?,
    ): Either<Throwable, SafeGasToken?> = Either.catch {
        if (address.equals(SafeGasToken.Coin.address, ignoreCase = true)) return@catch SafeGasToken.Coin

        val token = gaslessTransactionRepository.getSupportedTokens(network)
            .filterIsInstance<CryptoCurrency.Token>()
            .firstOrNull { it.contractAddress.equals(address, ignoreCase = true) }
            ?: return@catch null

        val blockchain = network.toBlockchain()
        val coinRate = fiatRate(rawCurrencyId = CryptoCurrency.RawID(blockchain.toCoinId()))
        val tokenQuoteId = checkNotNull(rawCurrencyId) { "No quote id for the gas token $address" }
        val tokenRate = fiatRate(rawCurrencyId = tokenQuoteId)

        SafeGasToken.Token(
            address = token.contractAddress.lowercase(),
            decimals = token.decimals,
            coinDecimals = blockchain.decimals(),
            nativeToTokenRate = coinRate.divide(tokenRate, MathContext.DECIMAL128),
        )
    }

    private suspend fun fiatRate(rawCurrencyId: CryptoCurrency.RawID): BigDecimal {
        val quote = singleQuoteStatusSupplier.getSyncOrNull(
            params = SingleQuoteStatusProducer.Params(rawCurrencyId = rawCurrencyId),
        )
        val data = checkNotNull(quote?.value as? QuoteStatus.Data) { "No quote for ${rawCurrencyId.value}" }
        check(data.fiatRate.signum() > 0) { "Zero quote for ${rawCurrencyId.value}" }

        return data.fiatRate
    }
}
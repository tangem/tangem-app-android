package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.feature.wallet.presentation.wallet.state.model.VisaTxDetailsBottomSheetConfig
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.VisaWalletIntents
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList
import org.joda.time.DateTimeZone
import java.math.BigDecimal
import java.util.Currency

internal class VisaTxDetailsBottomSheetConverter(
    private val visaCurrency: VisaCurrency,
    private val clickIntents: VisaWalletIntents,
) : Converter<VisaTxDetails, VisaTxDetailsBottomSheetConfig> {

    override fun convert(value: VisaTxDetails): VisaTxDetailsBottomSheetConfig {
        return VisaTxDetailsBottomSheetConfig(
            transaction = createTransaction(value),
            requests = value.requests.map(::createRequest).toImmutableList(),
        )
    }

    private fun createTransaction(details: VisaTxDetails): VisaTxDetailsBottomSheetConfig.Transaction {
        return VisaTxDetailsBottomSheetConfig.Transaction(
            id = details.id,
            type = details.type,
            status = details.status,
            blockchainAmount = formatNetworkAmount(details.blockchainAmount),
            blockchainFee = formatNetworkAmount(details.blockchainFee),
            transactionAmount = formatFiatAmount(details.transactionAmount, details.fiatCurrency),
            transactionCurrencyCode = details.transactionCurrencyCode.toString(),
            merchantName = details.merchantName ?: UNKNOWN,
            merchantCity = details.merchantCity ?: UNKNOWN,
            merchantCountryCode = details.merchantCountryCode ?: UNKNOWN,
            merchantCategoryCode = details.merchantCategoryCode ?: UNKNOWN,
        )
    }

    private fun createRequest(request: VisaTxDetails.Request): VisaTxDetailsBottomSheetConfig.Request {
        val localDate = request.requestDate.withZone(DateTimeZone.getDefault())
        val exploreUrl = request.exploreUrl

        return VisaTxDetailsBottomSheetConfig.Request(
            id = request.id,
            type = request.requestType,
            status = request.requestStatus,
            blockchainAmount = formatNetworkAmount(request.blockchainAmount),
            blockchainFee = formatNetworkAmount(request.blockchainFee),
            transactionAmount = formatFiatAmount(request.transactionAmount, request.fiatCurrency),
            currencyCode = request.billingCurrencyCode.toString(),
            errorCode = request.errorCode,
            date = DateTimeFormatters.formatDate(DateTimeFormatters.dateTimeFormatter, date = localDate),
            txHash = request.txHash ?: UNKNOWN,
            txStatus = request.txStatus ?: UNKNOWN,
            onExploreClick = if (exploreUrl != null) {
                { clickIntents.onExploreClick(exploreUrl) }
            } else {
                null
            },
        )
    }

    private fun formatNetworkAmount(amount: BigDecimal): String {
        return BigDecimalFormatter.formatCryptoAmount(
            cryptoAmount = amount,
            cryptoCurrency = visaCurrency.symbol,
            decimals = visaCurrency.decimals,
        )
    }

    private fun formatFiatAmount(amount: BigDecimal, fiatCurrency: Currency): String {
        return BigDecimalFormatter.formatFiatAmount(
            fiatAmount = amount,
            fiatCurrencyCode = fiatCurrency.currencyCode,
            fiatCurrencySymbol = fiatCurrency.symbol,
        )
    }

    private companion object {
        const val UNKNOWN = "Unknown"
    }
}

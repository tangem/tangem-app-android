package com.tangem.feature.wallet.presentation.wallet.state.transformers.converter

import com.tangem.core.ui.extensions.capitalize
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.utils.BigDecimalFormatter
import com.tangem.core.ui.utils.DateTimeFormatters
import com.tangem.domain.visa.model.VisaCurrency
import com.tangem.domain.visa.model.VisaTxDetails
import com.tangem.feature.wallet.presentation.wallet.state.model.VisaTxDetailsBottomSheetConfig
import com.tangem.feature.wallet.child.wallet.model.intents.VisaWalletIntents
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
            type = details.type.capitalize(),
            status = details.status.capitalize(),
            blockchainAmount = formatNetworkAmount(details.blockchainAmount),
            blockchainFee = formatNetworkAmount(details.blockchainFee),
            transactionAmount = formatFiatAmount(details.transactionAmount, details.fiatCurrency),
            transactionCurrencyCode = details.transactionCurrencyCode.toString(),
            merchantName = details.merchantName?.capitalize() ?: UNKNOWN,
            merchantCity = details.merchantCity?.capitalize() ?: UNKNOWN,
            merchantCountryCode = details.merchantCountryCode ?: UNKNOWN,
            merchantCategoryCode = details.merchantCategoryCode ?: UNKNOWN,
        )
    }

    private fun createRequest(request: VisaTxDetails.Request): VisaTxDetailsBottomSheetConfig.Request {
        val localDate = request.requestDate.withZone(DateTimeZone.getDefault())
        val exploreUrl = request.exploreUrl

        return VisaTxDetailsBottomSheetConfig.Request(
            id = request.id,
            type = request.requestType.capitalize(),
            status = request.requestStatus.capitalize(),
            blockchainAmount = formatNetworkAmount(request.blockchainAmount),
            blockchainFee = formatNetworkAmount(request.blockchainFee),
            transactionAmount = formatFiatAmount(request.transactionAmount, request.fiatCurrency),
            currencyCode = request.billingCurrencyCode.toString(),
            errorCode = request.errorCode,
            date = DateTimeFormatters.formatDate(localDate, DateTimeFormatters.dateTimeFormatter),
            txHash = request.txHash ?: UNKNOWN,
            txStatus = request.txStatus?.capitalize() ?: UNKNOWN,
            onExploreClick = if (exploreUrl != null) {
                { clickIntents.onExploreClick(exploreUrl) }
            } else {
                null
            },
        )
    }

    private fun formatNetworkAmount(amount: BigDecimal): String {
        return amount.format { crypto(visaCurrency.symbol, visaCurrency.decimals) }
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
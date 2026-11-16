package com.tangem.data.pay.util

import com.tangem.domain.pay.model.bankofframp.BankOfframpQuote
import com.tangem.spend.datasource.pay.models.response.BankOfframpQuoteResponse
import com.tangem.utils.converter.Converter
import org.joda.time.DateTime

internal object BankOfframpQuoteConverter : Converter<BankOfframpQuoteResponse.Result, BankOfframpQuote?> {

    override fun convert(value: BankOfframpQuoteResponse.Result): BankOfframpQuote? {
        return BankOfframpQuote(
            quoteId = value.quoteId?.takeIf(String::isNotBlank) ?: return null,
            bankAccountId = value.bankAccountId?.takeIf(String::isNotBlank) ?: return null,
            feeAmount = value.feeAmount,
            sourceAmount = value.sourceAmount ?: return null,
            destinationAmount = value.destinationAmount ?: return null,
            destinationCurrency = value.destinationCurrency?.takeIf(String::isNotBlank) ?: return null,
            exchangeRate = value.exchangeRate ?: return null,
            fee = value.fee?.let(BankOfframpOptionsConverter::convertFee) ?: return null,
            eta = value.eta?.let(BankOfframpOptionsConverter::convertEta) ?: return null,
            expiresAt = value.expiresAt?.let { runCatching { DateTime.parse(it) }.getOrNull() } ?: return null,
        )
    }
}
package com.tangem.data.tangempay.util

import com.tangem.domain.tangempay.model.BankCredentials
import com.tangem.domain.tangempay.model.FeeCalculationType
import com.tangem.spend.datasource.tangempay.models.response.BankCredentialsResponse
import com.tangem.spend.datasource.tangempay.models.response.FeesResponse
import com.tangem.utils.converter.Converter
import java.math.BigDecimal
import java.util.Locale

internal object BankCredentialsConverter : Converter<BankCredentialsResponse.Result, BankCredentials> {

    private const val LEGACY_FIXED_TYPE = "FLAT"
    private const val LEGACY_PERCENTAGE_TYPE = "VARIABLE"

    override fun convert(value: BankCredentialsResponse.Result): BankCredentials {
        return BankCredentials(
            type = value.type.orEmpty(),
            beneficiaryName = value.beneficiaryName.orEmpty(),
            beneficiaryAddress = value.beneficiaryAddress.orEmpty(),
            beneficiaryBankName = value.beneficiaryBankName.orEmpty(),
            beneficiaryBankAddress = value.beneficiaryBankAddress.orEmpty(),
            accountNumber = value.accountNumber.orEmpty(),
            routingNumber = value.routingNumber.orEmpty(),
            rail = value.rail?.takeIf(String::isNotBlank),
            transferMessage = value.transferMessage?.takeIf(String::isNotBlank),
            depositFee = value.depositFee?.let(::convertDepositFee) ?: value.legacyFee()?.let(::convertLegacyFee),
            depositLimit = convertDepositLimit(value.depositLimit),
            exchangeRate = convertExchangeRate(value.exchangeRate),
            amountLimits = value.amountLimits?.let(AmountLimitsConverter::convert),
        )
    }

    private fun convertDepositFee(value: FeesResponse.Fee): BankCredentials.DepositFee? = depositFee(
        kind = value.calculationType ?: value.type,
        amount = value.amount,
        fixedAmount = value.fixedAmount,
        currency = value.currency,
    )

    private fun convertLegacyFee(value: BankCredentialsResponse.LegacyFee): BankCredentials.DepositFee? =
        depositFee(kind = value.type, amount = value.amount?.toBigDecimalOrNull())

    private fun depositFee(
        kind: String?,
        amount: BigDecimal?,
        fixedAmount: BigDecimal? = null,
        currency: String? = null,
    ): BankCredentials.DepositFee? {
        return BankCredentials.DepositFee(
            calculationType = convertCalculationType(kind) ?: return null,
            amount = amount ?: return null,
            fixedAmount = fixedAmount,
            currency = currency?.takeIf(String::isNotBlank),
        )
    }

    @Suppress("DEPRECATION")
    private fun BankCredentialsResponse.Result.legacyFee(): BankCredentialsResponse.LegacyFee? = fee

    private fun convertCalculationType(kind: String?): FeeCalculationType? {
        return when (kind?.uppercase(Locale.US)) {
            FeeCalculationType.FIXED.name, LEGACY_FIXED_TYPE -> FeeCalculationType.FIXED
            FeeCalculationType.PERCENTAGE.name, LEGACY_PERCENTAGE_TYPE -> FeeCalculationType.PERCENTAGE
            else -> null
        }
    }

    private fun convertDepositLimit(value: BankCredentialsResponse.DepositLimit?): BankCredentials.DepositLimit? {
        val amount = value?.amount?.toBigDecimalOrNull() ?: return null
        val period = value.period?.let(BankCredentials.DepositLimit.Period::fromString)
        return BankCredentials.DepositLimit(
            amount = amount,
            period = period ?: BankCredentials.DepositLimit.Period.UNKNOWN,
        )
    }

    private fun convertExchangeRate(value: BankCredentialsResponse.ExchangeRate?): BankCredentials.ExchangeRate? {
        val rate = value?.rate?.toBigDecimalOrNull() ?: return null
        val base = value.base?.takeIf(String::isNotBlank) ?: return null
        val quote = value.quote?.takeIf(String::isNotBlank) ?: return null
        return BankCredentials.ExchangeRate(rate = rate, base = base, quote = quote)
    }
}
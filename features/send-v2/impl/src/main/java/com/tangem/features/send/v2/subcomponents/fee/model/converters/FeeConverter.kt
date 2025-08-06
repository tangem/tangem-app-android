package com.tangem.features.send.v2.subcomponents.fee.model.converters

import com.tangem.blockchain.common.transaction.Fee
import com.tangem.blockchain.common.transaction.TransactionFee
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.api.entity.CustomFeeFieldUM
import com.tangem.features.send.v2.subcomponents.fee.model.SendFeeClickIntents
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeType
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList

internal class FeeConverter(
    clickIntents: SendFeeClickIntents,
    appCurrency: AppCurrency,
    feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val fees: TransactionFee,
) : Converter<FeeConverter.Data, Fee> {

    private val customFeeConverter = SendFeeCustomFieldConverter(
        clickIntents = clickIntents,
        appCurrency = appCurrency,
        feeCryptoCurrencyStatus = feeCryptoCurrencyStatus,
        normalFee = fees.normal,
    )

    override fun convert(value: Data): Fee {
        return when (fees) {
            is TransactionFee.Choosable -> {
                when (value.feeType) {
                    FeeType.Slow -> fees.minimum
                    FeeType.Market -> fees.normal
                    FeeType.Fast -> fees.priority
                    FeeType.Custom -> customFeeConverter.convertBack(value.customValues)
                }
            }
            is TransactionFee.Single ->
                when (value.feeType) {
                    FeeType.Market -> fees.normal
                    FeeType.Custom -> customFeeConverter.convertBack(value.customValues)
                    else -> fees.normal
                }
        }
    }

    data class Data(
        val customValues: ImmutableList<CustomFeeFieldUM>,
        val feeType: FeeType,
    )
}
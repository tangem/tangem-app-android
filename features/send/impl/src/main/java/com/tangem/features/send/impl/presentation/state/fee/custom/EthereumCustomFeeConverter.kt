package com.tangem.features.send.impl.presentation.state.fee.custom

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.blockchain.common.transaction.Fee
import com.tangem.common.ui.amountScreen.utils.getFiatReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.utils.parseBigDecimal
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.R
import com.tangem.features.send.impl.presentation.state.StateRouter
import com.tangem.features.send.impl.presentation.state.fee.checkExceedBalance
import com.tangem.features.send.impl.presentation.state.fields.SendTextField
import com.tangem.features.send.impl.presentation.model.SendClickIntents
import com.tangem.utils.Provider
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

internal class EthereumCustomFeeConverter(
    private val clickIntents: SendClickIntents,
    private val stateRouterProvider: Provider<StateRouter>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
) : BaseEthereumCustomFeeConverter<Fee.Ethereum> {

    private val feeCurrency: CryptoCurrencyStatus.Value?
        get() = feeCryptoCurrencyStatusProvider()?.value

    private val legacyFeeConverter = EthereumLegacyCustomFeeConverter(
        clickIntents = clickIntents,
        appCurrencyProvider = appCurrencyProvider,
        feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
    )

    private val eipFeeConverter = EthereumEIPCustomFeeConverter(
        clickIntents = clickIntents,
        appCurrencyProvider = appCurrencyProvider,
        feeCryptoCurrencyStatusProvider = feeCryptoCurrencyStatusProvider,
    )

    override fun convert(value: Fee.Ethereum): ImmutableList<SendTextField.CustomFee> {
        return buildList {
            convertFeeValue(value).let(::add)

            when (value) {
                is Fee.Ethereum.EIP1559 -> eipFeeConverter.convert(value)
                is Fee.Ethereum.Legacy -> legacyFeeConverter.convert(value)
            }
                .let(::addAll)

            convertGasLimitValue(value).let(::add)
        }
            .toImmutableList()
    }

    override fun convertBack(normalFee: Fee.Ethereum, value: ImmutableList<SendTextField.CustomFee>): Fee.Ethereum {
        return when (normalFee) {
            is Fee.Ethereum.EIP1559 -> eipFeeConverter.convertBack(normalFee = normalFee, value = value)
            is Fee.Ethereum.Legacy -> legacyFeeConverter.convertBack(normalFee = normalFee, value = value)
        }
    }

    override fun getGasLimitIndex(feeValue: Fee.Ethereum): Int {
        return when (feeValue) {
            is Fee.Ethereum.EIP1559 -> eipFeeConverter.getGasLimitIndex(feeValue)
            is Fee.Ethereum.Legacy -> legacyFeeConverter.getGasLimitIndex(feeValue)
        }
    }

    override fun onValueChange(
        feeValue: Fee.Ethereum,
        customValues: ImmutableList<SendTextField.CustomFee>,
        index: Int,
        value: String,
    ): ImmutableList<SendTextField.CustomFee> {
        return when (feeValue) {
            is Fee.Ethereum.EIP1559 -> eipFeeConverter.onValueChange(feeValue, customValues, index, value)
            is Fee.Ethereum.Legacy -> legacyFeeConverter.onValueChange(feeValue, customValues, index, value)
        }
    }

    private fun convertFeeValue(value: Fee.Ethereum): SendTextField.CustomFee {
        val feeValue = value.amount.value

        return SendTextField.CustomFee(
            value = feeValue?.parseBigDecimal(value.amount.decimals).orEmpty(),
            decimals = value.amount.decimals,
            symbol = value.amount.currencySymbol,
            onValueChange = { clickIntents.onCustomFeeValueChange(FEE_AMOUNT, it) },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next, keyboardType = KeyboardType.Number),
            title = resourceReference(R.string.send_max_fee),
            footer = resourceReference(R.string.send_custom_amount_fee_footer),
            label = getFiatReference(
                rate = feeCurrency?.fiatRate,
                value = feeValue,
                appCurrency = appCurrencyProvider(),
            ),
            keyboardActions = KeyboardActions(),
        )
    }

    private fun convertGasLimitValue(value: Fee.Ethereum): SendTextField.CustomFee {
        val isExceedBalance = checkExceedBalance(feeBalance = feeCurrency?.amount, feeAmount = value.amount.value)

        return SendTextField.CustomFee(
            value = value.gasLimit.toString(),
            decimals = GAS_DECIMALS,
            symbol = "",
            title = resourceReference(R.string.send_gas_limit),
            footer = resourceReference(R.string.send_gas_limit_footer),
            onValueChange = { clickIntents.onCustomFeeValueChange(getGasLimitIndex(value), it) },
            keyboardOptions = KeyboardOptions(
                imeAction = if (isExceedBalance) ImeAction.None else ImeAction.Done,
                keyboardType = KeyboardType.Number,
            ),
            keyboardActions = KeyboardActions(
                onDone = { clickIntents.onNextClick(stateRouterProvider().isEditState) },
            ),
        )
    }

    companion object {
        const val ETHEREUM_GAS_UNIT = "GWEI"
        const val GIGA_DECIMALS = 9
        const val GAS_DECIMALS = 0
        const val FEE_AMOUNT = 0
    }
}

internal fun MutableList<SendTextField.CustomFee>.setEmpty(index: Int) {
    set(index, this[index].copy(value = ""))
}
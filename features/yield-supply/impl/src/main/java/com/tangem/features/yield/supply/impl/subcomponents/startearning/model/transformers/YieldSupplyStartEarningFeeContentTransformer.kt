package com.tangem.features.yield.supply.impl.subcomponents.startearning.model.transformers

import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.yield.supply.models.YieldSupplyMaxFee
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

@Suppress("LongParameterList")
internal class YieldSupplyStartEarningFeeContentTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val updatedTransactionList: List<TransactionData.Uncompiled>,
    private val feeValue: BigDecimal,
    private val estimatedFeeValueInTokenCurrency: BigDecimal,
    private val maxNetworkFee: YieldSupplyMaxFee,
    private val minAmount: BigDecimal,
) : Transformer<YieldSupplyActionUM> {
    override fun transform(prevState: YieldSupplyActionUM): YieldSupplyActionUM {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val tokenFiatRate = cryptoCurrencyStatus.value.fiatRate
        val feeFiatRate = feeCryptoCurrencyStatus.value.fiatRate

        val feeFiat = feeFiatRate?.let(feeValue::multiply)
        val feeFiatValueText = feeFiat.format { fiat(appCurrency.code, appCurrency.symbol) }

        val estimatedFeeToken = estimatedFeeValueInTokenCurrency
        val estimatedFeeTokenValueText = estimatedFeeToken.format { crypto(cryptoCurrency) }
        val estimatedFiatFee = tokenFiatRate?.let(estimatedFeeToken::multiply)
        val estimatedFeeFiatValueText = estimatedFiatFee.format { fiat(appCurrency.code, appCurrency.symbol) }

        val maxFeeCryptoValueText = maxNetworkFee.tokenMaxFee.format { crypto(cryptoCurrency) }
        val maxFiatFeeValueText = maxNetworkFee.fiatMaxFee.format { fiat(appCurrency.code, appCurrency.symbol) }

        val minAmountCryptoText = minAmount.format { crypto(cryptoCurrency) }
        val minAmountFiat = tokenFiatRate?.let(minAmount::multiply)
        val minAmountFiatText = minAmountFiat.format { fiat(appCurrency.code, appCurrency.symbol) }

        val feeNoteValue = resourceReference(
            id = R.string.yield_module_fee_policy_sheet_fee_note,
            formatArgs = wrappedList(
                estimatedFeeFiatValueText,
                estimatedFeeTokenValueText,
                maxFiatFeeValueText,
                maxFeeCryptoValueText,
            ),
        )

        val minFeeNoteValue = resourceReference(
            id = R.string.yield_module_fee_policy_sheet_min_amount_note,
            formatArgs = wrappedList(
                minAmountFiatText,
                minAmountCryptoText,
            ),
        )

        return if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
            prevState.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
        } else {
            prevState.copy(
                yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                    transactionDataList = updatedTransactionList.toPersistentList(),
                    feeFiatValue = stringReference(feeFiatValueText),
                    maxNetworkFeeFiatValue = stringReference(maxFiatFeeValueText),
                    minTopUpFiatValue = stringReference(minAmountFiatText),
                    feeNoteValue = feeNoteValue,
                    minFeeNoteValue = minFeeNoteValue,
                    estimatedFiatValue = stringReference(estimatedFeeFiatValueText),
                ),
            )
        }
    }
}
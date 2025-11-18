package com.tangem.features.yield.supply.impl.subcomponents.stopearning.model.transformer

import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal

internal class YieldSupplyStopEarningFeeContentTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val transactions: List<TransactionData.Uncompiled>,
    private val feeValue: BigDecimal,
) : Transformer<YieldSupplyActionUM> {
    override fun transform(prevState: YieldSupplyActionUM): YieldSupplyActionUM {
        val feeFiatValueRaw = feeCryptoCurrencyStatus.value.fiatRate?.let { rate ->
            feeValue.multiply(rate)
        }
        val fiatFeeText = feeFiatValueRaw.format { fiat(appCurrency.code, appCurrency.symbol) }

        return if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
            prevState.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
        } else {
            prevState.copy(
                isPrimaryButtonEnabled = true,
                yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                    transactionDataList = transactions.toPersistentList(),
                    feeFiatValue = stringReference(fiatFeeText),
                    maxNetworkFeeFiatValue = TextReference.EMPTY,
                    minTopUpFiatValue = TextReference.EMPTY,
                    feeNoteValue = TextReference.EMPTY,
                    estimatedFiatValue = TextReference.EMPTY,
                ),
            )
        }
    }
}
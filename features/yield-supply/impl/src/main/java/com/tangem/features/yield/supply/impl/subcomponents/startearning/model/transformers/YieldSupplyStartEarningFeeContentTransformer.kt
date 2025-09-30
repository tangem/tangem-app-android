package com.tangem.features.yield.supply.impl.subcomponents.startearning.model.transformers

import com.tangem.blockchain.common.TransactionData
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.format.bigdecimal.crypto
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyActionUM
import com.tangem.features.yield.supply.impl.common.entity.YieldSupplyFeeUM
import com.tangem.utils.StringsSigns.DOT
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList
import java.math.BigDecimal
import java.math.RoundingMode

internal class YieldSupplyStartEarningFeeContentTransformer(
    private val cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
    private val updatedTransactionList: List<TransactionData.Uncompiled>,
    private val feeValue: BigDecimal,
    private val maxNetworkFee: BigDecimal,
) : Transformer<YieldSupplyActionUM> {
    override fun transform(prevState: YieldSupplyActionUM): YieldSupplyActionUM {
        val cryptoCurrency = cryptoCurrencyStatus.currency
        val fiatRate = cryptoCurrencyStatus.value.fiatRate
        val feeFiatRate = feeCryptoCurrencyStatus.value.fiatRate

        val crypto = feeValue.format { crypto(feeCryptoCurrencyStatus.currency) }
        val fiatFeeValue = feeFiatRate?.let(feeValue::multiply)

        val fiat = fiatFeeValue.format { fiat(appCurrency.code, appCurrency.symbol) }

        val tokenCryptoFeeValue = fiatRate?.let { rate ->
            fiatFeeValue?.divide(rate, cryptoCurrency.decimals, RoundingMode.HALF_UP)
        }

        val tokenCryptoFee = tokenCryptoFeeValue.format { crypto(cryptoCurrency) }

        val maxCryptoFee = maxNetworkFee.format { crypto(cryptoCurrency) }
        val maxFiatFeeValue = fiatRate?.let { rate ->
            maxNetworkFee.divide(rate, cryptoCurrency.decimals, RoundingMode.HALF_UP)
        }
        val maxFiatFee = maxFiatFeeValue.format { fiat(appCurrency.code, appCurrency.symbol) }

        return if (cryptoCurrencyStatus.value is CryptoCurrencyStatus.Loading) {
            prevState.copy(yieldSupplyFeeUM = YieldSupplyFeeUM.Loading)
        } else {
            prevState.copy(
                yieldSupplyFeeUM = YieldSupplyFeeUM.Content(
                    transactionDataList = updatedTransactionList.toPersistentList(),
                    feeValue = combinedReference(
                        stringReference(crypto),
                        stringReference(" $DOT "),
                        stringReference(fiat),
                    ),
                    currentNetworkFeeValue = combinedReference(
                        stringReference(tokenCryptoFee),
                        stringReference(" $DOT "),
                        stringReference(fiat),
                    ),
                    maxNetworkFeeValue = combinedReference(
                        stringReference(maxCryptoFee),
                        stringReference(" $DOT "),
                        stringReference(maxFiatFee),
                    ),
                ),
            )
        }
    }
}
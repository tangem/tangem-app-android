package com.tangem.features.send.v2.subcomponents.fee.model.transformers

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeSelectorUM
import com.tangem.features.send.v2.subcomponents.fee.ui.state.FeeUM
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class SendFeeInitialStateTransformer(
    cryptoCurrencyStatus: CryptoCurrencyStatus,
    private val feeCryptoCurrencyStatus: CryptoCurrencyStatus,
    private val appCurrency: AppCurrency,
) : Transformer<FeeUM> {

    private val cryptoCurrency = cryptoCurrencyStatus.currency

    override fun transform(prevState: FeeUM): FeeUM {
        return FeeUM.Content(
            isPrimaryButtonEnabled = false,
            feeSelectorUM = FeeSelectorUM.Loading,
            notifications = persistentListOf(),
            rate = feeCryptoCurrencyStatus.value.fiatRate,
            appCurrency = appCurrency,
            isFeeApproximate = false,
            isCustomSelected = false,
            isFeeConvertibleToFiat = feeCryptoCurrencyStatus.currency.network.hasFiatFeeRate,
            isTronToken = cryptoCurrency is CryptoCurrency.Token &&
                isTron(cryptoCurrency.network.rawId),
        )
    }
}
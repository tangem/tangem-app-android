package com.tangem.features.send.impl.presentation.state.fee

import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.lib.crypto.BlockchainUtils.isTron
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class SendFeeStateConverter(
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val feeCryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<Unit, SendStates.FeeState> {

    override fun convert(value: Unit): SendStates.FeeState {
        return SendStates.FeeState(
            feeSelectorState = FeeSelectorState.Loading,
            fee = null,
            notifications = persistentListOf(),
            rate = feeCryptoCurrencyStatusProvider()?.value?.fiatRate,
            appCurrency = appCurrencyProvider(),
            isFeeApproximate = false,
            isCustomSelected = false,
            isFeeConvertibleToFiat = cryptoCurrencyStatusProvider().currency.network.hasFiatFeeRate,
            isTronToken = cryptoCurrencyStatusProvider().currency is CryptoCurrency.Token &&
                isTron(cryptoCurrencyStatusProvider().currency.network.id.value),
        )
    }
}
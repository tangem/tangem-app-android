package com.tangem.features.send.impl.presentation.state.recipient

import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.features.send.impl.presentation.state.SendStates
import com.tangem.features.send.impl.presentation.viewmodel.SendClickIntents
import com.tangem.utils.Provider
import com.tangem.utils.converter.Converter

internal class SendRecipientStateConverter(
    private val clickIntents: SendClickIntents,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus>,
) : Converter<String, SendStates.RecipientState> {

    private val addressFieldConverter by lazy { SendRecipientAddressFieldConverter(clickIntents) }
    private val memoFieldConverter by lazy {
        SendRecipientMemoFieldConverter(
            clickIntents,
            cryptoCurrencyStatusProvider,
        )
    }

    override fun convert(value: String): SendStates.RecipientState {
        return SendStates.RecipientState(
            addressTextField = addressFieldConverter.convert(value),
            memoTextField = memoFieldConverter.convertOrNull(),
            network = cryptoCurrencyStatusProvider().currency.network.name,
            isPrimaryButtonEnabled = false,
        )
    }
}

package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.utils.transformer.Transformer

internal class SendDestinationInitialStateTransformer(
    val cryptoCurrency: CryptoCurrency,
    val isRedesignEnabled: Boolean,
    val isInitialized: Boolean = false,
) : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val memoType = when (cryptoCurrency.network.transactionExtrasType) {
            Network.TransactionExtrasType.NONE -> null
            Network.TransactionExtrasType.MEMO -> R.string.send_extras_hint_memo
            Network.TransactionExtrasType.DESTINATION_TAG -> R.string.send_destination_tag_field
        }
        val placeholder = when (cryptoCurrency.network.nameResolvingType) {
            Network.NameResolvingType.NONE -> resourceReference(R.string.send_enter_address_field)
            Network.NameResolvingType.ENS -> resourceReference(R.string.send_enter_address_field_ens)
        }
        return DestinationUM.Content(
            isPrimaryButtonEnabled = false,
            isInitialized = isInitialized,
            addressTextField = DestinationTextFieldUM.RecipientAddress(
                value = "",
                keyboardOptions = KeyboardOptions(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text,
                ),
                error = null,
                placeholder = placeholder,
                label = resourceReference(R.string.send_recipient),
                isValuePasted = false,
            ),
            memoTextField = memoType?.let {
                DestinationTextFieldUM.RecipientMemo(
                    value = "",
                    keyboardOptions = KeyboardOptions(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Text,
                    ),
                    placeholder = resourceReference(R.string.send_optional_field),
                    label = resourceReference(memoType),
                    error = resourceReference(R.string.send_memo_destination_tag_error),
                    disabledText = resourceReference(R.string.send_additional_field_already_included),
                    isEnabled = true,
                    isValuePasted = false,
                )
            },
            wallets = loadingListState(WALLET_KEY_TAG, WALLET_DEFAULT_COUNT),
            recent = loadingListState(RECENT_KEY_TAG, RECENT_DEFAULT_COUNT),
            networkName = cryptoCurrency.network.name,
            isValidating = false,
            isRedesignEnabled = isRedesignEnabled,
        )
    }
}
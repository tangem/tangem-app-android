package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import arrow.core.Either
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.transaction.error.ValidateMemoError
import com.tangem.features.send.v2.impl.R
import com.tangem.features.send.v2.subcomponents.destination.ui.state.DestinationUM
import com.tangem.utils.transformer.Transformer

internal class SendDestinationValidationResultTransformer(
    private val addressValidationResult: AddressValidationResult,
    private val memoValidationResult: Either<ValidateMemoError, Unit>,
) : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        val isValidAddress = addressValidationResult.isRight()
        val isValidMemo = memoValidationResult.isRight()

        val addressErrorText = addressValidationResult.mapLeft {
            when (it) {
                is AddressValidation.Error.DataError,
                AddressValidation.Error.InvalidAddress,
                -> R.string.send_recipient_address_error
                AddressValidation.Error.AddressInWallet -> R.string.send_error_address_same_as_wallet
            }
        }.leftOrNull()

        return state.copy(
            isValidating = false,
            isPrimaryButtonEnabled = isValidAddress && isValidMemo,
            addressTextField = state.addressTextField.copy(
                error = addressErrorText?.let(::resourceReference),
                isError = state.addressTextField.value.isNotEmpty() && !isValidAddress,
            ),
            memoTextField = state.memoTextField?.copy(
                isError = state.memoTextField.value.isNotEmpty() && !isValidMemo,
                isEnabled = !shouldDisableMemo(),
            ),
        )
    }

    /** Ripple X-Address contains memo, so memo field is unnecessary */
    private fun shouldDisableMemo(): Boolean {
        return addressValidationResult.isRight { it == AddressValidation.Success.ValidXAddress }
    }
}
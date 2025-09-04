package com.tangem.features.send.v2.subcomponents.destination.model.transformers

import arrow.core.Either
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.transaction.error.ValidateMemoError
import com.tangem.features.send.v2.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.v2.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

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

        val shouldDisableMemo = shouldDisableMemo()
        val blockchainAddress =
            (addressValidationResult.getOrNull() as? AddressValidation.Success.ValidNamedAddress)?.blockchainAddress

        val memoField = state.memoTextField
        return state.copy(
            isValidating = false,
            isPrimaryButtonEnabled = isValidAddress && isValidMemo,
            addressTextField = state.addressTextField.copy(
                error = addressErrorText?.let(::resourceReference),
                isError = state.addressTextField.value.isNotEmpty() && !isValidAddress,
                blockchainAddress = blockchainAddress,
            ),
            memoTextField = memoField?.copy(
                value = memoField.value.takeIf { !shouldDisableMemo }.orEmpty(),
                isError = memoField.value.isNotEmpty() && !isValidMemo,
                isEnabled = !shouldDisableMemo,
            ),
            recent = state.recent.map { recent ->
                recent.copy(isVisible = !isValidAddress && (recent.isLoading || recent.title != TextReference.EMPTY))
            }.toPersistentList(),
            wallets = state.wallets.map { wallet ->
                wallet.copy(isVisible = !isValidAddress && (wallet.isLoading || wallet.title != TextReference.EMPTY))
            }.toPersistentList(),
            isRecentHidden = isValidAddress,
        )
    }

    /** Ripple X-Address contains memo, so memo field is unnecessary */
    private fun shouldDisableMemo(): Boolean {
        return addressValidationResult.isRight { it == AddressValidation.Success.ValidXAddress }
    }
}
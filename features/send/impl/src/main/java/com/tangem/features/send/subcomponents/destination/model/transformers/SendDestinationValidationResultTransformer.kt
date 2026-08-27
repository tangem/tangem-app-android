package com.tangem.features.send.subcomponents.destination.model.transformers

import arrow.core.Either
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.transaction.error.AddressValidation
import com.tangem.domain.transaction.error.AddressValidationResult
import com.tangem.domain.transaction.error.ValidateMemoError
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationTextFieldUM
import com.tangem.features.send.api.subcomponents.destination.entity.DestinationUM
import com.tangem.features.send.impl.R
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toPersistentList

internal class SendDestinationValidationResultTransformer(
    private val addressValidationResult: AddressValidationResult,
    private val memoValidationResult: Either<ValidateMemoError, Unit>,
    private val isMemoRequired: Boolean = false,
) : Transformer<DestinationUM> {
    override fun transform(prevState: DestinationUM): DestinationUM {
        val state = prevState as? DestinationUM.Content ?: return prevState

        val shouldDisableMemo = shouldDisableMemo()
        val isValidAddress = addressValidationResult.isRight()
        val isValidMemo = shouldDisableMemo || memoValidationResult.isRight()

        val addressErrorText = resolveAddressErrorText()

        val blockchainAddress =
            (addressValidationResult.getOrNull() as? AddressValidation.Success.ValidNamedAddress)?.blockchainAddress

        val isMemoMissing = isMemoRequired && !shouldDisableMemo && state.memoTextField?.value.isNullOrBlank()
        return state.copy(
            isValidating = false,
            isPrimaryButtonEnabled = isValidAddress && isValidMemo && !isMemoMissing,
            addressTextField = state.addressTextField.copy(
                error = addressErrorText?.let(::resourceReference),
                isError = state.addressTextField.value.isNotEmpty() && !isValidAddress,
                blockchainAddress = blockchainAddress,
            ),
            memoTextField = buildMemoField(
                memoField = state.memoTextField,
                isValidMemo = isValidMemo,
                isMemoMissing = isMemoMissing,
                shouldDisableMemo = shouldDisableMemo,
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

    private fun resolveAddressErrorText(): Int? = addressValidationResult.mapLeft { error ->
        when (error) {
            is AddressValidation.Error.DataError,
            AddressValidation.Error.InvalidAddress,
            -> R.string.send_recipient_address_error
            AddressValidation.Error.AddressInWallet -> R.string.send_error_address_same_as_wallet
            AddressValidation.Error.RecipientWalletBackupError -> R.string.warning_backup_error_add_funds_message
        }
    }.leftOrNull()

    private fun buildMemoField(
        memoField: DestinationTextFieldUM.RecipientMemo?,
        isValidMemo: Boolean,
        isMemoMissing: Boolean,
        shouldDisableMemo: Boolean,
    ): DestinationTextFieldUM.RecipientMemo? {
        memoField ?: return null
        val isMemoFormatError = memoField.value.isNotEmpty() && !isValidMemo
        return memoField.copy(
            value = memoField.value.takeIf { !shouldDisableMemo }.orEmpty(),
            isError = isMemoFormatError || isMemoMissing,
            error = if (isMemoMissing) {
                resourceReference(R.string.send_validation_destination_tag_required_title)
            } else {
                resourceReference(R.string.send_memo_destination_tag_error)
            },
            isEnabled = !shouldDisableMemo,
        )
    }

    /** Ripple X-Address contains memo, so memo field is unnecessary */
    private fun shouldDisableMemo(): Boolean {
        return addressValidationResult.isRight { it == AddressValidation.Success.ValidXAddress }
    }
}
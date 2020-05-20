package com.tangem.tangem_sdk_new.extensions

import com.tangem.TangemSdkError
import com.tangem.tangem_sdk_new.R

fun TangemSdkError.localizedDescription(): Int {
    return when (this) {
        is TangemSdkError.TagLost -> R.string.error_tag_lost
        is TangemSdkError.ExtendedLengthNotSupported -> R.string.error_operation
        is TangemSdkError.SerializeCommandError -> R.string.error_operation
        is TangemSdkError.DeserializeApduFailed -> R.string.error_operation
        is TangemSdkError.EncodingFailedTypeMismatch -> R.string.error_operation
        is TangemSdkError.EncodingFailed -> R.string.error_operation
        is TangemSdkError.DecodingFailedMissingTag -> R.string.error_operation
        is TangemSdkError.DecodingFailedTypeMismatch -> R.string.error_operation
        is TangemSdkError.DecodingFailed -> R.string.error_operation
        is TangemSdkError.UnknownStatus -> R.string.error_operation
        is TangemSdkError.ErrorProcessingCommand -> R.string.error_operation
        is TangemSdkError.InvalidState -> R.string.error_operation
        is TangemSdkError.InsNotSupported -> R.string.error_operation
        is TangemSdkError.InvalidParams -> R.string.error_operation
        is TangemSdkError.NeedEncryption -> R.string.error_operation
        is TangemSdkError.AlreadyPersonalized -> R.string.error_already_personalized
        is TangemSdkError.CannotBeDepersonalized -> R.string.error_cannot_be_depersonalized
        is TangemSdkError.Pin1Required -> R.string.error_operation
        is TangemSdkError.AlreadyCreated -> R.string.error_already_created
        is TangemSdkError.PurgeWalletProhibited -> R.string.error_purge_prohibited
        is TangemSdkError.Pin1CannotBeChanged -> R.string.error_pin1_cannot_be_changed
        is TangemSdkError.Pin2CannotBeChanged -> R.string.error_pin2_cannot_be_changed
        is TangemSdkError.Pin1CannotBeDefault -> R.string.error_pin1_cannot_be_default
        is TangemSdkError.NoRemainingSignatures -> R.string.error_no_remaining_signatures
        is TangemSdkError.EmptyHashes -> R.string.error_empty_hashes
        is TangemSdkError.HashSizeMustBeEqual -> R.string.error_cannot_be_signed
        is TangemSdkError.CardIsEmpty -> R.string.error_card_is_empty
        is TangemSdkError.SignHashesNotAvailable -> R.string.error_cannot_be_signed
        is TangemSdkError.TooManyHashesInOneTransaction -> R.string.error_cannot_be_signed
        is TangemSdkError.NotPersonalized -> R.string.error_not_personalized
        is TangemSdkError.NotActivated -> R.string.error_not_activated
        is TangemSdkError.CardIsPurged -> R.string.error_purged
        is TangemSdkError.Pin2OrCvcRequired -> R.string.error_operation
        is TangemSdkError.VerificationFailed -> R.string.error_verification_failed
        is TangemSdkError.DataSizeTooLarge -> R.string.error_data_size_too_large
        is TangemSdkError.ExendedDataSizeTooLarge -> R.string.error_data_size_too_large_extended
        is TangemSdkError.MissingCounter -> R.string.error_missing_counter
        is TangemSdkError.OverwritingDataIsProhibited -> R.string.error_data_cannot_be_written
        is TangemSdkError.DataCannotBeWritten -> R.string.error_data_cannot_be_written
        is TangemSdkError.MissingIssuerPubicKey -> R.string.error_missing_issuer_public_key
        is TangemSdkError.UnknownError -> R.string.error_operation
        is TangemSdkError.UserCancelled -> R.string.error_user_cancelled
        is TangemSdkError.Busy -> R.string.error_busy
        is TangemSdkError.MissingPreflightRead -> R.string.error_operation
        is TangemSdkError.WrongCardNumber -> R.string.error_wrong_card_number
        is TangemSdkError.WrongCardType -> R.string.error_wrong_card_type
        is TangemSdkError.CardError -> R.string.error_card_error
    }
}
package com.tangem.tap.common.analytics

import com.tangem.common.core.TangemSdkError

object TangemSdkErrorMapper {

    // This mapping is performed to group errors in FirebaseCrashlytics.
    // At the moment, the errors in Crashlytics can only be grouped by their place of creation (class and line).
    @Suppress("LongMethod", "ComplexMethod")
    fun map(error: TangemSdkError): TangemSdkError {
        return when (error) {
            is TangemSdkError.TagLost -> TangemSdkError.TagLost()
            is TangemSdkError.ExtendedLengthNotSupported -> TangemSdkError.ExtendedLengthNotSupported()
            is TangemSdkError.SerializeCommandError -> TangemSdkError.SerializeCommandError()
            is TangemSdkError.DeserializeApduFailed -> TangemSdkError.DeserializeApduFailed()
            is TangemSdkError.EncodingFailedTypeMismatch -> TangemSdkError.EncodingFailedTypeMismatch(
                error.customMessage,
            )
            is TangemSdkError.EncodingFailed -> TangemSdkError.EncodingFailed(error.customMessage)
            is TangemSdkError.DecodingFailedMissingTag -> TangemSdkError.DecodingFailedMissingTag(
                error.customMessage,
            )
            is TangemSdkError.DecodingFailedTypeMismatch -> TangemSdkError.DecodingFailedTypeMismatch(
                error.customMessage,
            )
            is TangemSdkError.DecodingFailed -> TangemSdkError.DecodingFailed(error.customMessage)
            is TangemSdkError.InvalidResponse -> TangemSdkError.InvalidResponse()
            is TangemSdkError.UnknownStatus -> TangemSdkError.UnknownStatus(error.statusWord)
            is TangemSdkError.ErrorProcessingCommand -> TangemSdkError.ErrorProcessingCommand()
            is TangemSdkError.InvalidState -> TangemSdkError.InvalidState()
            is TangemSdkError.InsNotSupported -> TangemSdkError.InsNotSupported()
            is TangemSdkError.InvalidParams -> TangemSdkError.InvalidParams()
            is TangemSdkError.NeedEncryption -> TangemSdkError.NeedEncryption()
            is TangemSdkError.FileNotFound -> TangemSdkError.FileNotFound()
            is TangemSdkError.WalletNotFound -> TangemSdkError.WalletNotFound()
            is TangemSdkError.AlreadyPersonalized -> TangemSdkError.AlreadyPersonalized()
            is TangemSdkError.CannotBeDepersonalized -> TangemSdkError.CannotBeDepersonalized()
            is TangemSdkError.AccessCodeRequired -> TangemSdkError.AccessCodeRequired()
            is TangemSdkError.CardReadWrongWallet -> TangemSdkError.CardReadWrongWallet()
            is TangemSdkError.CardWithMaxZeroWallets -> TangemSdkError.CardWithMaxZeroWallets()
            is TangemSdkError.AlreadyCreated -> TangemSdkError.AlreadyCreated()
            is TangemSdkError.MaxNumberOfWalletsCreated -> TangemSdkError.MaxNumberOfWalletsCreated()
            is TangemSdkError.PurgeWalletProhibited -> TangemSdkError.PurgeWalletProhibited()
            is TangemSdkError.AccessCodeCannotBeChanged -> TangemSdkError.AccessCodeCannotBeChanged()
            is TangemSdkError.PasscodeCannotBeChanged -> TangemSdkError.PasscodeCannotBeChanged()
            is TangemSdkError.AccessCodeCannotBeDefault -> TangemSdkError.AccessCodeCannotBeDefault()
            is TangemSdkError.NoRemainingSignatures -> TangemSdkError.NoRemainingSignatures()
            is TangemSdkError.EmptyHashes -> TangemSdkError.EmptyHashes()
            is TangemSdkError.HashSizeMustBeEqual -> TangemSdkError.HashSizeMustBeEqual()
            is TangemSdkError.WalletIsNotCreated -> TangemSdkError.WalletIsNotCreated()
            is TangemSdkError.SignHashesNotAvailable -> TangemSdkError.SignHashesNotAvailable()
            is TangemSdkError.TooManyHashesInOneTransaction -> TangemSdkError.TooManyHashesInOneTransaction()
            is TangemSdkError.ExtendedDataSizeTooLarge -> TangemSdkError.ExtendedDataSizeTooLarge()
            is TangemSdkError.NotPersonalized -> TangemSdkError.NotPersonalized()
            is TangemSdkError.NotActivated -> TangemSdkError.NotActivated()
            is TangemSdkError.WalletIsPurged -> TangemSdkError.WalletIsPurged()
            is TangemSdkError.PasscodeRequired -> TangemSdkError.PasscodeRequired()
            is TangemSdkError.VerificationFailed -> TangemSdkError.VerificationFailed()
            is TangemSdkError.DataSizeTooLarge -> TangemSdkError.DataSizeTooLarge()
            is TangemSdkError.MissingCounter -> TangemSdkError.MissingCounter()
            is TangemSdkError.OverwritingDataIsProhibited -> TangemSdkError.OverwritingDataIsProhibited()
            is TangemSdkError.DataCannotBeWritten -> TangemSdkError.DataCannotBeWritten()
            is TangemSdkError.MissingIssuerPubicKey -> TangemSdkError.MissingIssuerPubicKey()
            is TangemSdkError.CardVerificationFailed -> TangemSdkError.CardVerificationFailed()
            is TangemSdkError.WrongAccessCode -> TangemSdkError.WrongAccessCode()
            is TangemSdkError.WrongPasscode -> TangemSdkError.WrongPasscode()
            is TangemSdkError.UnknownError -> TangemSdkError.UnknownError()
            is TangemSdkError.UserCancelled -> TangemSdkError.UserCancelled()
            is TangemSdkError.Busy -> TangemSdkError.Busy()
            is TangemSdkError.MissingPreflightRead -> TangemSdkError.MissingPreflightRead()
            is TangemSdkError.WrongCardNumber -> TangemSdkError.WrongCardNumber()
            is TangemSdkError.WrongCardType -> TangemSdkError.WrongCardType(null)
            is TangemSdkError.CardError -> TangemSdkError.CardError()
            is TangemSdkError.NotSupportedFirmwareVersion -> TangemSdkError.NotSupportedFirmwareVersion()
            is TangemSdkError.WalletError -> TangemSdkError.WalletError()
            is TangemSdkError.WalletCannotBeCreated -> TangemSdkError.WalletCannotBeCreated()
            is TangemSdkError.UnsupportedCurve -> TangemSdkError.UnsupportedCurve()
            is TangemSdkError.UnsupportedWalletConfig -> TangemSdkError.UnsupportedWalletConfig()
            is TangemSdkError.CryptoUtilsError -> TangemSdkError.CryptoUtilsError(error.customMessage)
            is TangemSdkError.NetworkError -> TangemSdkError.NetworkError(error.customMessage)
            is TangemSdkError.ExceptionError -> TangemSdkError.ExceptionError(error.cause)
            is TangemSdkError.TooMuchBackupCards -> TangemSdkError.TooMuchBackupCards()
            is TangemSdkError.BackupCardRequired -> TangemSdkError.BackupCardRequired()
            is TangemSdkError.CertificateSignatureRequired -> TangemSdkError.CertificateSignatureRequired()
            is TangemSdkError.AccessCodeOrPasscodeRequired -> TangemSdkError.AccessCodeOrPasscodeRequired()
            is TangemSdkError.ResetPinNoCardsToReset -> TangemSdkError.ResetPinNoCardsToReset()
            is TangemSdkError.ResetPinWrongCard -> TangemSdkError.ResetPinWrongCard()
            is TangemSdkError.BackupFailedCardNotLinked -> TangemSdkError.BackupFailedCardNotLinked()
            is TangemSdkError.BackupNotAllowed -> TangemSdkError.BackupNotAllowed()
            is TangemSdkError.BackupCardAlreadyAdded -> TangemSdkError.BackupCardAlreadyAdded()
            is TangemSdkError.MissingPrimaryCard -> TangemSdkError.MissingPrimaryCard()
            is TangemSdkError.MissingPrimaryAttestSignature -> TangemSdkError.MissingPrimaryAttestSignature()
            is TangemSdkError.NoBackupDataForCard -> TangemSdkError.NoBackupDataForCard()
            is TangemSdkError.BackupFailedEmptyWallets -> TangemSdkError.BackupFailedEmptyWallets()
            is TangemSdkError.BackupFailedNotEmptyWallets -> TangemSdkError.BackupFailedNotEmptyWallets()
            is TangemSdkError.NoActiveBackup -> TangemSdkError.NoActiveBackup()
            is TangemSdkError.BackupServiceInvalidState -> TangemSdkError.BackupServiceInvalidState()
            is TangemSdkError.NoBackupCardForIndex -> TangemSdkError.NoBackupCardForIndex()
            is TangemSdkError.EmptyBackupCards -> TangemSdkError.EmptyBackupCards()
            is TangemSdkError.BackupFailedWrongIssuer -> TangemSdkError.BackupFailedWrongIssuer()
            is TangemSdkError.BackupFailedHDWalletSettings -> TangemSdkError.BackupFailedHDWalletSettings()
            is TangemSdkError.BackupFailedNotEnoughCurves -> TangemSdkError.BackupFailedNotEnoughCurves()
            is TangemSdkError.BackupFailedNotEnoughWallets -> TangemSdkError.BackupFailedNotEnoughWallets()
            is TangemSdkError.FileSettingsUnsupported -> TangemSdkError.FileSettingsUnsupported()
            is TangemSdkError.FilesIsEmpty -> TangemSdkError.FilesIsEmpty()
            is TangemSdkError.FilesDisabled -> TangemSdkError.FilesDisabled()
            is TangemSdkError.HDWalletDisabled -> TangemSdkError.HDWalletDisabled()
            is TangemSdkError.WrongInteractionMode -> TangemSdkError.WrongInteractionMode()
            is TangemSdkError.IssuerSignatureLoadingFailed -> TangemSdkError.IssuerSignatureLoadingFailed()
            is TangemSdkError.BackupFailedFirmware -> TangemSdkError.BackupFailedFirmware()
            is TangemSdkError.UserForgotTheCode -> TangemSdkError.UserForgotTheCode()
            is TangemSdkError.BackupFailedIncompatibleBatch -> TangemSdkError.BackupFailedIncompatibleBatch()
            is TangemSdkError.BiometricsUnavailable -> error
            is TangemSdkError.BiometricsAuthenticationFailed -> error
            is TangemSdkError.BiometricsAuthenticationLockout -> error
            is TangemSdkError.BiometricsAuthenticationPermanentLockout -> error
            is TangemSdkError.UserCanceledBiometricsAuthentication -> error
            is TangemSdkError.EncryptionOperationFailed -> error
            is TangemSdkError.InvalidEncryptionKey -> error
            is TangemSdkError.KeyGenerationException -> error
            is TangemSdkError.MnemonicException -> error
            is TangemSdkError.WalletAlreadyCreated -> error
            is TangemSdkError.ResetBackupFailedHasBackedUpWallets -> error
            is TangemSdkError.KeysImportDisabled -> error
            is TangemSdkError.Underlying -> error
            is TangemSdkError.UserCodeRecoveryDisabled -> error
        }
    }
}

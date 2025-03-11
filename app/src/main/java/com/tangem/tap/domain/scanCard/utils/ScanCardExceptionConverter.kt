package com.tangem.tap.domain.scanCard.utils

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.domain.card.ScanCardException
import com.tangem.tap.domain.scanCard.chains.ScanChainException
import com.tangem.utils.converter.TwoWayConverter

internal class ScanCardExceptionConverter : TwoWayConverter<TangemError, ScanCardException> {
    override fun convert(value: TangemError): ScanCardException = when (value) {
        is TangemSdkError -> convertTangemSdkError(value)
        else -> ScanCardException.UnknownException(value)
    }

    override fun convertBack(value: ScanCardException): TangemError {
        return when (value) {
            is ScanCardException.UnknownException -> TangemSdkError.ExceptionError(value)
            is ScanCardException.UserCancelled -> TangemSdkError.UserCancelled()
            is ScanCardException.WrongAccessCode -> TangemSdkError.WrongAccessCode()
            is ScanCardException.WrongCardId -> TangemSdkError.WrongCardNumber(value.cardId)
            is ScanCardException.ChainException -> concertScanChainException(value)
        }
    }

    private fun convertTangemSdkError(value: TangemSdkError): ScanCardException = when (value) {
        is TangemSdkError.UserCancelled -> ScanCardException.UserCancelled
        is TangemSdkError.WrongAccessCode,
        is TangemSdkError.WrongPasscode,
        -> ScanCardException.WrongAccessCode
        is TangemSdkError.WrongCardNumber -> ScanCardException.WrongCardId(value.cardId)
        else -> ScanCardException.UnknownException(value)
    }

    private fun concertScanChainException(value: ScanCardException.ChainException): TangemError {
        return when (val e = value as? ScanChainException) {
            is ScanChainException.DisclaimerWasCanceled -> TangemSdkError.UserCancelled()
            is ScanChainException.OnboardingNeeded,
            null,
            -> TangemSdkError.ExceptionError(e?.cause)
        }
    }
}
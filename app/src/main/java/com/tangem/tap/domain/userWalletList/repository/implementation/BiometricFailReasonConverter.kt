package com.tangem.tap.domain.userWalletList.repository.implementation

import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.utils.converter.Converter

object BiometricFailReasonConverter : Converter<TangemError, Basic.BiometryFailed.BiometricFailReason> {

    override fun convert(value: TangemError): Basic.BiometryFailed.BiometricFailReason {
        // 1. Try handle TangemSdkError cases first if error has not been mapped
        when (value) {
            is TangemSdkError.AuthenticationCanceled ->
                return Basic.BiometryFailed.BiometricFailReason.AuthenticationCancelled
            is TangemSdkError.AuthenticationAlreadyInProgress ->
                return Basic.BiometryFailed.BiometricFailReason.AuthenticationAlreadyInProgress
        }

        // 2. For other errors, check if they are of type UserWalletsListError
        if (value !is UserWalletsListError) {
            return Basic.BiometryFailed.BiometricFailReason.Other(value.customMessage)
        }
        // 3. Map UserWalletsListError to BiometricFailReason
        return when (value) {
            UserWalletsListError.AllKeysInvalidated ->
                Basic.BiometryFailed.BiometricFailReason.AllKeysInvalidated
            UserWalletsListError.BiometricsAuthenticationDisabled ->
                Basic.BiometryFailed.BiometricFailReason.BiometricsAuthenticationDisabled
            is UserWalletsListError.BiometricsAuthenticationLockout ->
                if (value.isPermanent) {
                    Basic.BiometryFailed.BiometricFailReason.AuthenticationLockoutPermanent
                } else {
                    Basic.BiometryFailed.BiometricFailReason.AuthenticationLockout
                }
            else -> Basic.BiometryFailed.BiometricFailReason.Other(value.customMessage)
        }
    }
}
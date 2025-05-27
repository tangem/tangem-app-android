package com.tangem.tap.domain.userWalletList.repository.implementation

import com.tangem.common.core.TangemError
import com.tangem.core.analytics.models.Basic
import com.tangem.domain.wallets.legacy.UserWalletsListError
import com.tangem.utils.converter.Converter

object BiometricFailReasonConverter : Converter<TangemError, Basic.BiometryFailed.BiometricFailReason> {

    override fun convert(value: TangemError): Basic.BiometryFailed.BiometricFailReason {
        if (value !is UserWalletsListError) {
            return Basic.BiometryFailed.BiometricFailReason.Other(value.customMessage)
        }
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
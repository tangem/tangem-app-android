package com.tangem.common.ui.userwallet

import com.tangem.common.ui.R
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.event.SignIn
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessage
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.common.wallets.error.UnlockWalletError
import com.tangem.domain.common.wallets.error.UnlockWalletError.UnableToUnlock.Reason

inline fun UnlockWalletError.handle(
    onAlreadyUnlocked: () -> Unit = {},
    onUserCancelled: () -> Unit = {},
    analyticsEventHandler: AnalyticsEventHandler,
    noinline showMessage: (EventMessage) -> Unit,
) {
    when (this) {
        UnlockWalletError.AlreadyUnlocked -> onAlreadyUnlocked()
        UnlockWalletError.ScannedCardWalletNotMatched -> {
            showMessage(
                DialogMessage(
                    title = resourceReference(R.string.common_warning),
                    message = resourceReference(R.string.error_wrong_wallet_tapped),
                ),
            )
        }
        UnlockWalletError.UserCancelled -> onUserCancelled()
        UnlockWalletError.UserWalletNotFound -> {
            // This should never happen in this flow, as we always check for the wallet existence before unlocking
            showMessage(SnackbarMessage(TextReference.Res(R.string.generic_error)))
        }
        is UnlockWalletError.UnableToUnlock -> handleUnableToUnlock(this, analyticsEventHandler, showMessage)
    }
}

fun handleUnableToUnlock(
    error: UnlockWalletError.UnableToUnlock,
    analyticsEventHandler: AnalyticsEventHandler,
    showDialog: (DialogMessage) -> Unit,
) {
    val dialogMessage = when (error) {
        is UnlockWalletError.UnableToUnlock.WithReason -> {
            when (error.reason) {
                Reason.AllKeysInvalidated -> {
                    analyticsEventHandler.send(SignIn.ErrorBiometricUpdated())
                    DialogMessage(
                        title = resourceReference(R.string.biometric_updated_warning_title),
                        message = resourceReference(R.string.biometric_updated_warning_description),
                    )
                }
                Reason.BiometricsAuthenticationDisabled -> {
                    DialogMessage(
                        title = resourceReference(R.string.biometric_disabled_warning_title),
                        message = resourceReference(R.string.biometric_disabled_warning_description),
                    )
                }
                is Reason.BiometricsAuthenticationLockout -> {
                    if ((error.reason as Reason.BiometricsAuthenticationLockout).isPermanent) {
                        DialogMessage(
                            title = resourceReference(R.string.biometric_lockout_permanent_warning_title),
                            message = resourceReference(R.string.biometric_lockout_permanent_warning_description_2),
                        )
                    } else {
                        DialogMessage(
                            title = resourceReference(R.string.biometric_lockout_warning_title),
                            message = resourceReference(R.string.biometric_lockout_warning_description_2),
                        )
                    }
                }
            }
        }
        is UnlockWalletError.UnableToUnlock.RawException -> {
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = stringReference(error.throwable.toString()),
            )
        }
        UnlockWalletError.UnableToUnlock.Empty -> return
    }

    showDialog(dialogMessage)
}
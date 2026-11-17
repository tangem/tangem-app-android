package com.tangem.features.introduction.impl.model

import androidx.annotation.StringRes
import com.tangem.core.decompose.ui.UiMessage
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.features.hotwallet.HotWalletFeatureToggles
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR

internal class SocialLoginErrorMessages @Inject constructor(
    private val hotWalletFeatureToggles: HotWalletFeatureToggles,
) {

    private val isMultipleHotWalletsEnabled: Boolean get() = hotWalletFeatureToggles.isMultipleHotWalletsEnabled

    val backupNotSavedTitle: TextReference?
        get() = resourceReference(CoreResR.string.social_login_error_backup_not_saved_title)
            .takeIf { isMultipleHotWalletsEnabled }

    val backupNotSavedText: TextReference
        get() = if (isMultipleHotWalletsEnabled) {
            resourceReference(CoreResR.string.social_login_error_backup_not_saved_description)
        } else {
            resourceReference(CoreResR.string.social_login_error_backup_not_saved)
        }

    fun walletNotSelected(): UiMessage = if (isMultipleHotWalletsEnabled) {
        genericErrorDialog()
    } else {
        SnackbarMessage(resourceReference(CoreResR.string.common_unknown_error))
    }

    fun walletAlreadySaved(): UiMessage = if (isMultipleHotWalletsEnabled) {
        Dialogs.walletAlreadyAddedDialog()
    } else {
        DialogMessage(message = resourceReference(CoreResR.string.user_wallet_list_error_wallet_already_saved))
    }

    fun failure(error: SocialLoginError, provider: SocialLoginProvider): UiMessage {
        if (!isMultipleHotWalletsEnabled) return SnackbarMessage(error.toSnackbarText())
        return when (error) {
            SocialLoginError.NetworkError -> gotItDialog(
                title = resourceReference(CoreResR.string.social_login_error_network_title),
                message = resourceReference(CoreResR.string.social_login_error_network_description),
            )
            SocialLoginError.NoProviderAccount -> gotItDialog(
                title = providerReference(CoreResR.string.social_login_error_no_account_title, provider),
                message = providerReference(CoreResR.string.social_login_error_no_account, provider),
            )
            else -> genericErrorDialog()
        }
    }

    private fun SocialLoginError.toSnackbarText(): TextReference = when (this) {
        SocialLoginError.NetworkError -> resourceReference(CoreResR.string.hw_cloud_backup_error_network)
        is SocialLoginError.BackupWriteFailed -> resourceReference(CoreResR.string.social_login_error_backup_not_saved)
        else -> resourceReference(CoreResR.string.social_login_error_generic)
    }

    private fun genericErrorDialog() = gotItDialog(
        title = resourceReference(CoreResR.string.common_something_went_wrong),
        message = resourceReference(CoreResR.string.social_login_error_generic_v2),
    )

    private fun gotItDialog(title: TextReference, message: TextReference) = DialogMessage(
        title = title,
        message = message,
        firstActionBuilder = {
            EventMessageAction(title = resourceReference(CoreResR.string.common_got_it), onClick = {})
        },
    )
}

internal fun providerReference(@StringRes id: Int, provider: SocialLoginProvider): TextReference =
    resourceReference(id = id, formatArgs = wrappedList(provider.displayName))
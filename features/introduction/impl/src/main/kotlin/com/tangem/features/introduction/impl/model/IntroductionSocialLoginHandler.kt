package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.usecase.GetAvailableSocialLoginProvidersUseCase
import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.tangem.core.res.R as CoreResR
import com.tangem.core.ui.R as CoreUiR

/** Turns the outcome of [SocialLoginUseCase.signIn] into UI — the create vs. restore choice is not made here */
@Suppress("LongParameterList")
@ModelScoped
internal class IntroductionSocialLoginHandler @Inject constructor(
    private val getAvailableSocialLoginProvidersUseCase: GetAvailableSocialLoginProvidersUseCase,
    private val socialLoginUseCase: SocialLoginUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val appCoroutineScope: AppCoroutineScope,
    private val router: Router,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) {

    /** `null` hides the option — no Google services, or the feature toggle is off */
    var provider: SocialLoginProvider? = null
        private set

    suspend fun resolveProvider() {
        provider = getAvailableSocialLoginProvidersUseCase().firstOrNull()
    }

    fun createWalletOption(
        isEnabled: Boolean,
        isLoading: Boolean,
        onPicked: (SocialLoginProvider) -> Unit,
    ): WalletOptionUM? = provider?.let { provider ->
        WalletOptionUM(
            // TODO [REDACTED_TASK_KEY]: the coloured provider mark is not in the design system yet.
            iconRes = CoreUiR.drawable.ic_user_24,
            // TODO [REDACTED_TASK_KEY]: waiting on introduction_create_wallet_google_title in Lokalise.
            title = stringReference("Create with Google"),
            description = resourceReference(CoreResR.string.introduction_create_wallet_seed_phrase_description),
            isEnabled = isEnabled,
            isLoading = isLoading,
            onClick = { onPicked(provider) },
        )
    }

    /**
     * The same call serves both sheets — the backend decides create vs. restore.
     *
     * Safe to interrupt: the flow resumes a partly finished creation on the next attempt rather than
     * creating a second wallet.
     */
    fun start(scope: CoroutineScope, provider: SocialLoginProvider, host: Host) {
        scope.launch { signIn(provider, host) }
    }

    suspend fun signIn(provider: SocialLoginProvider, host: Host) {
        val result = socialLoginUseCase.signIn(
            provider = provider,
            onStageChanged = { stage -> host.onProgress(stage.toProgressUM(provider)) },
        )

        // Only after the result is processed: a failure signs the provider out, and a tap accepted
        // before that finishes lets the next attempt read the account it is still clearing. The
        // progress stays up until then too, or the screen would sit locked with nothing on it.
        try {
            result.fold(
                ifLeft = { error -> onFailure(provider, error) },
                ifRight = { outcome -> onSuccess(outcome) },
            )
        } finally {
            host.onProgress(null)
            host.onFinished()
        }
    }

    /** What the screen has to render while the flow runs; `null` takes that surface down */
    interface Host {

        fun onProgress(progress: SocialLoginProgressUM?)

        fun onFinished()
    }

    private suspend fun onSuccess(outcome: SocialLoginOutcome) {
        // Leaving the previous wallet selected would open its portfolio, as if the sign-in went elsewhere.
        userWalletsListRepository.select(outcome.userWalletId).onLeft { error ->
            TangemLogger.e("Failed to select the social login wallet: $error")
            uiMessageSender.send(SnackbarMessage(resourceReference(CoreResR.string.common_unknown_error)))
            return
        }

        uiMessageSender.send(
            SnackbarMessage(
                // TODO [REDACTED_TASK_KEY]: social_login_toast_wallet_created / _added are not in Lokalise yet.
                when (outcome) {
                    is SocialLoginOutcome.WalletCreated -> stringReference("Wallet created")
                    is SocialLoginOutcome.WalletRestored -> stringReference("Wallet added")
                },
            ),
        )
        router.replaceAll(AppRoute.Wallet)
    }

    /**
     * Signs out first: the alternative flows offer a plain retry, and on Android the account chooser
     * only comes back for an account the app has not remembered.
     */
    private suspend fun onFailure(provider: SocialLoginProvider, error: SocialLoginError) {
        socialLoginUseCase.signOut(provider)

        when (error) {
            SocialLoginError.BackupNotFound,
            SocialLoginError.InvalidBackupFile,
            is SocialLoginError.UnsupportedBackupVersion,
            is SocialLoginError.RecoveryKeyDecryptionFailed,
            SocialLoginError.PassphraseNotSupported,
            -> showRestoreFailedDialog(
                // TODO [REDACTED_TASK_KEY]: waiting on social_login_error_restore_failed_message in Lokalise.
                title = resourceReference(CoreResR.string.common_something_went_wrong),
                message = stringReference(
                    "We couldn't restore the wallet for this account. Try again or contact support",
                ),
            )
            SocialLoginError.CloudUnavailable -> showRestoreFailedDialog(
                // TODO [REDACTED_TASK_KEY]: waiting on social_login_error_cloud_access_* in Lokalise.
                title = stringReference("Couldn't restore wallet"),
                message = stringReference(
                    "Make sure Tangem has access to your ${provider.cloudName()}, then try again.",
                ),
            )
            else -> uiMessageSender.send(SnackbarMessage(error.toMessage()))
        }
    }

    private fun showRestoreFailedDialog(title: TextReference, message: TextReference) {
        uiMessageSender.send(
            DialogMessage(
                title = title,
                message = message,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(CoreResR.string.common_contact_support),
                        onClick = { sendSupportRequest() },
                    )
                },
                secondActionBuilder = { cancelAction(onClick = {}) },
            ),
        )
    }

    private fun sendSupportRequest() {
        appCoroutineScope.launch { sendFeedbackEmailUseCase(type = FeedbackEmailType.SocialLoginProblem) }
    }

    private fun SocialLoginProvider.displayName(): String = when (this) {
        SocialLoginProvider.Google -> "Google"
    }

    private fun SocialLoginProvider.cloudName(): String = when (this) {
        SocialLoginProvider.Google -> "Google Drive"
    }

    // TODO [REDACTED_TASK_KEY]: social_login_verifying_account / _preparing_wallet / _creating_wallet /
    //  _restoring_wallet and their descriptions are not in Lokalise yet.
    private fun SocialLoginStage.toProgressUM(provider: SocialLoginProvider): SocialLoginProgressUM? = when (this) {
        // the provider shows its own UI here, which an app-side progress state would only cover up
        SocialLoginStage.SigningIn -> null
        SocialLoginStage.VerifyingAccount -> SocialLoginProgressUM(
            title = stringReference("Verifying ${provider.displayName()} account"),
        )
        SocialLoginStage.PreparingWallet -> SocialLoginProgressUM(
            title = stringReference("Preparing your wallet"),
        )
        SocialLoginStage.CreatingWallet -> SocialLoginProgressUM(
            title = stringReference("Creating your wallet"),
            description = stringReference("Your keys are generated on this device"),
        )
        SocialLoginStage.RestoringWallet -> SocialLoginProgressUM(
            title = stringReference("Restoring your wallet"),
            description = stringReference("Your keys never leave this device unencrypted"),
        )
    }

    // TODO [REDACTED_TASK_KEY]: the social_login_error_* texts below are not in Lokalise yet.
    private fun SocialLoginError.toMessage(): TextReference = when (this) {
        SocialLoginError.NetworkError -> resourceReference(CoreResR.string.hw_cloud_backup_error_network)
        SocialLoginError.SessionExpired -> stringReference("Session expired. Please try again")
        is SocialLoginError.BackupWriteFailed ->
            stringReference("Couldn't save backup — make sure your cloud storage is available and try again")
        SocialLoginError.BindingConflict -> stringReference("This Google account is already linked to another wallet.")
        SocialLoginError.NoProviderAccount -> stringReference("Add a Google account to this device to continue.")
        SocialLoginError.ProviderUnavailable ->
            stringReference("Google sign-in is not available on this device.")
        else -> stringReference("Something went wrong. Please try again")
    }
}
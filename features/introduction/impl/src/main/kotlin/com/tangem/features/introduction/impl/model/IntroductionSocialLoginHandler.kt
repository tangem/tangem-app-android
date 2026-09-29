package com.tangem.features.introduction.impl.model

import com.tangem.common.TangemBlogUrlBuilder
import com.tangem.common.TangemBlogUrlBuilder.Post.WhatWalletToChoose
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import androidx.annotation.StringRes
import androidx.compose.ui.text.SpanStyle
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.plus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.styledResourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.EventMessageAction
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.usecase.GetAvailableSocialLoginProvidersUseCase
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
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
    private val urlOpener: UrlOpener,
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
            iconRes = CoreUiR.drawable.img_google_32,
            isIconTinted = false,
            title = resourceReference(
                id = CoreResR.string.introduction_create_wallet_social_title,
                formatArgs = wrappedList(provider.displayName()),
            ),
            description = resourceReference(CoreResR.string.introduction_create_wallet_social_description),
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
            onStageChanged = { stage -> host.onProgress(stage.toProgressUM()) },
            requestConsent = {
                host.onProgress(null)
                consentPrompt(provider, host::onConsent).await()
            },
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

    interface Host {

        fun onProgress(progress: SocialLoginProgressUM?)

        fun onConsent(consent: SocialLoginConsentUM?)

        fun onFinished()
    }

    private fun consentPrompt(
        provider: SocialLoginProvider,
        onConsent: (SocialLoginConsentUM?) -> Unit,
    ): SocialLoginConsentPrompt = SocialLoginConsentPrompt(
        title = resourceReference(
            id = CoreResR.string.social_login_consent_title,
            formatArgs = wrappedList(provider.displayName()),
        ),
        description = resourceReference(
            id = CoreResR.string.social_login_consent_description,
            formatArgs = wrappedList(provider.displayName()),
        ) + stringReference(" ") + styledResourceReference(
            id = CoreResR.string.social_login_consent_learn_more,
            spanStyleReference = { SpanStyle(color = TangemTheme.colors3.text.primary) },
            // TODO [REDACTED_TASK_KEY]: switch to the social login article once it is published.
            onClick = ::openHelpArticle,
        ),
        acknowledgement = providerReference(CoreResR.string.social_login_consent_acknowledge, provider),
        onStateChanged = onConsent,
    )

    private fun openHelpArticle() {
        appCoroutineScope.launch { urlOpener.openUrl(TangemBlogUrlBuilder.build(WhatWalletToChoose)) }
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
                when (outcome) {
                    is SocialLoginOutcome.WalletCreated ->
                        resourceReference(CoreResR.string.social_login_toast_wallet_created)
                    is SocialLoginOutcome.WalletRestored ->
                        resourceReference(CoreResR.string.social_login_toast_wallet_added)
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
            SocialLoginError.ConsentDeclined -> Unit
            SocialLoginError.BackupNotFound,
            SocialLoginError.InvalidBackupFile,
            is SocialLoginError.UnsupportedBackupVersion,
            is SocialLoginError.RecoveryKeyDecryptionFailed,
            SocialLoginError.PassphraseNotSupported,
            -> showRestoreFailedDialog(
                title = resourceReference(CoreResR.string.common_something_went_wrong),
                message = resourceReference(CoreResR.string.social_login_error_restore_failed_description),
            )
            SocialLoginError.CloudUnavailable -> showRestoreFailedDialog(
                title = resourceReference(CoreResR.string.social_login_error_cloud_access_title),
                message = resourceReference(
                    id = CoreResR.string.social_login_error_cloud_access_description,
                    formatArgs = wrappedList(provider.cloudName()),
                ),
            )
            else -> uiMessageSender.send(SnackbarMessage(error.toMessage(provider)))
        }
    }

    private fun showRestoreFailedDialog(title: TextReference, message: TextReference) {
        uiMessageSender.send(
            DialogMessage(
                title = title,
                message = message,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(CoreResR.string.social_login_error_support),
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

    private fun SocialLoginStage.toProgressUM(): SocialLoginProgressUM? = when (this) {
        // the provider shows its own UI here, which an app-side progress state would only cover up
        SocialLoginStage.SigningIn -> null
        SocialLoginStage.VerifyingAccount,
        SocialLoginStage.PreparingWallet,
        -> SocialLoginProgressUM.Loader
        SocialLoginStage.CreatingWallet -> SocialLoginProgressUM.Stage(
            title = resourceReference(CoreResR.string.social_login_creating_wallet),
            description = resourceReference(CoreResR.string.social_login_creating_wallet_description),
        )
        SocialLoginStage.RestoringWallet -> SocialLoginProgressUM.Stage(
            title = resourceReference(CoreResR.string.social_login_restoring_wallet),
            description = resourceReference(CoreResR.string.social_login_restoring_wallet_description),
        )
    }

    private fun SocialLoginError.toMessage(provider: SocialLoginProvider): TextReference = when (this) {
        SocialLoginError.NetworkError -> resourceReference(CoreResR.string.hw_cloud_backup_error_network)
        SocialLoginError.SessionExpired -> resourceReference(CoreResR.string.social_login_error_session_expired)
        is SocialLoginError.BackupWriteFailed -> resourceReference(CoreResR.string.social_login_error_backup_not_saved)
        SocialLoginError.BindingConflict -> providerReference(
            CoreResR.string.social_login_error_account_linked,
            provider,
        )
        SocialLoginError.NoProviderAccount -> providerReference(CoreResR.string.social_login_error_no_account, provider)
        SocialLoginError.ProviderUnavailable -> providerReference(
            CoreResR.string.social_login_error_unavailable,
            provider,
        )
        else -> resourceReference(CoreResR.string.social_login_error_generic)
    }

    private fun providerReference(@StringRes id: Int, provider: SocialLoginProvider): TextReference =
        resourceReference(id = id, formatArgs = wrappedList(provider.displayName()))
}
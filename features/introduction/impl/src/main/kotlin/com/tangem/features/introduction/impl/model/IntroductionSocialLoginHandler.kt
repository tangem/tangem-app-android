package com.tangem.features.introduction.impl.model

import com.tangem.common.routing.AppRoute
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.analytics.models.event.OnboardingAnalyticsEvent
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
import com.tangem.domain.feedback.models.WalletMetaInfo
import com.tangem.domain.sociallogin.models.SocialLoginError
import com.tangem.domain.sociallogin.models.SocialLoginProvider
import com.tangem.domain.sociallogin.usecase.GetAvailableSocialLoginProvidersUseCase
import com.tangem.features.hotwallet.SocialLoginOutcome
import com.tangem.features.hotwallet.SocialLoginStage
import com.tangem.features.hotwallet.SocialLoginUseCase
import com.tangem.features.introduction.impl.analytics.SocialLoginAnalyticsEvents
import com.tangem.features.introduction.impl.ui.state.SocialLoginConsentUM
import com.tangem.features.introduction.impl.ui.state.SocialLoginProgressUM
import com.tangem.features.introduction.impl.ui.state.WalletOptionUM
import com.tangem.utils.SupportedLanguages
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.UUID
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
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val referral: IntroductionReferral,
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
            icon = WalletOptionUM.Icon.Image(CoreUiR.drawable.img_google_32),
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
    fun start(
        scope: CoroutineScope,
        provider: SocialLoginProvider,
        source: AnalyticsParam.ScreensSources,
        host: Host,
    ) {
        scope.launch { signIn(provider, source, host) }
    }

    suspend fun signIn(provider: SocialLoginProvider, source: AnalyticsParam.ScreensSources, host: Host) {
        val attemptId = UUID.randomUUID().toString()
        TangemLogger.i("Social login attempt $attemptId started")

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
                ifLeft = { error -> onFailure(provider, error, attemptId) },
                ifRight = { outcome -> onSuccess(outcome, source) },
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
            onClick = ::openHelpArticle,
        ),
        acknowledgement = providerReference(CoreResR.string.social_login_consent_acknowledge, provider),
        onStateChanged = onConsent,
        onEvent = ::onConsentEvent,
    )

    private fun openHelpArticle() {
        val language = SupportedLanguages.getCurrentSupportedLanguageCode()
        urlOpener.openUrl("https://tangem.com/$language/$HELP_ARTICLE_PATH")
    }

    private fun onConsentEvent(event: SocialLoginConsentPrompt.Event) {
        analyticsEventHandler.send(
            when (event) {
                SocialLoginConsentPrompt.Event.Shown -> SocialLoginAnalyticsEvents.RiskConsentScreenOpened()
                SocialLoginConsentPrompt.Event.AcknowledgementMissing ->
                    SocialLoginAnalyticsEvents.RiskConsentNotConfirmed()
                is SocialLoginConsentPrompt.Event.Confirmed -> SocialLoginAnalyticsEvents.MarketingEmailsConsent(
                    isGiven = event.isEmailSharingAllowed,
                    // TODO [REDACTED_TASK_KEY]: the localized social_login_consent_email_sharing the viewer saw, once it exists
                    consentText = "Share my email with Tangem for product updates",
                )
            },
        )
    }

    private suspend fun onSuccess(outcome: SocialLoginOutcome, source: AnalyticsParam.ScreensSources) {
        // Before the selection: the wallet is saved either way, and a retry would come back as a restore.
        when (outcome) {
            is SocialLoginOutcome.WalletCreated ->
                sendWalletCreated(source, AnalyticsParam.WalletCreationType.NewSocialLogin)
            is SocialLoginOutcome.WalletRestored -> if (outcome.isNewOnDevice) {
                sendWalletCreated(source, AnalyticsParam.WalletCreationType.SocialLoginImport)
            }
        }

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

    private suspend fun sendWalletCreated(
        source: AnalyticsParam.ScreensSources,
        creationType: AnalyticsParam.WalletCreationType,
    ) {
        analyticsEventHandler.send(OnboardingAnalyticsEvent.Onboarding.Finished(source = source))
        analyticsEventHandler.send(
            OnboardingAnalyticsEvent.CreateWallet.WalletCreatedSuccessfully(
                source = source,
                creationType = creationType,
                seedPhraseLength = SEED_PHRASE_LENGTH,
                passPhraseState = AnalyticsParam.EmptyFull.Empty,
                referralId = referral.readId(),
            ),
        )
    }

    /**
     * Signs out first: the alternative flows offer a plain retry, and on Android the account chooser
     * only comes back for an account the app has not remembered.
     */
    private suspend fun onFailure(provider: SocialLoginProvider, error: SocialLoginError, attemptId: String) {
        socialLoginUseCase.signOut(provider)

        if (error != SocialLoginError.ConsentDeclined) {
            analyticsEventHandler.send(SocialLoginAnalyticsEvents.ErrorShown(errorMessage = error.toAnalyticsMessage()))
        }

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
                attemptId = attemptId,
            )
            SocialLoginError.CloudUnavailable -> showRestoreFailedDialog(
                title = resourceReference(CoreResR.string.social_login_error_cloud_access_title),
                message = resourceReference(
                    id = CoreResR.string.social_login_error_cloud_access_description,
                    formatArgs = wrappedList(provider.cloudName()),
                ),
                attemptId = attemptId,
            )
            else -> uiMessageSender.send(SnackbarMessage(error.toMessage(provider)))
        }
    }

    private fun showRestoreFailedDialog(title: TextReference, message: TextReference, attemptId: String) {
        uiMessageSender.send(
            DialogMessage(
                title = title,
                message = message,
                firstActionBuilder = {
                    EventMessageAction(
                        title = resourceReference(CoreResR.string.social_login_error_support),
                        onClick = { openSupportChat(attemptId) },
                    )
                },
                secondActionBuilder = { cancelAction(onClick = {}) },
            ),
        )
    }

    private fun openSupportChat(attemptId: String) {
        router.push(
            AppRoute.Usedesk(
                walletMetaInfo = WalletMetaInfo(userWalletId = null),
                source = AnalyticsParam.ScreensSources.SocialLogin,
                clientEmail = attemptId,
            ),
        )
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

    private fun SocialLoginError.toAnalyticsMessage(): String = when (this) {
        SocialLoginError.BackupNotFound,
        SocialLoginError.InvalidBackupFile,
        is SocialLoginError.UnsupportedBackupVersion,
        is SocialLoginError.RecoveryKeyDecryptionFailed,
        SocialLoginError.PassphraseNotSupported,
        -> "We couldn't restore the wallet for this account"
        SocialLoginError.CloudUnavailable -> "No access to the cloud storage"
        SocialLoginError.NetworkError -> "No internet connection"
        SocialLoginError.SessionExpired -> "Session expired"
        is SocialLoginError.BackupWriteFailed -> "Couldn't save backup"
        SocialLoginError.BindingConflict -> "Account is already linked to another wallet"
        SocialLoginError.NoProviderAccount -> "No account on the device"
        SocialLoginError.ProviderUnavailable -> "Sign-in is not available on this device"
        else -> "Something went wrong"
    }

    private companion object {
        const val SEED_PHRASE_LENGTH = 12
        const val HELP_ARTICLE_PATH = "help-center/getting-started-with-mobile-wallet/" +
            "how-to-create-a-mobile-wallet/#create-a-wallet-with-google-or-apple"
    }
}
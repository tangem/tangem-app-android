package com.tangem.features.survey.impl.nativesurvey.opener

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.GlobalUiMessageSender
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isLocked
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.features.survey.SurveyFeatureToggles
import com.tangem.features.survey.impl.nativesurvey.model.NativeSurvey
import com.tangem.features.survey.impl.nativesurvey.model.SurveyEntry
import com.tangem.features.survey.impl.nativesurvey.model.toSurveySource
import com.tangem.features.survey.impl.nativesurvey.repository.NativeSurveyRepository
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** The single way into a native survey (§7.1 of the spec), whatever opened it. */
@Singleton
internal class NativeSurveyOpener @Inject constructor(
    private val repository: NativeSurveyRepository,
    private val surveyFeatureToggles: SurveyFeatureToggles,
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
    @GlobalUiMessageSender private val uiMessageSender: UiMessageSender,
) {

    val isAvailable: Boolean
        get() = repository.isAvailable && NativeSurvey.Type.entries.any(::isEnabled)

    suspend fun open(request: Request, router: Router) {
        val userWallet = selectedUnlockedWallet()
        if (userWallet == null) {
            TangemLogger.i("Native survey: no unlocked wallet to open survey ${request.surveyId} for")
            return
        }

        val screenBeforeLoading = router.topRoute()
        val outcome = prepare(request, userWallet)
        if (selectedUnlockedWallet()?.walletId != userWallet.walletId) {
            TangemLogger.i("Native survey: the wallet changed while survey ${request.surveyId} was loading")
            return
        }

        when (outcome) {
            is Outcome.Show -> router.navigate(
                transformer = { stack ->
                    if (stack.canShowSurveyAfter(screenBeforeLoading)) stack + outcome.route else stack
                },
            )
            Outcome.Failed -> if (router.topRoute() == screenBeforeLoading) {
                uiMessageSender.send(
                    DialogMessage(
                        title = resourceReference(R.string.common_something_went_wrong),
                        message = resourceReference(R.string.common_try_again_later),
                    ),
                )
            }
            Outcome.Disabled -> TangemLogger.i("Native survey: the type of survey ${request.surveyId} is disabled")
        }
    }

    private suspend fun prepare(request: Request, userWallet: UserWallet): Outcome {
        val entry = SurveyEntry(
            surveyId = request.surveyId,
            source = request.source.toSurveySource(),
            stage = request.stage,
            repeatAfterDays = request.repeatAfterDays,
            userWallet = userWallet,
        )
        if (repository.isAnswered(entry)) {
            return Outcome.Show(request.toRoute(userWallet, isAlreadyAnswered = true, isFromCache = true))
        }

        val loaded = repository.load(entry) ?: return Outcome.Failed
        if (!isEnabled(loaded.survey.type)) return Outcome.Disabled

        return Outcome.Show(request.toRoute(userWallet, isAlreadyAnswered = false, isFromCache = loaded.isFromCache))
    }

    /**
     * A survey already open is not shown twice, and a lock or another deep link that changed the screen while loading
     * means the survey is no longer wanted.
     */
    private fun List<Route>.canShowSurveyAfter(screenBeforeLoading: Route?): Boolean =
        lastOrNull() == screenBeforeLoading && none { route -> route is AppRoute.NativeSurvey }

    private fun selectedUnlockedWallet(): UserWallet? =
        getSelectedWalletSyncUseCase().getOrNull()?.takeUnless(UserWallet::isLocked)

    /** Decompose defers a navigation started while another one is running, e.g. a deep link dispatched on unlock. */
    private suspend fun Router.topRoute(): Route? = withContext(dispatchers.mainImmediate) {
        suspendCancellableCoroutine { continuation ->
            var top: Route? = null
            navigate(
                transformer = { stack -> stack.also { top = it.lastOrNull() } },
                onComplete = { continuation.resume(top) },
            )
        }
    }

    private fun isEnabled(type: NativeSurvey.Type): Boolean = when (type) {
        NativeSurvey.Type.NPS -> surveyFeatureToggles.isNpsSurveyEnabled
    }

    private fun Request.toRoute(userWallet: UserWallet, isAlreadyAnswered: Boolean, isFromCache: Boolean) =
        AppRoute.NativeSurvey(
            surveyId = surveyId,
            source = source,
            stage = stage,
            repeatAfterDays = repeatAfterDays,
            userWalletId = userWallet.walletId,
            isAlreadyAnswered = isAlreadyAnswered,
            isFromCache = isFromCache,
        )

    private sealed interface Outcome {
        data class Show(val route: AppRoute.NativeSurvey) : Outcome
        data object Failed : Outcome
        data object Disabled : Outcome
    }

    data class Request(
        val surveyId: Long,
        val source: AppRoute.NativeSurvey.Source,
        val stage: String,
        val repeatAfterDays: Int?,
    )
}
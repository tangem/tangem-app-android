package com.tangem.features.collectibles.impl.onboarding.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.message.DialogMessage
import arrow.core.Either
import arrow.core.left
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.collectibles.usecase.CreateOrReactivateCollectiblesAccountUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.getSyncOrNull
import com.tangem.features.collectibles.impl.CollectiblesRoute
import com.tangem.features.collectibles.impl.R
import com.tangem.features.collectibles.impl.onboarding.CollectiblesOnboardingComponent
import com.tangem.features.collectibles.impl.onboarding.model.converter.CollectiblesOnboardingUMConverter
import com.tangem.features.collectibles.impl.onboarding.ui.state.CollectiblesOnboardingUM
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class CollectiblesOnboardingModel @Inject constructor(
    paramsContainer: ParamsContainer,
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val messageSender: UiMessageSender,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val createOrReactivateCollectiblesAccountUseCase: CreateOrReactivateCollectiblesAccountUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    private val params = paramsContainer.require<CollectiblesOnboardingComponent.Params>()

    val uiState: StateFlow<CollectiblesOnboardingUM>
        field = MutableStateFlow(
            CollectiblesOnboardingUMConverter().convert(
                value = CollectiblesOnboardingUMConverter.Callbacks(
                    onCloseClick = ::onCloseClick,
                    onCreateAccountClick = ::onCreateAccountClick,
                    onCollectiblesTermsClick = ::onCollectiblesTermsClick,
                    onTangemTermsClick = ::onTangemTermsClick,
                    onPrivacyNoticeClick = ::onPrivacyNoticeClick,
                ),
            ),
        )

    init {
        preloadStory()
    }

    @Suppress("EmptyFunctionBlock")
    private fun preloadStory() {
        // TODO([REDACTED_TASK_KEY]): nothing to warm while the slides are local — see the TODO in
        //  CollectiblesStoriesUMConverter. With a backend story, fetch it here with `refresh = true` and preload
        //  its images, the way YieldBoostStoryPreloader does.
    }

    @Suppress("FunctionOnlyReturningConstant")
    private fun isStoryReady(): Boolean {
        // TODO([REDACTED_TASK_KEY]): local slides are always there. With a backend story this asks
        //  ShouldShowStoriesInteractor / GetStoryContentUseCase and starts returning false — the case
        //  onCreateAccountClick already handles.
        return true
    }

    private fun onCloseClick() {
        router.pop()
    }

    private fun onCreateAccountClick() {
        TangemLogger.d("onboarding: Create Packs account button tapped, walletId=${params.userWalletId}")

        if (uiState.value.isCreatingAccount) {
            TangemLogger.d("onboarding: creation is already in progress, the repeated tap is ignored")
            return
        }

        // Raised before the launch: two taps within a frame would otherwise both get past the guard
        uiState.update { it.copy(isCreatingAccount = true) }

        modelScope.launch {
            prepareAccount().fold(
                ifLeft = ::onPrepareFailed,
                ifRight = { onPrepared() },
            )

            uiState.update { it.copy(isCreatingAccount = false) }
        }
    }

    private suspend fun prepareAccount(): Either<CollectiblesAccountError, Unit> {
        val userWallet = userWalletsListRepository.getSyncOrNull(params.userWalletId)
            ?: run {
                TangemLogger.d("onboarding: wallet not found -> Unsupported")
                return CollectiblesAccountError.Unsupported.left()
            }

        // TODO([REDACTED_TASK_KEY]): also check the geo block and whether the user is allowed to create a gacha account
        return createOrReactivateCollectiblesAccountUseCase(userWallet)
    }

    private fun onPrepared() {
        TangemLogger.d("onboarding: the gacha account is ready to use, going to Main")

        val main = CollectiblesRoute.Main(userWalletId = params.userWalletId)

        if (isStoryReady()) {
            router.replaceAll(main, CollectiblesRoute.Stories)
        } else {
            router.replaceAll(main)
        }
    }

    private fun onPrepareFailed(error: CollectiblesAccountError) {
        TangemLogger.d("onboarding: account creation failed: $error")

        // A card refusal is already on the NFC sheet, and a cancellation is the user's own choice: neither needs a dialog
        if (error is CollectiblesAccountError.CardError || error is CollectiblesAccountError.Cancelled) return

        TangemLogger.e("Failed to create the collectibles account: $error")

        messageSender.send(
            DialogMessage(
                title = TextReference.Res(R.string.common_something_went_wrong),
                message = TextReference.Res(R.string.collectibles_welcome_creation_failed_message),
            ),
        )
    }

    private fun onCollectiblesTermsClick() {
        // No-op: the service has not supplied its terms URL yet.
    }

    private fun onTangemTermsClick() {
        urlOpener.openUrl(TANGEM_TERMS_URL)
    }

    private fun onPrivacyNoticeClick() {
        urlOpener.openUrl(PRIVACY_NOTICE_URL)
    }

    private companion object {

        const val TANGEM_TERMS_URL = "https://tangem.com/tangem_tos.html"
        const val PRIVACY_NOTICE_URL = "https://tangem.com/privacy-policy.html"
    }
}
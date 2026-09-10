package com.tangem.features.collectibles.impl.onboarding.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.message.DialogMessage
import arrow.core.Either
import arrow.core.left
import com.tangem.domain.collectibles.usecase.CreateOrReactivateCollectiblesAccountUseCase
import com.tangem.domain.collectibles.CollectiblesAccountError
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.features.collectibles.impl.CollectiblesRoute
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
internal class CollectiblesOnboardingModel @Inject constructor(
    private val router: Router,
    private val urlOpener: UrlOpener,
    private val messageSender: UiMessageSender,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val createOrReactivateCollectiblesAccountUseCase: CreateOrReactivateCollectiblesAccountUseCase,
    override val dispatchers: CoroutineDispatcherProvider,
) : Model() {

    val uiState: StateFlow<CollectiblesOnboardingUM>
        field = MutableStateFlow(
            CollectiblesOnboardingUMConverter().convert(
                value = CollectiblesOnboardingUMConverter.Callbacks(
                    onCloseClick = ::onCloseClick,
                    onCreateAccountClick = ::onCreateAccountClick,
                    onCollectiblesTermsClick = ::onCollectiblesTermsClick,
                    onTangemTermsClick = ::onTangemTermsClick,
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
        if (uiState.value.isCreatingAccount) return

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
        val userWallet = userWalletsListRepository.selectedUserWalletSync()

        // TODO([REDACTED_TASK_KEY]): also check the geo block and whether the user is allowed to create a gacha account
        if (userWallet == null) {
            return CollectiblesAccountError.Unsupported.left()
        }

        return createOrReactivateCollectiblesAccountUseCase(userWallet)
    }

    private fun onPrepared() {
        if (isStoryReady()) {
            router.replaceAll(CollectiblesRoute.Main, CollectiblesRoute.Stories)
        } else {
            router.replaceAll(CollectiblesRoute.Main)
        }
    }

    private fun onPrepareFailed(error: CollectiblesAccountError) {
        // A card refusal is already on the NFC sheet, and a cancellation is the user's own choice: neither needs a dialog
        if (error is CollectiblesAccountError.CardError || error is CollectiblesAccountError.Cancelled) return

        TangemLogger.e("Failed to create the collectibles account: $error")

        // TODO([REDACTED_TASK_KEY]): hardcoded like the rest of this feature's copy — move to Lokalise with it.
        messageSender.send(
            DialogMessage(
                title = stringReference("Couldn't create the Packs account"),
                message = stringReference("Please try again later"),
            ),
        )
    }

    private fun onCollectiblesTermsClick() {
        // No-op: the service has not supplied its terms URL yet.
    }

    private fun onTangemTermsClick() {
        urlOpener.openUrl(TANGEM_TERMS_URL)
    }

    private companion object {

        const val TANGEM_TERMS_URL = "https://tangem.com/tangem_tos.html"
    }
}
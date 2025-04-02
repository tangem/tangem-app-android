package com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.model

import androidx.compose.runtime.Stable
import com.tangem.common.CompletionResult
import com.tangem.common.core.TangemSdkError
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.feedback.GetCardInfoUseCase
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.wallets.builder.UserWalletBuilder
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.features.onboarding.v2.multiwallet.impl.child.createwallet.ui.state.MultiWalletCreateWalletUM
import com.tangem.features.onboarding.v2.multiwallet.impl.common.ui.resetCardDialog
import com.tangem.features.onboarding.v2.multiwallet.impl.model.OnboardingMultiWalletState.Step
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Suppress("LongParameterList")
@Stable
@ModelScoped
internal class MultiWalletCreateWalletModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val tangemSdkManager: TangemSdkManager,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
    private val getCardInfoUseCase: GetCardInfoUseCase,
    private val cardRepository: CardRepository,
    private val analyticsHandler: AnalyticsEventHandler,
    private val userWalletBuilderFactory: UserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val multiWalletState
        get() = params.multiWalletState

    private val _uiState = MutableStateFlow(
        MultiWalletCreateWalletUM(
            title = if (params.parentParams.withSeedPhraseFlow) {
                resourceReference(R.string.onboarding_create_wallet_options_title)
            } else {
                resourceReference(R.string.onboarding_create_wallet_header)
            },
            bodyText = if (params.parentParams.withSeedPhraseFlow) {
                resourceReference(R.string.onboarding_create_wallet_options_message)
            } else {
                resourceReference(R.string.onboarding_create_wallet_body)
            },
            onCreateWalletClick = {
                analyticsHandler.send(OnboardingEvent.CreateWallet.ButtonCreateWallet)
                createWallet(false)
            },
            showOtherOptionsButton = params.parentParams.withSeedPhraseFlow,
            onOtherOptionsClick = {
                modelScope.launch {
                    onDone.emit(Step.SeedPhrase)
                }
            },
            dialog = null,
        ),
    )

    val uiState: StateFlow<MultiWalletCreateWalletUM> = _uiState
    val onDone = MutableSharedFlow<Step>()

    init {
        analyticsHandler.send(OnboardingEvent.CreateWallet.ScreenOpened)
    }

    private fun createWallet(shouldReset: Boolean) {
        modelScope.launch {
            val result = tangemSdkManager.createProductWallet(
                scanResponse = multiWalletState.value.currentScanResponse,
                shouldReset = shouldReset,
            )

            when (result) {
                is CompletionResult.Success -> {
                    multiWalletState.update {
                        it.copy(
                            currentScanResponse = it.currentScanResponse.copy(
                                card = result.data.card,
                                derivedKeys = result.data.derivedKeys,
                                primaryCard = result.data.primaryCard,
                            ),
                        )
                    }

                    cardRepository.startCardActivation(cardId = result.data.card.cardId)

                    analyticsHandler.send(OnboardingEvent.CreateWallet.WalletCreatedSuccessfully())

                    val cardDoesNotSupportBackup = result.data.card.settings.isBackupAllowed.not()
                    when {
                        cardDoesNotSupportBackup -> createWalletAndNavigateBackWithDone()
                        params.parentParams.withSeedPhraseFlow -> onDone.emit(Step.AddBackupDevice)
                        else -> onDone.emit(Step.ChooseBackupOption)
                    }
                }

                is CompletionResult.Failure -> {
                    if (result.error is TangemSdkError.WalletAlreadyCreated) {
                        // show should reset dialog
                        handleActivationError()
                    }
                }
            }
        }
    }

    private fun createWalletAndNavigateBackWithDone() {
        modelScope.launch {
            val scanResponse = params.multiWalletState.value.currentScanResponse

            val userWallet = createUserWallet(scanResponse)
            saveWalletUseCase(userWallet, canOverride = true)
                .onRight {
                    cardRepository.finishCardActivation(scanResponse.card.cardId)

                    // save user wallet for manage tokens screen
                    params.multiWalletState.update {
                        it.copy(resultUserWallet = userWallet)
                    }

                    onDone.emit(Step.Done)
                }
        }
    }

    private suspend fun createUserWallet(scanResponse: ScanResponse): UserWallet {
        return requireNotNull(
            value = userWalletBuilderFactory.create(scanResponse = scanResponse).build(),
            lazyMessage = { "User wallet not created" },
        )
    }

    private fun handleActivationError() {
        _uiState.update { state ->
            state.copy(
                dialog = resetCardDialog(
                    onConfirm = ::navigateToSupportScreen,
                    dismiss = { _uiState.update { it.copy(dialog = null) } },
                    onDismissButtonClick = ::resetCard,
                ),
            )
        }
    }

    private fun resetCard() {
        createWallet(true)
    }

    fun navigateToSupportScreen() {
        modelScope.launch {
            val cardInfo = getCardInfoUseCase(multiWalletState.value.currentScanResponse).getOrNull() ?: return@launch
            sendFeedbackEmailUseCase(FeedbackEmailType.DirectUserRequest(cardInfo))
        }
    }
}
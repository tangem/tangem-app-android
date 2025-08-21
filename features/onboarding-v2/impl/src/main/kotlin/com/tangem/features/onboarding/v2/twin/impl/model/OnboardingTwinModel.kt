package com.tangem.features.onboarding.v2.twin.impl.model

import com.tangem.Message
import com.tangem.common.CompletionResult
import com.tangem.common.KeyPair
import com.tangem.common.core.TangemError
import com.tangem.common.core.TangemSdkError
import com.tangem.common.extensions.hexToBytes
import com.tangem.common.extensions.toHexString
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.AnalyticsParam
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.toWrappedList
import com.tangem.datasource.local.config.issuers.IssuersConfigStorage
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.common.TwinCardNumber
import com.tangem.domain.common.getTwinCardNumber
import com.tangem.domain.feedback.SendFeedbackEmailUseCase
import com.tangem.domain.feedback.models.FeedbackEmailType
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.onboarding.SaveTwinsOnboardingShownUseCase
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.builder.UserWalletIdBuilder
import com.tangem.domain.wallets.usecase.DeleteWalletUseCase
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.features.onboarding.v2.common.analytics.OnboardingEvent
import com.tangem.features.onboarding.v2.common.ui.interruptBackupDialog
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.api.OnboardingTwinComponent.Params.Mode
import com.tangem.features.onboarding.v2.twin.impl.DefaultOnboardingTwinComponent
import com.tangem.features.onboarding.v2.twin.impl.ui.TwinWalletArtworkUM
import com.tangem.features.onboarding.v2.twin.impl.ui.state.OnboardingTwinUM
import com.tangem.sdk.api.TangemSdkManager
import com.tangem.sdk.extensions.localizedDescriptionRes
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class OnboardingTwinModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val deleteWalletUseCase: DeleteWalletUseCase,
    private val analyticsEventHandler: AnalyticsEventHandler,
    private val saveTwinsOnboardingShownUseCase: SaveTwinsOnboardingShownUseCase,
    private val tangemSdkManager: TangemSdkManager,
    private val issuersConfigStorage: IssuersConfigStorage,
    private val cardRepository: CardRepository,
    private val uiMessageSender: UiMessageSender,
    private val sendFeedbackEmailUseCase: SendFeedbackEmailUseCase,
) : Model() {

    private val params = paramsContainer.require<OnboardingTwinComponent.Params>()
    private val firstCardTwinNumber = params.scanResponse.card.getTwinCardNumber() ?: error("Not twin")
    private val cryptoCurrencyStatusJobHolder = JobHolder()

    private val _uiState = MutableStateFlow(
        when (params.mode) {
            Mode.WelcomeOnly -> {
                OnboardingTwinUM.Welcome(
                    pairCardNumber = firstCardTwinNumber.pairNumber().number,
                    onContinueClick = ::saveWalletAndDone,
                )
            }
            Mode.RecreateWallet -> {
                OnboardingTwinUM.ResetWarning(
                    onAcceptClick = { toggle ->
                        update<OnboardingTwinUM.ResetWarning> { it.copy(acceptToggle = toggle) }
                    },
                    onContinueClick = {
                        navigateToFirstScan()
                    },
                )
            }
            Mode.CreateWallet -> {
                OnboardingTwinUM.Welcome(
                    pairCardNumber = firstCardTwinNumber.pairNumber().number,
                    onContinueClick = ::navigateToFirstScan,
                )
            }
        },
    )

    val uiState = _uiState.asStateFlow()
    val innerNavigationState = MutableStateFlow(DefaultOnboardingTwinComponent.TwinInnerNavigationState(1))

    init {
        when (_uiState.value) {
            is OnboardingTwinUM.Welcome -> {
                analyticsEventHandler.send(OnboardingEvent.Twins.ScreenOpened)
                modelScope.launch {
                    saveTwinsOnboardingShownUseCase()
                }
            }
            else -> {}
        }
    }

    fun onBack() {
        val state = _uiState.value
        val twinningInProgress = state is OnboardingTwinUM.ScanCard &&
            (state.step == OnboardingTwinUM.ScanCard.Step.Second || state.step == OnboardingTwinUM.ScanCard.Step.Third)

        if (twinningInProgress) {
            uiMessageSender.send(TwinProcessNotCompleted)
        } else {
            uiMessageSender.send(
                interruptBackupDialog(
                    onConfirm = { params.modelCallbacks.onBack() },
                ),
            )
        }
    }

    private fun navigateToFirstScan() {
        _uiState.value = OnboardingTwinUM.ScanCard(
            artworkStep = when (firstCardTwinNumber) {
                TwinCardNumber.First -> TwinWalletArtworkUM.Leapfrog.Step.FirstCard
                TwinCardNumber.Second -> TwinWalletArtworkUM.Leapfrog.Step.SecondCard
            },
            step = OnboardingTwinUM.ScanCard.Step.First,
            onScanClick = { createFirstWallet() },
        )
    }

    private fun createFirstWallet() {
        modelScope.launch {
            setLoading(true)

            val result = tangemSdkManager.createFirstTwinWallet(
                cardId = params.scanResponse.card.cardId,
                initialMessage = Message(
                    tangemSdkManager.getString(
                        R.string.twins_recreate_title_format,
                        firstCardTwinNumber.number,
                    ),
                ),
            )

            when (result) {
                is CompletionResult.Failure -> {
                    setLoading(false)
                    showCardVerificationFailedDialog(result.error)
                }
                is CompletionResult.Success -> {
                    if (params.mode == Mode.CreateWallet) {
                        cardRepository.startCardActivation(result.data.cardId)
                    }

                    // remove wallet only after first step of retwin
                    if (params.mode == Mode.RecreateWallet) {
                        UserWalletIdBuilder.scanResponse(params.scanResponse).build()?.let {
                            deleteWalletUseCase(it)
                        }
                    }

                    analyticsEventHandler.send(OnboardingEvent.CreateWallet.WalletCreatedSuccessfully())

                    update<OnboardingTwinUM.ScanCard> {
                        it.copy(
                            isLoading = false,
                            artworkStep = it.artworkStep.next(),
                            step = OnboardingTwinUM.ScanCard.Step.Second,
                            onScanClick = {
                                createSecondWallet(firstPublicKey = result.data.wallet.publicKey.toHexString())
                            },
                        )
                    }
                    innerNavigationState.update { it.copy(stackSize = 2) }
                }
            }
        }
    }

    private fun createSecondWallet(firstPublicKey: String) {
        setLoading(true)
        modelScope.launch {
            val secondCardNumber = firstCardTwinNumber.pairNumber().number
            val result = tangemSdkManager.createSecondTwinWallet(
                firstPublicKey = firstPublicKey,
                firstCardId = params.scanResponse.card.cardId,
                issuerKeys = getIssuerKeys(),
                preparingMessage = Message(
                    tangemSdkManager.getString(R.string.twins_recreate_title_preparing),
                ),
                creatingWalletMessage = Message(
                    tangemSdkManager.getString(R.string.twins_recreate_title_creating_wallet),
                ),
                initialMessage = Message(
                    tangemSdkManager.getString(
                        R.string.twins_recreate_title_format,
                        secondCardNumber,
                    ),
                ),
            )

            when (result) {
                is CompletionResult.Failure -> {
                    setLoading(false)
                    showCardVerificationFailedDialog(result.error)
                }
                is CompletionResult.Success -> {
                    if (params.mode == Mode.CreateWallet) {
                        cardRepository.startCardActivation(result.data.cardId)
                    }

                    update<OnboardingTwinUM.ScanCard> {
                        it.copy(
                            isLoading = false,
                            artworkStep = it.artworkStep.next(),
                            step = OnboardingTwinUM.ScanCard.Step.Third,
                            onScanClick = {
                                createThirdWallet(
                                    secondCardPublicKey = result.data.wallet.publicKey,
                                )
                            },
                        )
                    }

                    innerNavigationState.update {
                        it.copy(stackSize = 3)
                    }
                }
            }
        }
    }

    private fun createThirdWallet(secondCardPublicKey: ByteArray) {
        modelScope.launch {
            setLoading(true)

            val result = tangemSdkManager.finalizeTwin(
                secondCardPublicKey = secondCardPublicKey,
                issuerKeyPair = getIssuerKeys(),
                cardId = params.scanResponse.card.cardId,
                initialMessage = Message(
                    tangemSdkManager.getString(
                        R.string.twins_recreate_title_format,
                        firstCardTwinNumber.number,
                    ),
                ),
            )

            when (result) {
                is CompletionResult.Failure -> {
                    setLoading(false)
                    showCardVerificationFailedDialog(result.error)
                }
                is CompletionResult.Success -> {
                    scanComplete(result.data)
                }
            }
        }
    }

    private fun scanComplete(scanResponse: ScanResponse) {
        analyticsEventHandler.send(OnboardingEvent.Twins.SetupFinished)

        when (params.mode) {
            Mode.WelcomeOnly -> saveWalletAndDone()
            Mode.RecreateWallet -> params.modelCallbacks.onDone()
            Mode.CreateWallet -> {
                modelScope.launch {
                    setLoading(true)
                    finishActivation(scanResponse)
                }.saveIn(cryptoCurrencyStatusJobHolder)
            }
        }
    }

    private suspend fun finishActivation(scanResponse: ScanResponse) = coroutineScope {
        val userWallet = coldUserWalletBuilderFactory.create(scanResponse).build() ?: run {
            Timber.e("User wallet not created")
            setLoading(false)
            return@coroutineScope
        }

        saveWalletUseCase(
            userWallet = userWallet,
            canOverride = true,
        ).onLeft {
            Timber.e("Unable to save user wallet: $it")
            setLoading(false)
            return@coroutineScope
        }

        cardRepository.finishCardActivation(params.scanResponse.card.cardId)

        params.modelCallbacks.onDone()
    }

    private fun saveWalletAndDone() {
        setLoading(true)

        modelScope.launch {
            val userWallet = coldUserWalletBuilderFactory.create(params.scanResponse).build() ?: run {
                Timber.e("User wallet not created")
                setLoading(false)
                return@launch
            }

            saveWalletUseCase(
                userWallet = userWallet,
                canOverride = true,
            ).onLeft {
                Timber.e("Unable to save user wallet: $it")
                setLoading(false)
                return@launch
            }

            params.modelCallbacks.onDone()
        }
    }

    private fun showCardVerificationFailedDialog(error: TangemError) {
        if (error !is TangemSdkError.CardVerificationFailed) return

        analyticsEventHandler.send(OnboardingEvent.OfflineAttestationFailed(AnalyticsParam.ScreensSources.Onboarding))

        val resource = error.localizedDescriptionRes()
        val resId = resource.resId ?: R.string.common_unknown_error
        val resArgs = resource.args.map { it.value }

        uiMessageSender.send(
            cardVerificationFailedDialog(
                errorDescription = resourceReference(id = resId, resArgs.toWrappedList()),
                onRequestSupport = {
                    modelScope.launch {
                        sendFeedbackEmailUseCase(type = FeedbackEmailType.CardAttestationFailed)
                    }
                },
            ),
        )
    }

    private suspend fun getIssuerKeys(): KeyPair {
        val issuer = issuersConfigStorage.getConfig()
            .first { it.publicKey == params.scanResponse.card.issuer.publicKey.toHexString() }

        return KeyPair(
            publicKey = issuer.publicKey.hexToBytes(),
            privateKey = issuer.privateKey.hexToBytes(),
        )
    }

    private inline fun <reified T : OnboardingTwinUM> update(updateBlock: (T) -> OnboardingTwinUM) {
        _uiState.update { st ->
            (st as? T)?.let { updateBlock(it) } ?: st
        }
    }

    private fun setLoading(flag: Boolean) {
        _uiState.update { it.copySealed(isLoading = flag) }
    }

    private fun TwinWalletArtworkUM.Leapfrog.Step.next(): TwinWalletArtworkUM.Leapfrog.Step = when (this) {
        TwinWalletArtworkUM.Leapfrog.Step.FirstCard -> TwinWalletArtworkUM.Leapfrog.Step.SecondCard
        TwinWalletArtworkUM.Leapfrog.Step.SecondCard -> TwinWalletArtworkUM.Leapfrog.Step.FirstCard
    }
}
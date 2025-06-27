package com.tangem.features.onboarding.v2.note.impl.model

import com.arkivanov.decompose.router.stack.StackNavigation
import com.tangem.common.ui.userwallet.converter.ArtworkUMConverter
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.features.onboarding.v2.common.ui.exitOnboardingDialog
import com.tangem.features.onboarding.v2.note.api.OnboardingNoteComponent
import com.tangem.features.onboarding.v2.note.impl.OnboardingNoteInnerNavigationState
import com.tangem.features.onboarding.v2.note.impl.route.OnboardingNoteRoute
import com.tangem.features.onboarding.v2.note.impl.route.stepNum
import com.tangem.operations.attestation.ArtworkSize
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@ModelScoped
internal class OnboardingNoteModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val messageSender: UiMessageSender,
    private val getCardImageUseCase: GetCardImageUseCase,
    private val artworkUMConverter: ArtworkUMConverter,
) : Model() {

    @Suppress("UnusedPrivateMember")
    private val params = paramsContainer.require<OnboardingNoteComponent.Params>()

    val initialRoute = initializeRoute()

    private val _innerNavigationState =
        MutableStateFlow(OnboardingNoteInnerNavigationState(stackSize = initialRoute.stepNum()))

    val innerNavigationState = _innerNavigationState.asStateFlow()

    val stackNavigation = StackNavigation<OnboardingNoteRoute>()

    private val _uiState = MutableStateFlow(
        OnboardingNoteCommonState(
            scanResponse = params.scanResponse,
        ),
    )

    val commonUiState: MutableStateFlow<OnboardingNoteCommonState> = _uiState

    init {
        loadArtwork()
    }

    fun updateStepForNewRoute(route: OnboardingNoteRoute) {
        _innerNavigationState.value = innerNavigationState.value.copy(
            stackSize = route.stepNum(),
        )
    }

    @Suppress("UnusedPrivateMember")
    fun onChildBack(currentRoute: OnboardingNoteRoute) {
        messageSender.send(
            exitOnboardingDialog(
                onConfirm = {
                    router.pop()
                },
            ),
        )
    }

    fun onWalletCreated(userWallet: UserWallet) {
        commonUiState.update {
            it.copy(userWallet = userWallet)
        }
    }

    private fun initializeRoute(): OnboardingNoteRoute {
        val card = params.scanResponse.card
        return if (card.wallets.isEmpty()) {
            OnboardingNoteRoute.CreateWallet
        } else {
            OnboardingNoteRoute.TopUp
        }
    }

    private fun loadArtwork() {
        modelScope.launch {
            val cardInfo = params.scanResponse.card
            val artwork = getCardImageUseCase.invoke(
                cardId = cardInfo.cardId,
                cardPublicKey = cardInfo.cardPublicKey,
                size = ArtworkSize.LARGE,
                manufacturerName = cardInfo.manufacturer.name,
                firmwareVersion = cardInfo.firmwareVersion.toSdkFirmwareVersion(),
            )
            _uiState.update {
                it.copy(cardArtwork = artworkUMConverter.convert(artwork))
            }
        }
    }
}
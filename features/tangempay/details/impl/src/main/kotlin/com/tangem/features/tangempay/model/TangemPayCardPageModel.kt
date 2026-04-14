package com.tangem.features.tangempay.model

import androidx.compose.runtime.Stable
import com.arkivanov.decompose.router.slot.SlotNavigation
import com.arkivanov.decompose.router.slot.activate
import com.arkivanov.decompose.router.slot.dismiss
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.features.tangempay.components.TangemPayCardPageComponent
import com.tangem.features.tangempay.components.ViewPinListener
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.AddToWalletBlockState
import com.tangem.features.tangempay.entity.TangemPayCardPageSetting
import com.tangem.features.tangempay.entity.TangemPayCardPageUM
import com.tangem.features.tangempay.entity.TangemPayDetailsNavigation
import com.tangem.features.tangempay.navigation.TangemPayDetailsInnerRoute
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.JobHolder
import com.tangem.utils.coroutines.saveIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class TangemPayCardPageModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val uiMessageSender: UiMessageSender,
) : Model(), ViewPinListener {

    private val params: TangemPayCardPageComponent.Params = paramsContainer.require()

    private var currentFrozenState: TangemPayCardFrozenState = params.config.cardFrozenState

    private val addToWalletBannerJobHolder = JobHolder()

    val uiState: StateFlow<TangemPayCardPageUM>
        field = MutableStateFlow(
            TangemPayCardPageUM(
                onBackClick = router::pop,
                onSettingClick = ::onSettingClick,
            ),
        )

    val bottomSheetNavigation: SlotNavigation<TangemPayDetailsNavigation> = SlotNavigation()

    init {
        fetchAddToWalletBanner()
        subscribeToCardFrozenState()
    }

    private fun onSettingClick(setting: TangemPayCardPageSetting) = when (setting) {
        TangemPayCardPageSetting.ChangePIN -> onClickChangePIN()
        TangemPayCardPageSetting.FreezeCard -> onClickFreezeOrUnfreezeCard()
        TangemPayCardPageSetting.ReplaceCard -> Unit // TODO v_rodionov #[REDACTED_TASK_KEY]
    }

    private fun onClickChangePIN() {
        if (!params.config.isPinSet) {
            router.push(TangemPayDetailsInnerRoute.ChangePIN)
        } else {
            bottomSheetNavigation.activate(
                TangemPayDetailsNavigation.ViewPinCode(
                    userWalletId = params.userWalletId,
                    cardId = params.config.cardId,
                ),
            )
        }
    }

    private fun onClickFreezeOrUnfreezeCard() {
        when (currentFrozenState) {
            TangemPayCardFrozenState.Frozen -> uiMessageSender.send(
                TangemPayMessagesFactory.createUnfreezeCardMessage(onUnfreezeClicked = ::unfreezeCard),
            )
            else -> uiMessageSender.send(
                TangemPayMessagesFactory.createFreezeCardMessage(onFreezeClicked = ::freezeCard),
            )
        }
    }

    private fun freezeCard() {
        modelScope.launch {
            cardDetailsRepository.freezeCard(
                userWalletId = params.userWalletId,
                cardId = params.config.cardId,
            ).onLeft {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed))
                uiMessageSender.send(message)
            }.onRight { state ->
                val message = if (state == TangemPayCardFrozenState.Frozen) {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_success))
                } else {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_freeze_card_failed))
                }
                uiMessageSender.send(message)
            }
        }
    }

    private fun unfreezeCard() {
        modelScope.launch {
            cardDetailsRepository.unfreezeCard(
                userWalletId = params.userWalletId,
                cardId = params.config.cardId,
            ).onLeft {
                val message = SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed))
                uiMessageSender.send(message)
            }.onRight { state ->
                val message = if (state == TangemPayCardFrozenState.Unfrozen) {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_success))
                } else {
                    SnackbarMessage(resourceReference(R.string.tangem_pay_unfreeze_card_failed))
                }
                uiMessageSender.send(message)
            }
        }
    }

    private fun subscribeToCardFrozenState() {
        cardDetailsRepository
            .cardFrozenState(params.config.cardId)
            .onEach { state -> currentFrozenState = state }
            .launchIn(modelScope)
    }

    private fun fetchAddToWalletBanner() {
        modelScope.launch {
            val isDone = cardDetailsRepository.isAddToWalletDone(params.userWalletId).getOrNull() == true
            if (!isDone) {
                uiState.update { state ->
                    state.copy(
                        addToWalletBlockState = AddToWalletBlockState(
                            onClick = ::onClickAddToWallet,
                            onClickClose = ::onClickCloseBanner,
                        ),
                    )
                }
            }
        }.saveIn(addToWalletBannerJobHolder)
    }

    private fun onClickAddToWallet() {
        router.push(TangemPayDetailsInnerRoute.AddToWallet)
    }

    private fun onClickCloseBanner() {
        modelScope.launch {
            cardDetailsRepository.setAddToWalletAsDone(params.userWalletId)
            uiState.update { it.copy(addToWalletBlockState = null) }
        }.saveIn(addToWalletBannerJobHolder)
    }

    override fun onClickChangePin() {
        bottomSheetNavigation.dismiss()
        router.push(TangemPayDetailsInnerRoute.ChangePIN)
    }

    override fun onDismissViewPin() {
        bottomSheetNavigation.dismiss()
    }
}
package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.MainCustomerInfoContentState
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.TangemPayCustomerInfoError
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.pay.usecase.TangemPayMainScreenCustomerInfoUseCase
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletTangemPayAnalyticsEventSender
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@Suppress("LongParameterList")
internal class TangemPayMainSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val tangemPayMainScreenCustomerInfoUseCase: TangemPayMainScreenCustomerInfoUseCase,
    private val analytics: WalletTangemPayAnalyticsEventSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return subscribeOnTangemPayInfoUpdates()
    }

    private fun subscribeOnTangemPayInfoUpdates(): Flow<*> {
        return tangemPayMainScreenCustomerInfoUseCase(userWalletId = userWallet.walletId)
            .distinctUntilChanged()
            .onEach { mainInfoData ->
                val userWalletId = userWallet.walletId
                mainInfoData.onLeft { tangemPayError ->
                    when (tangemPayError) {
                        TangemPayCustomerInfoError.RefreshNeededError -> {
                            stateController.update(
                                transformer = TangemPayRefreshNeededStateTransformer(
                                    userWalletId = userWalletId,
                                    userWallet = userWallet,
                                    onRefreshClick = { clickIntents.onRefreshPayToken(userWallet) },
                                ),
                            )
                        }
                        TangemPayCustomerInfoError.UnavailableError -> {
                            stateController.update(
                                transformer = TangemPayUnavailableStateTransformer(userWalletId),
                            )
                        }
                        TangemPayCustomerInfoError.ExposedDeviceError -> {
                            stateController.update(TangemPayExposedDeviceTransformer(userWalletId))
                        }
                        TangemPayCustomerInfoError.UnknownError -> {
                            // hide TangemPay block
                            Timber.e("Failed when loading main screen TangemPay info: $tangemPayError")
                            stateController.update(
                                transformer = TangemPayHiddenStateTransformer(userWalletId),
                            )
                        }
                    }
                }.onRight { contentState -> handleContentState(state = contentState) }
            }
    }

    private suspend fun handleContentState(state: MainCustomerInfoContentState) {
        val userWalletId = userWallet.walletId
        when (state) {
            MainCustomerInfoContentState.Loading -> stateController.update(
                transformer = TangemPayLoadingStateTransformer(userWalletId),
            )
            is MainCustomerInfoContentState.Content -> {
                updateTangemPay(data = state.info, userWalletId = userWalletId)
                analytics.send(customerInfo = state.info)
            }
            is MainCustomerInfoContentState.OnboardingBanner -> stateController.update(
                transformer = TangemPayOnboardingBannerStateTransformer(
                    userWalletId = userWalletId,
                    onClick = clickIntents::onOnboardingBannerClick,
                    closeOnClick = clickIntents::onOnboardingBannerCloseClick,
                ),
            )
        }
    }

    private suspend fun updateTangemPay(data: MainScreenCustomerInfo, userWalletId: UserWalletId) {
        val cardFrozenState =
            data.info.productInstance?.cardId?.let { cardDetailsRepository.cardFrozenStateSync(it) }
                ?: TangemPayCardFrozenState.Unfrozen
        stateController.update(
            transformer = TangemPayUpdateInfoStateTransformer(
                userWalletId = userWalletId,
                value = data,
                cardFrozenState = cardFrozenState,
                tangemPayClickIntents = clickIntents,
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): TangemPayMainSubscriber
    }
}
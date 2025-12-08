package com.tangem.feature.wallet.presentation.wallet.subscribers

import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.MainScreenCustomerInfo
import com.tangem.domain.pay.model.TangemPayCustomerInfoError
import com.tangem.domain.pay.repository.TangemPayCardDetailsRepository
import com.tangem.domain.visa.model.TangemPayCardFrozenState
import com.tangem.feature.wallet.child.wallet.model.TangemPayMainInfoManager
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.router.InnerWalletRouter
import com.tangem.feature.wallet.presentation.wallet.analytics.utils.WalletTangemPayAnalyticsEventSender
import com.tangem.feature.wallet.presentation.wallet.state.WalletStateController
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayHiddenStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayRefreshNeededStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayUnavailableStateTransformer
import com.tangem.feature.wallet.presentation.wallet.state.transformers.TangemPayUpdateInfoStateTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

@Suppress("LongParameterList")
internal class TangemPayMainSubscriber @AssistedInject constructor(
    @Assisted private val userWallet: UserWallet,
    private val stateController: WalletStateController,
    private val clickIntents: WalletClickIntents,
    private val innerWalletRouter: InnerWalletRouter,
    private val cardDetailsRepository: TangemPayCardDetailsRepository,
    private val tangemPayMainInfoManager: TangemPayMainInfoManager,
    private val analytics: WalletTangemPayAnalyticsEventSender,
) : WalletSubscriber() {

    override fun create(coroutineScope: CoroutineScope): Flow<*> {
        return subscribeOnTangemPayInfoUpdates()
    }

    private fun subscribeOnTangemPayInfoUpdates(): Flow<*> {
        return tangemPayMainInfoManager.mainScreenCustomerInfo
            .filter { it.isNotEmpty() }
            .distinctUntilChanged()
            .onEach { data ->
                val userWalletId = userWallet.walletId
                val mainInfoData = data[userWalletId] ?: return@onEach
                mainInfoData.onLeft { tangemPayError ->
                    when (tangemPayError) {
                        TangemPayCustomerInfoError.RefreshNeededError -> {
                            stateController.update(
                                transformer = TangemPayRefreshNeededStateTransformer(
                                    userWalletId = userWalletId,
                                    onRefreshClick = { clickIntents.onRefreshPayToken(userWalletId) },
                                ),
                            )
                        }
                        TangemPayCustomerInfoError.UnavailableError -> {
                            stateController.update(
                                transformer = TangemPayUnavailableStateTransformer(userWalletId),
                            )
                        }
                        else -> {
                            // hide TangemPay block
                            Timber.e("Failed when loading main screen TangemPay info: $tangemPayError")
                            stateController.update(
                                transformer = TangemPayHiddenStateTransformer(userWalletId),
                            )
                        }
                    }
                }.onRight { customerInfo ->
                    updateTangemPay(customerInfo, userWalletId)
                    analytics.send(customerInfo = customerInfo)
                }
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
                onClickKyc = innerWalletRouter::openTangemPayOnboarding,
                onIssuingCard = clickIntents::onIssuingCardClicked,
                onIssuingFailed = clickIntents::onIssuingFailedClicked,
                openDetails = { config ->
                    innerWalletRouter.openTangemPayDetails(
                        userWalletId = userWalletId,
                        config = config,
                    )
                },
            ),
        )
    }

    @AssistedFactory
    interface Factory {
        fun create(userWallet: UserWallet): TangemPayMainSubscriber
    }
}
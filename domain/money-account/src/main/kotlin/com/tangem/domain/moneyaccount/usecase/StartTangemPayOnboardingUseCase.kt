package com.tangem.domain.moneyaccount.usecase

import arrow.core.Either
import arrow.core.raise.either
import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.models.account.TangemPayTariffPlanTransition
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.wallet.isColdWallet
import com.tangem.domain.moneyaccount.analytics.ProductActivationTracker
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.moneyaccount.model.StartTangemPayOnboardingError
import com.tangem.domain.pay.usecase.CreateOnboardingOrderUseCase
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents
import com.tangem.domain.tangempay.TangemPayAnalyticsEvents.SignatureResult
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials
import com.tangem.utils.coroutines.AppCoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.minutes
import kotlin.time.TimeSource

class StartTangemPayOnboardingUseCase(
    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase,
    private val createOnboardingOrderUseCase: CreateOnboardingOrderUseCase,
    private val pollTangemPayAccountStatusesUseCase: PollTangemPayAccountStatusesUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val analytics: AnalyticsEventHandler,
    private val appCoroutineScope: AppCoroutineScope,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) {
    suspend operator fun invoke(
        userWalletId: UserWalletId,
        tariffPlanTransition: TangemPayTariffPlanTransition?,
        availableProducts: Set<TangemPayProduct>,
    ): Either<StartTangemPayOnboardingError, Unit> = either {
        val depositCredentials = if (TangemPayProduct.BANKING_ACCOUNT in availableProducts) {
            activateMoneyAccount(userWalletId).bind()
        } else {
            null
        }

        createOnboardingOrderUseCase(
            userWalletId = userWalletId,
            tariffPlanTransition = tariffPlanTransition,
            depositCredentials = depositCredentials,
        )
            .onRight {
                analytics.send(
                    TangemPayAnalyticsEvents.OnboardingOrderCreated(
                        availableProducts = availableProducts,
                        plan = tariffPlanTransition?.plan,
                    ),
                )
            }
            .onLeft { error ->
                analytics.send(
                    TangemPayAnalyticsEvents.OnboardingOrderFailed(
                        error = error,
                        availableProducts = availableProducts,
                    ),
                )
            }
            .mapLeft { StartTangemPayOnboardingError.Failed }
            .bind()

        val tracker = ProductActivationTracker(
            orderedProducts = availableProducts,
            plan = tariffPlanTransition?.plan,
            orderedAt = timeSource.markNow(),
        )
        appCoroutineScope.launch { trackActivation(userWalletId = userWalletId, tracker = tracker) }
    }

    private suspend fun trackActivation(userWalletId: UserWalletId, tracker: ProductActivationTracker) {
        withTimeoutOrNull(ACTIVATION_POLLING_TIMEOUT) {
            pollTangemPayAccountStatusesUseCase.states(userWalletId).collect { state ->
                tracker.onState(state).forEach(analytics::send)
            }
        }
        tracker.onPollingStopped().forEach(analytics::send)
    }

    private suspend fun activateMoneyAccount(
        userWalletId: UserWalletId,
    ): Either<StartTangemPayOnboardingError, MoneyAccountDepositCredentials> {
        val walletType = walletTypeOf(userWalletId)
        analytics.send(TangemPayAnalyticsEvents.WalletSignatureRequested(walletType = walletType))
        return activateMoneyAccountUseCase(userWalletId)
            .onRight { sendSignatureFinished(result = SignatureResult.SIGNED, walletType = walletType) }
            .mapLeft { error ->
                when (error) {
                    MoneyAccountActivationError.UserCancelled -> {
                        sendSignatureFinished(result = SignatureResult.CANCELLED, walletType = walletType)
                        StartTangemPayOnboardingError.UserCancelled
                    }
                    MoneyAccountActivationError.PersistenceFailed -> {
                        sendSignatureFinished(result = SignatureResult.SIGNED, walletType = walletType)
                        StartTangemPayOnboardingError.Failed
                    }
                    MoneyAccountActivationError.Unknown -> {
                        sendSignatureFinished(result = SignatureResult.FAILED, walletType = walletType)
                        StartTangemPayOnboardingError.Failed
                    }
                }
            }
    }

    private suspend fun walletTypeOf(userWalletId: UserWalletId): TangemPayAnalyticsEvents.WalletType {
        val userWallet = userWalletsListRepository.userWalletsSync().firstOrNull { it.walletId == userWalletId }
        return if (userWallet == null || userWallet.isColdWallet) {
            TangemPayAnalyticsEvents.WalletType.CARD
        } else {
            TangemPayAnalyticsEvents.WalletType.HOT
        }
    }

    private fun sendSignatureFinished(result: SignatureResult, walletType: TangemPayAnalyticsEvents.WalletType) {
        analytics.send(TangemPayAnalyticsEvents.WalletSignatureFinished(result = result, walletType = walletType))
    }

    private companion object {
        val ACTIVATION_POLLING_TIMEOUT = 15.minutes
    }
}
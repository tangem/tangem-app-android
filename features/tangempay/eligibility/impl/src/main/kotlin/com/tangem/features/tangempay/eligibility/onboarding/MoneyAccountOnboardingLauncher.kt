package com.tangem.features.tangempay.eligibility.onboarding

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.moneyaccount.usecase.ActivateMoneyAccountUseCase
import com.tangem.domain.moneyaccount.usecase.CreateMoneyAccountOrderUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountEligibilityUseCase
import com.tangem.domain.pay.repository.OnboardingRepository
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class MoneyAccountOnboardingLauncher @Inject constructor(
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val getMoneyAccountEligibilityUseCase: GetMoneyAccountEligibilityUseCase,
    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase,
    private val createMoneyAccountOrderUseCase: CreateMoneyAccountOrderUseCase,
    private val onboardingRepository: OnboardingRepository,
) : OnboardingLauncher {

    override suspend fun start(
        userWalletId: UserWalletId?,
        onWalletSelectionRequired: (List<UserWalletId>) -> Unit,
        onFailed: () -> Unit,
    ) {
        if (userWalletId != null) {
            continueWith(userWalletId = userWalletId, onFailed = onFailed)
            return
        }

        val walletsIds = when (val eligibility = getMoneyAccountEligibilityUseCase()) {
            is MoneyAccountEligibility.Available -> eligibility.wallets.map { it.walletId }
            MoneyAccountEligibility.NotAvailable -> emptyList()
        }

        when (walletsIds.size) {
            0 -> router.pop()
            1 -> continueWith(
                userWalletId = walletsIds.first(),
                onFailed = onFailed,
            )
            else -> onWalletSelectionRequired(walletsIds)
        }
    }

    override suspend fun continueWith(userWalletId: UserWalletId, onFailed: () -> Unit) {
        val credentials = activateMoneyAccountUseCase(userWalletId).getOrElse { error ->
            if (error is MoneyAccountActivationError.UserCancelled) {
                onFailed()
            } else {
                showError(onFailed)
            }
            return
        }

        createMoneyAccountOrderUseCase(
            userWalletId = userWalletId,
            depositAddress = credentials.address,
            signature = credentials.signature,
        ).onLeft { _ ->
            showError(onFailed)
            return
        }

        onboardingRepository.getCustomerInfo(userWalletId).fold(
            ifLeft = { _ -> showError(onFailed) },
            ifRight = { customerInfo ->
                if (customerInfo.kycStatus == KycStatus.APPROVED) {
                    router.replaceAll(AppRoute.Wallet)
                } else {
                    router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(userWalletId = userWalletId))
                }
            },
        )
    }

    private fun showError(onFailed: () -> Unit) {
        onFailed()
        uiMessageSender.send(
            DialogMessage(
                title = resourceReference(R.string.common_something_went_wrong),
                message = resourceReference(R.string.common_try_again_later),
            ),
        )
    }
}
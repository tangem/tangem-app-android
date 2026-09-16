package com.tangem.features.tangempay.eligibility.onboarding

import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class PaymentAccountOnboardingLauncher @Inject constructor(
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val eligibilityManager: TangemPayEligibilityManager,
    private val onboardingRepository: OnboardingRepository,
    private val produceInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
    private val featureToggles: TangemPayFeatureToggles,
) {
    suspend fun start(onWalletSelectionRequired: (List<UserWalletId>) -> Unit, onFailed: () -> Unit) {
        val walletsIds = eligibilityManager.getCompatibleWalletsIds(shouldExcludePaeraCustomers = true)

        when (walletsIds.size) {
            0 -> router.pop()
            1 -> continueWith(userWalletId = walletsIds.first(), onFailed = onFailed)
            else -> onWalletSelectionRequired(walletsIds)
        }
    }

    suspend fun continueWith(userWalletId: UserWalletId, onFailed: () -> Unit) {
        produceInitialDataUseCase(userWalletId).onLeft { _ ->
            showError(onFailed)
            return
        }

        onboardingRepository.getCustomerInfo(userWalletId).fold(
            ifLeft = { _ -> showError(onFailed) },
            ifRight = { customerInfo ->
                if (customerInfo.kycStatus == KycStatus.APPROVED) {
                    router.pop()
                } else {
                    createOrderIfNeeded(userWalletId = userWalletId, customerInfo = customerInfo)
                    router.replaceAll(AppRoute.Wallet, AppRoute.Kyc(userWalletId = userWalletId))
                }
            },
        )
    }

    private suspend fun createOrderIfNeeded(userWalletId: UserWalletId, customerInfo: CustomerInfo) {
        val shouldCreateOrder = !featureToggles.isTiersPlusPlanEnabled && customerInfo.productInstance == null
        if (!shouldCreateOrder) return

        onboardingRepository.createOrder(userWalletId)
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
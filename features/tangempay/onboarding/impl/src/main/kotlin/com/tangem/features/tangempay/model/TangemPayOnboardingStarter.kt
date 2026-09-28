package com.tangem.features.tangempay.model

import arrow.core.getOrElse
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.DialogMessage
import com.tangem.core.ui.message.dialog.Dialogs
import com.tangem.domain.hotwallet.IsHotWalletCreationSupported
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerOffersUseCase
import com.tangem.domain.pay.usecase.ProduceTangemPayInitialDataUseCase
import com.tangem.domain.wallets.usecase.CreateHotWalletUseCase
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.tangempay.common.TangemPayCampaignMessages
import com.tangem.hot.sdk.model.HotAuth
import com.tangem.hot.sdk.model.MnemonicType
import com.tangem.utils.logging.TangemLogger
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class TangemPayOnboardingStarter @Inject constructor(
    private val router: Router,
    private val uiMessageSender: UiMessageSender,
    private val repository: OnboardingRepository,
    private val produceInitialDataUseCase: ProduceTangemPayInitialDataUseCase,
    private val getCustomerOffersUseCase: GetCustomerOffersUseCase,
    private val createHotWalletUseCase: CreateHotWalletUseCase,
    private val isHotWalletCreationSupported: IsHotWalletCreationSupported,
    private val tangemPayFeatureToggles: TangemPayFeatureToggles,
) {

    enum class Mode { StartOnboarding, ContinueToKyc }

    class Callbacks(
        val onLoadingChange: (Boolean) -> Unit,
        val onExit: () -> Unit,
        val onWalletSelectorRequested: (List<UserWalletId>) -> Unit = {},
        val onCustomerAlreadyApproved: (suspend (UserWalletId) -> Unit)? = null,
    )

    suspend fun proceedWithWallets(
        walletsIds: List<UserWalletId>,
        mode: Mode,
        campaignKey: String?,
        callbacks: Callbacks,
    ) {
        when (walletsIds.size) {
            0 -> callbacks.onExit()
            1 -> checkCustomerInfo(
                userWalletId = walletsIds[0],
                mode = mode,
                campaignKey = campaignKey,
                callbacks = callbacks,
            )
            else -> callbacks.onWalletSelectorRequested(walletsIds)
        }
    }

    suspend fun checkCustomerInfo(userWalletId: UserWalletId, mode: Mode, campaignKey: String?, callbacks: Callbacks) {
        callbacks.onLoadingChange(true)
        repository.getCustomerInfo(userWalletId = userWalletId)
            .onRight { customerInfo ->
                if (customerInfo.kycStatus != KycStatus.APPROVED) {
                    when (mode) {
                        Mode.ContinueToKyc -> proceedToKyc(
                            userWalletId = userWalletId,
                            campaignKey = campaignKey,
                            callbacks = callbacks,
                        )
                        Mode.StartOnboarding -> startOnboarding(userWalletId, campaignKey, callbacks)
                    }
                } else {
                    callbacks.onCustomerAlreadyApproved?.invoke(userWalletId) ?: callbacks.onExit()
                }
                callbacks.onLoadingChange(false)
            }
            .onLeft { startOnboarding(userWalletId, campaignKey, callbacks) }
    }

    suspend fun startOnboarding(userWalletId: UserWalletId, campaignKey: String?, callbacks: Callbacks) {
        callbacks.onLoadingChange(true)
        produceInitialDataUseCase(userWalletId).onLeft { error ->
            TangemLogger.e("Error producing initial data: ${error.message ?: "Unknown error"}")
            showRetryableError(callbacks)
            return
        }
        repository.getCustomerInfo(userWalletId = userWalletId).fold(
            ifLeft = { error ->
                TangemLogger.e("Error getCustomerInfo: ${error.errorCode}")
                showRetryableError(callbacks)
            },
            ifRight = { customerInfo ->
                if (customerInfo.kycStatus == KycStatus.APPROVED) {
                    callbacks.onCustomerAlreadyApproved?.invoke(userWalletId) ?: callbacks.onExit()
                } else {
                    proceedToKyc(
                        userWalletId = userWalletId,
                        campaignKey = campaignKey,
                        callbacks = callbacks,
                    )
                }
            },
        )
    }

    suspend fun createHotWallet(onLoadingChange: (Boolean) -> Unit): UserWallet.Hot? {
        val isSupported = isHotWalletCreationSupported()
        TangemLogger.i("[TangemPay][HWO]Hot wallet creation supported=$isSupported")
        if (!isSupported) {
            uiMessageSender.send(
                Dialogs.hotWalletCreationNotSupportedDialog(isHotWalletCreationSupported.getLeastVersionName()),
            )
            router.replaceCurrent(AppRoute.Home())
            return null
        }

        TangemLogger.i("[TangemPay][HWO]Creating hot wallet")
        return createHotWalletUseCase(auth = HotAuth.NoAuth, mnemonicType = MnemonicType.Words12)
            .onRight { TangemLogger.i("[TangemPay][HWO]Hot wallet created") }
            .getOrElse {
                TangemLogger.e("[TangemPay][HWO] Failed to create hot wallet")
                onLoadingChange(false)
                uiMessageSender.send(
                    DialogMessage(
                        title = resourceReference(R.string.common_something_went_wrong),
                        message = resourceReference(R.string.common_unknown_error),
                    ),
                )
                null
            }
    }

    fun openKyc(userWalletId: UserWalletId) {
        router.replaceAll(
            AppRoute.Wallet,
            AppRoute.Kyc(userWalletId = userWalletId),
        )
    }

    private suspend fun proceedToKyc(userWalletId: UserWalletId, campaignKey: String?, callbacks: Callbacks) {
        if (campaignKey != null) {
            val isOrderCreated = createCampaignOrder(userWalletId, campaignKey, callbacks)
            if (!isOrderCreated) return
        }
        if (tangemPayFeatureToggles.isResidencyStepEnabled) {
            router.replaceAll(
                AppRoute.Wallet,
                AppRoute.TangemPayEligibility(
                    product = TangemPayProduct.PAYMENT_ACCOUNT,
                    userWalletId = userWalletId,
                ),
            )
        } else {
            openKyc(userWalletId)
        }
    }

    private suspend fun createCampaignOrder(
        userWalletId: UserWalletId,
        campaignKey: String,
        callbacks: Callbacks,
    ): Boolean {
        val offer = getCustomerOffersUseCase.campaignOffer(userWalletId = userWalletId, deeplinkValue = campaignKey)
            .getOrElse { error ->
                TangemLogger.e("[TangemPay][Campaign] Failed to load campaign offer: $error")
                showRetryableError(callbacks)
                return false
            }
        if (offer == null) {
            TangemLogger.i("[TangemPay][Campaign] No campaign offer for key=$campaignKey")
            callbacks.onLoadingChange(false)
            uiMessageSender.send(TangemPayCampaignMessages.unavailableDialog(onDismiss = callbacks.onExit))
            return false
        }
        val specificationName = offer.data.specificationName
        if (specificationName == null) {
            TangemLogger.e("[TangemPay][Campaign] Campaign offer has no specification name")
            showRetryableError(callbacks)
            return false
        }

        return repository.createOrder(
            userWalletId = userWalletId,
            deeplinkValue = campaignKey,
            specificationName = specificationName,
            type = offer.data.orderType,
        )
            .onLeft { error ->
                TangemLogger.e("[TangemPay][Campaign] Failed to create campaign order: $error")
                showRetryableError(callbacks)
            }
            .isRight()
    }

    private fun showRetryableError(callbacks: Callbacks) {
        callbacks.onLoadingChange(false)
        uiMessageSender.send(TangemPayCampaignMessages.retryableErrorSnackbar())
    }
}
package com.tangem.features.feed.components.v2.blocks.model

import androidx.compose.runtime.Stable
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.domain.account.supplier.MultiAccountListSupplier
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.flow.PaymentAccountStatusSupplier
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Whether the Tangem Pay pitch is worth making: at least one wallet can still be offered Pay and none is
 * already a customer. A customer still in KYC counts as offerable — the pitch leads them back to it.
 *
 * Only wallets that actually carry an [Account.Payment] are asked for a status, so a user without
 * Tangem Pay never spins up a payment pipeline.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Stable
@ModelScoped
internal class TangemPayBlockModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    multiAccountListSupplier: MultiAccountListSupplier,
    paymentAccountStatusSupplier: PaymentAccountStatusSupplier,
) : Model() {

    val onboardingMode: StateFlow<AppRoute.TangemPayOnboarding.Mode?> = multiAccountListSupplier()
        .map { lists ->
            lists.filter { list -> list.accounts.any { it is Account.Payment } }.map { it.userWalletId }
        }
        .distinctUntilChanged()
        .flatMapLatest { walletIds ->
            if (walletIds.isEmpty()) {
                flowOf(null)
            } else {
                combine(
                    flows = walletIds.map { walletId ->
                        paymentAccountStatusSupplier(walletId)
                            .map { status -> walletId to status.value.toPromoEligibility() }
                            .onStart { emit(walletId to PromoEligibility.NOT_AVAILABLE) }
                    },
                ) { eligibilities -> eligibilities.toOnboardingMode() }
            }
        }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = null)

    val isVisible: StateFlow<Boolean> = onboardingMode
        .map { mode -> mode != null }
        .stateIn(scope = modelScope, started = SharingStarted.Eagerly, initialValue = false)
}

private enum class PromoEligibility { CAN_OFFER, IN_KYC, NOT_AVAILABLE, ALREADY_CUSTOMER }

private fun PaymentAccountStatusValue.toPromoEligibility(): PromoEligibility = when (this) {
    is PaymentAccountStatusValue.NotCreated -> PromoEligibility.CAN_OFFER
    is PaymentAccountStatusValue.UnderReview -> PromoEligibility.IN_KYC
    is PaymentAccountStatusValue.Empty -> PromoEligibility.NOT_AVAILABLE
    is PaymentAccountStatusValue.Loading,
    is PaymentAccountStatusValue.IssuingCard,
    is PaymentAccountStatusValue.AwaitingPlanSelection,
    is PaymentAccountStatusValue.Inactive,
    is PaymentAccountStatusValue.Loaded,
    is PaymentAccountStatusValue.Deactivated,
    is PaymentAccountStatusValue.Error,
    -> PromoEligibility.ALREADY_CUSTOMER
}

private fun Array<Pair<UserWalletId, PromoEligibility>>.toOnboardingMode(): AppRoute.TangemPayOnboarding.Mode? {
    if (any { (_, eligibility) -> eligibility == PromoEligibility.ALREADY_CUSTOMER }) return null

    val walletInKyc = firstOrNull { (_, eligibility) -> eligibility == PromoEligibility.IN_KYC }?.first

    return when {
        walletInKyc != null -> AppRoute.TangemPayOnboarding.Mode.ContinueOnboarding(userWalletId = walletInKyc)
        any { (_, eligibility) -> eligibility == PromoEligibility.CAN_OFFER } ->
            AppRoute.TangemPayOnboarding.Mode.FromBannerOnMain
        else -> null
    }
}
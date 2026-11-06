package com.tangem.domain.pay.usecase

import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.PaymentAccountActivationState
import com.tangem.domain.pay.model.TangemPayCustomerState
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.utils.TangemPayCustomerStateConverter

class ResolvePaymentAccountStatusFromSummaryUseCase(
    private val onboardingRepository: OnboardingRepository,
) {

    suspend operator fun invoke(userWalletId: UserWalletId, summary: CustomerSummary): PaymentAccountStatusValue? {
        return when (val state = TangemPayCustomerStateConverter.convert(summary)) {
            TangemPayCustomerState.Blocked -> {
                PaymentAccountStatusValue.Empty
            }
            TangemPayCustomerState.KycRequired -> {
                kycStatus(userWalletId, summary, CustomerSummary.KycStatus.REQUIRED)
            }
            TangemPayCustomerState.KycInProgress -> {
                kycStatus(userWalletId, summary, CustomerSummary.KycStatus.IN_PROGRESS)
            }
            TangemPayCustomerState.KycDeclined -> {
                kycStatus(userWalletId, summary, CustomerSummary.KycStatus.DECLINED)
            }
            TangemPayCustomerState.KycApproved -> {
                verified()
            }
            is TangemPayCustomerState.Former -> {
                former(state.paymentAccount)
            }
            is TangemPayCustomerState.Accounts -> {
                account(userWalletId, summary, state.paymentAccount)
            }
        }
    }

    private fun former(state: PaymentAccountActivationState): PaymentAccountStatusValue? {
        return when (state) {
            PaymentAccountActivationState.NotStarted,
            PaymentAccountActivationState.Unavailable,
            is PaymentAccountActivationState.AwaitingKyc,
            -> PaymentAccountStatusValue.Empty
            is PaymentAccountActivationState.Issuing,
            is PaymentAccountActivationState.Failed,
            is PaymentAccountActivationState.Completed,
            -> null
        }
    }

    private suspend fun account(
        userWalletId: UserWalletId,
        summary: CustomerSummary,
        state: PaymentAccountActivationState,
    ): PaymentAccountStatusValue? {
        return when (state) {
            PaymentAccountActivationState.NotStarted, PaymentAccountActivationState.Unavailable -> {
                PaymentAccountStatusValue.Empty
            }
            is PaymentAccountActivationState.AwaitingKyc -> {
                kycStatus(userWalletId, summary, state.kycStatus)
            }
            is PaymentAccountActivationState.Failed -> {
                null
            }
            is PaymentAccountActivationState.Issuing, is PaymentAccountActivationState.Completed -> {
                null
            }
        }
    }

    private suspend fun kycStatus(
        userWalletId: UserWalletId,
        summary: CustomerSummary,
        kycStatus: CustomerSummary.KycStatus,
    ): PaymentAccountStatusValue {
        return when (kycStatus) {
            CustomerSummary.KycStatus.REQUIRED -> {
                PaymentAccountStatusValue.KycRequired(
                    source = StatusSource.ACTUAL,
                    customerId = summary.customerId,
                    customerResidency = onboardingRepository.getCustomerResidency(userWalletId),
                )
            }
            CustomerSummary.KycStatus.IN_PROGRESS, CustomerSummary.KycStatus.UNKNOWN -> {
                underReview(summary, KycStatus.PENDING)
            }
            CustomerSummary.KycStatus.DECLINED -> {
                underReview(summary, KycStatus.REJECTED)
            }
            CustomerSummary.KycStatus.APPROVED -> {
                verified()
            }
        }
    }

    private fun verified() = PaymentAccountStatusValue.Verified(source = StatusSource.ACTUAL)

    private fun underReview(summary: CustomerSummary, kycStatus: KycStatus) = PaymentAccountStatusValue.UnderReview(
        source = StatusSource.ACTUAL,
        kycStatus = kycStatus,
        customerId = summary.customerId,
    )
}
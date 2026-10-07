package com.tangem.data.moneyaccount.flow

import arrow.core.Either
import arrow.core.getOrElse
import arrow.core.raise.either
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.core.utils.catchOn
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.repository.MoneyAccountOrderRepository
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.security.isSecurityExposed
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import javax.inject.Inject

internal class DefaultMoneyAccountStatusFetcher @Inject constructor(
    private val moneyAccountStatusesStore: MoneyAccountStatusesStore,
    private val orderRepository: MoneyAccountOrderRepository,
    private val onboardingRepository: OnboardingRepository,
    private val dispatchers: CoroutineDispatcherProvider,
    private val deviceSecurity: DeviceSecurityInfoProvider,
) : MoneyAccountStatusFetcher {

    override suspend fun invoke(params: MoneyAccountStatusFetcher.Params): Either<Throwable, Unit> {
        val account = Account.Money(userWalletId = params.userWalletId)
        return Either.catchOn(dispatchers.default) {
            val status = fetch(params.userWalletId).getOrElse { error("Cannot fetch money account information") }
            moneyAccountStatusesStore.store(
                userWalletId = params.userWalletId,
                status = AccountStatus.Money(
                    account = account,
                    value = status,
                ),
            )
        }.onLeft {
            runSuspendCatching {
                moneyAccountStatusesStore.store(
                    userWalletId = params.userWalletId,
                    status = AccountStatus.Money(
                        account = account,
                        value = MoneyAccountStatusValue.Error.Unavailable,
                    ),
                )
            }
        }
    }

    private suspend fun fetch(userWalletId: UserWalletId): Either<VisaApiError, MoneyAccountStatusValue> = either {
        if (deviceSecurity.isSecurityExposed()) {
            return@either MoneyAccountStatusValue.Error.ExposedDevice
        }

        val customerInfo = onboardingRepository.getCustomerInfo(userWalletId).bind()
        val instance = customerInfo.moneyAccountProductInstance
        when {
            instance == null && customerInfo.state == CustomerInfo.State.FORMER -> MoneyAccountStatusValue.Empty
            instance == null -> onboardingStatus(userWalletId = userWalletId, customerInfo = customerInfo).bind()
            customerInfo.state == CustomerInfo.State.FORMER -> {
                MoneyAccountStatusValue.Account.Deactivated(
                    source = StatusSource.ACTUAL,
                    balance = null,
                    customerId = customerInfo.customerId,
                    productInstanceId = instance.id,
                )
            }
            instance.status == CustomerInfo.ProductInstance.Status.DEACTIVATED -> {
                MoneyAccountStatusValue.Account.Deactivated(
                    source = StatusSource.ACTUAL,
                    balance = null,
                    customerId = customerInfo.customerId,
                    productInstanceId = instance.id,
                )
            }
            instance.status == CustomerInfo.ProductInstance.Status.ACTIVE -> {
                MoneyAccountStatusValue.Account.Active(
                    source = StatusSource.ACTUAL,
                    balance = null,
                    customerId = customerInfo.customerId,
                    productInstanceId = instance.id,
                )
            }
            else -> onboardingStatus(userWalletId = userWalletId, customerInfo = customerInfo).bind()
        }
    }

    private fun onboardingStatus(
        userWalletId: UserWalletId,
        customerInfo: CustomerInfo,
    ): Either<VisaApiError, MoneyAccountStatusValue> = either {
        val order = orderRepository.getOrder(userWalletId)
        val customerId = customerInfo.customerId
        return@either when {
            order == null -> MoneyAccountStatusValue.NotCreated
            customerInfo.kycStatus == KycStatus.REQUIRED && order.orderStatus != OrderStatus.CANCELED -> {
                MoneyAccountStatusValue.Onboarding.KycRequired(
                    source = StatusSource.ACTUAL,
                    balance = null,
                    customerId = customerId,
                )
            }
            order.orderStatus == OrderStatus.COMPLETED -> {
                MoneyAccountStatusValue.Onboarding.Activating(
                    source = StatusSource.ACTUAL,
                    balance = null,
                    customerId = customerId,
                )
            }
            else -> when (order.orderStep) {
                OrderStep.AWAITING_KYC -> kycStatus(customerId = customerId, customerInfo = customerInfo)
                OrderStep.ACCOUNT_ISSUE,
                OrderStep.ACCOUNT_ACTIVATION,
                -> MoneyAccountStatusValue.Onboarding.Activating(
                    source = StatusSource.ACTUAL,
                    balance = null,
                    customerId = customerId,
                )
                else -> MoneyAccountStatusValue.Onboarding.Issuing(
                    source = StatusSource.ACTUAL,
                    balance = null,
                    customerId = customerId,
                    step = order.toIssuingStep(),
                )
            }
        }
    }

    private fun kycStatus(customerId: String, customerInfo: CustomerInfo): MoneyAccountStatusValue {
        val kycStatus = customerInfo.kycStatus
        return if (kycStatus == KycStatus.REJECTED) {
            MoneyAccountStatusValue.Onboarding.VerificationDeclined(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = customerId,
            )
        } else {
            MoneyAccountStatusValue.Onboarding.UnderReview(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = customerId,
                kycStatus = kycStatus,
            )
        }
    }

    private fun TangemPayOrderInfo.toIssuingStep(): MoneyAccountStatusValue.IssuingStep = when (orderStep) {
        OrderStep.STARTING -> MoneyAccountStatusValue.IssuingStep.STARTING
        OrderStep.INITIALIZATION -> MoneyAccountStatusValue.IssuingStep.INITIALIZATION
        OrderStep.ELIGIBILITY_CHECK -> MoneyAccountStatusValue.IssuingStep.ELIGIBILITY_CHECK
        OrderStep.RAIN_APPROVAL -> MoneyAccountStatusValue.IssuingStep.RAIN_APPROVAL
        else -> MoneyAccountStatusValue.IssuingStep.UNKNOWN
    }
}
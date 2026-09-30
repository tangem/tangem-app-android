package com.tangem.data.moneyaccount.flow

import arrow.core.Either
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
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.model.OrderStep
import com.tangem.domain.pay.model.TangemPayOrderInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.coroutines.runSuspendCatching
import java.math.BigDecimal
import javax.inject.Inject

internal class DefaultMoneyAccountStatusFetcher @Inject constructor(
    private val moneyAccountStatusesStore: MoneyAccountStatusesStore,
    private val orderRepository: MoneyAccountOrderRepository,
    private val onboardingRepository: OnboardingRepository,
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory,
    private val dispatchers: CoroutineDispatcherProvider,
) : MoneyAccountStatusFetcher {

    // TODO v_rodionov: [REDACTED_TASK_KEY] Test-only, remove it
    private var testStatusIndex = 0

    override suspend fun invoke(params: MoneyAccountStatusFetcher.Params): Either<Throwable, Unit> {
        val account = Account.Money(userWalletId = params.userWalletId)
        return Either.catchOn(dispatchers.default) {
            // TODO v_rodionov: [REDACTED_TASK_KEY] Test-only, remove it
            val status = nextTestStatus(params.userWalletId)
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

    @Suppress("UnusedPrivateMember")
    private suspend fun fetch(userWalletId: UserWalletId): Either<VisaApiError, MoneyAccountStatusValue> = either {
        val customerInfo = onboardingRepository.getCustomerInfo(userWalletId).bind()
        val instance = customerInfo.accountProductInstance
        when (instance?.status) {
            CustomerInfo.ProductInstance.Status.ACTIVE -> MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                balance = null,
                // TODO v_rodionov: [REDACTED_TASK_KEY] find out why customerId is nullable
                customerId = customerInfo.customerId.orEmpty(),
                productInstanceId = instance.id,
            )
            CustomerInfo.ProductInstance.Status.DEACTIVATED -> MoneyAccountStatusValue.Account.Deactivated(
                source = StatusSource.ACTUAL,
                balance = null,
                // TODO v_rodionov: [REDACTED_TASK_KEY] find out why customerId is nullable
                customerId = customerInfo.customerId.orEmpty(),
                productInstanceId = instance.id,
            )
            else -> onboardingStatus(userWalletId = userWalletId, customerInfo = customerInfo)
        }
    }

    private suspend fun onboardingStatus(
        userWalletId: UserWalletId,
        customerInfo: CustomerInfo,
    ): MoneyAccountStatusValue {
        val order = orderRepository.getOrder(userWalletId) ?: return lastOnboardingStatus(userWalletId)
        val customerId = customerInfo.customerId

        if (order.orderStatus == OrderStatus.COMPLETED) {
            return MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = customerId,
            )
        }

        return when (order.orderStep) {
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

    private fun kycStatus(customerId: String?, customerInfo: CustomerInfo?): MoneyAccountStatusValue {
        // TODO v_rodionov: [REDACTED_TASK_KEY] find out why customerId is nullable
        if (customerId == null) {
            return MoneyAccountStatusValue.Onboarding.Issuing(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = null,
                step = MoneyAccountStatusValue.IssuingStep.UNKNOWN,
            )
        }

        val kycStatus = customerInfo?.kycStatus ?: KycStatus.INIT
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

    private suspend fun lastOnboardingStatus(userWalletId: UserWalletId): MoneyAccountStatusValue {
        val previous = moneyAccountStatusesStore.getSyncOrNull(userWalletId)?.value
        return previous as? MoneyAccountStatusValue.Onboarding ?: MoneyAccountStatusValue.NotCreated
    }

    // TODO v_rodionov: [REDACTED_TASK_KEY] Test-only, remove it
    @Suppress("LongMethod")
    private fun nextTestStatus(userWalletId: UserWalletId): MoneyAccountStatusValue {
        val balance = MoneyAccountStatusValue.Balance(
            cryptoAmount = BigDecimal("123.45"),
            cryptoCurrency = tangemPayCurrencyFactory.createMoneyAccountToken(userWalletId),
            depositAddress = "0x0000000000000000000000000000000000000000",
            currencyCode = "USD",
            fiatRate = BigDecimal.ONE,
        )
        val statuses = listOf(
            MoneyAccountStatusValue.NotCreated,
            MoneyAccountStatusValue.Loading,
            MoneyAccountStatusValue.Onboarding.Issuing(
                source = StatusSource.ACTUAL,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
                step = MoneyAccountStatusValue.IssuingStep.STARTING,
            ),
            MoneyAccountStatusValue.Onboarding.Activating(
                source = StatusSource.ACTUAL,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
            ),
            MoneyAccountStatusValue.Onboarding.UnderReview(
                source = StatusSource.ACTUAL,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
                kycStatus = KycStatus.INIT,
            ),
            MoneyAccountStatusValue.Onboarding.VerificationDeclined(
                source = StatusSource.ACTUAL,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
            ),
            MoneyAccountStatusValue.Onboarding.CountryNotSupported,
            MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
                productInstanceId = TEST_PRODUCT_INSTANCE_ID,
            ),
            MoneyAccountStatusValue.Account.Active(
                source = StatusSource.CACHE,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
                productInstanceId = TEST_PRODUCT_INSTANCE_ID,
            ),
            MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ONLY_CACHE,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
                productInstanceId = TEST_PRODUCT_INSTANCE_ID,
            ),
            MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                balance = null,
                customerId = TEST_CUSTOMER_ID,
                productInstanceId = TEST_PRODUCT_INSTANCE_ID,
            ),
            MoneyAccountStatusValue.Account.Deactivated(
                source = StatusSource.ACTUAL,
                balance = balance,
                customerId = TEST_CUSTOMER_ID,
                productInstanceId = TEST_PRODUCT_INSTANCE_ID,
            ),
            MoneyAccountStatusValue.Error.Unavailable,
            MoneyAccountStatusValue.Error.NotSynced,
            MoneyAccountStatusValue.Error.ExposedDevice,
        )
        val status = statuses[testStatusIndex % statuses.size]
        testStatusIndex++
        return status
    }

    private fun TangemPayOrderInfo.toIssuingStep(): MoneyAccountStatusValue.IssuingStep = when (orderStep) {
        OrderStep.STARTING -> MoneyAccountStatusValue.IssuingStep.STARTING
        OrderStep.INITIALIZATION -> MoneyAccountStatusValue.IssuingStep.INITIALIZATION
        OrderStep.ELIGIBILITY_CHECK -> MoneyAccountStatusValue.IssuingStep.ELIGIBILITY_CHECK
        OrderStep.RAIN_APPROVAL -> MoneyAccountStatusValue.IssuingStep.RAIN_APPROVAL
        else -> MoneyAccountStatusValue.IssuingStep.UNKNOWN
    }
}

// TODO v_rodionov: [REDACTED_TASK_KEY] Test-only, remove it
private const val TEST_CUSTOMER_ID = "test_customer_id"
private const val TEST_PRODUCT_INSTANCE_ID = "test_product_instance_id"
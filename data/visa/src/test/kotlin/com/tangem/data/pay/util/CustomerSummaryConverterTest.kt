package com.tangem.data.pay.util

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.model.CustomerSummary.Activation.FailureReason
import com.tangem.domain.pay.model.CustomerSummary.Activation.Status
import com.tangem.domain.pay.model.CustomerSummary.Activation.Step
import com.tangem.spend.datasource.pay.models.response.CustomerSummaryResponse
import com.tangem.test.core.ProvideTestModels
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CustomerSummaryConverterTest {

    @ParameterizedTest
    @ProvideTestModels
    fun convert(model: ConvertModel) {
        // Act
        val actual = CustomerSummaryConverter.convert(model.response)

        // Assert
        assertThat(actual).isEqualTo(model.expected)
    }

    @Suppress("LongMethod")
    private fun provideTestModels() = listOf(
        ConvertModel(
            name = "all fields known -> mapped one to one",
            response = createResponse(),
            expected = createExpected(),
        ),
        ConvertModel(
            name = "lowercase values -> recognised",
            response = createResponse(
                customerStatus = "active",
                kycStatus = "approved",
                paymentActivation = activation(status = "processing", step = "awaiting_deposit"),
                bankingActivation = activation(status = "failed", failureReason = "rain_rejected"),
            ),
            expected = createExpected(
                paymentActivation = CustomerSummary.Activation(
                    status = Status.PROCESSING,
                    step = Step.AWAITING_DEPOSIT,
                    failureReason = null,
                ),
                bankingActivation = CustomerSummary.Activation(
                    status = Status.FAILED,
                    step = null,
                    failureReason = FailureReason.RAIN_REJECTED,
                ),
            ),
        ),
        ConvertModel(
            name = "unrecognised values -> UNKNOWN fallbacks",
            response = createResponse(
                customerStatus = "SUSPENDED",
                kycStatus = "ON_HOLD",
                paymentActivation = activation(status = "PAUSED", step = "SHIPPING", failureReason = "LIMITS"),
            ),
            expected = createExpected(
                customerStatus = CustomerInfo.State.UNDEFINED,
                kycStatus = KycStatus.UNKNOWN,
                paymentActivation = CustomerSummary.Activation(
                    status = Status.UNKNOWN,
                    step = Step.UNKNOWN,
                    failureReason = FailureReason.UNKNOWN,
                ),
            ),
        ),
        ConvertModel(
            name = "no account ids and no step -> nulls kept",
            response = createResponse(
                paymentAccountId = null,
                bankingAccountId = null,
                paymentActivation = activation(status = "NOT_STARTED"),
                bankingActivation = activation(status = "DEACTIVATED"),
            ),
            expected = createExpected(
                paymentAccountId = null,
                bankingAccountId = null,
                paymentActivation = CustomerSummary.Activation(
                    status = Status.NOT_STARTED,
                    step = null,
                    failureReason = null,
                ),
                bankingActivation = CustomerSummary.Activation(
                    status = Status.DEACTIVATED,
                    step = null,
                    failureReason = null,
                ),
            ),
        ),
    )

    data class ConvertModel(
        val name: String,
        val response: CustomerSummaryResponse.Result,
        val expected: CustomerSummary,
    ) {
        override fun toString(): String = name
    }

    private fun activation(status: String, step: String? = null, failureReason: String? = null) =
        CustomerSummaryResponse.Activation(status = status, step = step, failureReason = failureReason)

    private fun createResponse(
        customerStatus: String = "ACTIVE",
        kycStatus: String = "APPROVED",
        paymentAccountId: String? = PAYMENT_ACCOUNT_ID,
        bankingAccountId: String? = BANKING_ACCOUNT_ID,
        paymentActivation: CustomerSummaryResponse.Activation = activation(status = "COMPLETED"),
        bankingActivation: CustomerSummaryResponse.Activation = activation(status = "PROCESSING", step = "ISSUING"),
    ) = CustomerSummaryResponse.Result(
        customerId = CUSTOMER_ID,
        customerStatus = customerStatus,
        hidden = true,
        kycStatus = kycStatus,
        paymentAccount = CustomerSummaryResponse.PaymentAccount(
            id = paymentAccountId,
            activation = paymentActivation,
            cards = CustomerSummaryResponse.Cards(active = 1, frozen = 2, pending = 3),
        ),
        bankingAccount = CustomerSummaryResponse.BankingAccount(
            id = bankingAccountId,
            activation = bankingActivation,
        ),
    )

    private fun createExpected(
        customerStatus: CustomerInfo.State = CustomerInfo.State.ACTIVE,
        kycStatus: KycStatus = KycStatus.APPROVED,
        paymentAccountId: String? = PAYMENT_ACCOUNT_ID,
        bankingAccountId: String? = BANKING_ACCOUNT_ID,
        paymentActivation: CustomerSummary.Activation = CustomerSummary.Activation(
            status = Status.COMPLETED,
            step = null,
            failureReason = null,
        ),
        bankingActivation: CustomerSummary.Activation = CustomerSummary.Activation(
            status = Status.PROCESSING,
            step = Step.ISSUING,
            failureReason = null,
        ),
    ) = CustomerSummary(
        customerId = CUSTOMER_ID,
        customerStatus = customerStatus,
        isHidden = true,
        kycStatus = kycStatus,
        paymentAccount = CustomerSummary.PaymentAccount(
            id = paymentAccountId,
            activation = paymentActivation,
            cards = CustomerSummary.Cards(active = 1, frozen = 2, pending = 3),
        ),
        bankingAccount = CustomerSummary.BankingAccount(
            id = bankingAccountId,
            activation = bankingActivation,
        ),
    )

    private companion object {
        const val CUSTOMER_ID = "e363b338-6a30-4dff-95b7-ebd6188eb57b"
        const val PAYMENT_ACCOUNT_ID = "payment-account-id"
        const val BANKING_ACCOUNT_ID = "banking-account-id"
    }
}
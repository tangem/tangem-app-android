package com.tangem.domain.tangempay

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.tangempay.model.CustomerSummary.PaymentAccount
import com.tangem.test.core.ProvideTestModels
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest

internal class TangemPayUserPropertiesTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FromSummary {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN customer summary WHEN mapped THEN user properties match the spec`(model: SummaryModel) {
            // Act
            val actual = TangemPayUserProperties.of(model.summary)

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SummaryModel(
                name = "verification not finished",
                summary = summary(kycStatus = CustomerSummary.KycStatus.REQUIRED),
                expected = properties(kyc = "required", card = "not_started", account = "not_started"),
            ),
            SummaryModel(
                name = "under review",
                summary = summary(
                    kycStatus = CustomerSummary.KycStatus.IN_PROGRESS,
                    card = card(PaymentAccount.Activation.Status.PROCESSING, PaymentAccount.Activation.Step.AWAITING_KYC),
                    account = account(MoneyAccount.Activation.Status.PROCESSING, MoneyAccount.Activation.Step.AWAITING_KYC),
                ),
                expected = properties(kyc = "in_progress", card = "under_review", account = "under_review"),
            ),
            SummaryModel(
                name = "issuing",
                summary = summary(
                    card = card(PaymentAccount.Activation.Status.PROCESSING, PaymentAccount.Activation.Step.ISSUING),
                    account = account(MoneyAccount.Activation.Status.PROCESSING, MoneyAccount.Activation.Step.ISSUING),
                ),
                expected = properties(card = "issuing", account = "issuing"),
            ),
            SummaryModel(
                name = "waiting for the Plus deposit",
                summary = summary(
                    card = card(PaymentAccount.Activation.Status.PROCESSING, PaymentAccount.Activation.Step.AWAITING_DEPOSIT),
                ),
                expected = properties(card = "waiting_for_plus_deposit", account = "not_started"),
            ),
            SummaryModel(
                name = "Plus not funded",
                summary = summary(
                    card = card(
                        status = PaymentAccount.Activation.Status.FAILED,
                        failureReason = PaymentAccount.Activation.FailureReason.DEPOSIT_TIMEOUT,
                    ),
                ),
                expected = properties(card = "plus_not_funded", account = "not_started"),
            ),
            SummaryModel(
                name = "rejected by the issuer",
                summary = summary(
                    kycStatus = CustomerSummary.KycStatus.DECLINED,
                    card = card(
                        status = PaymentAccount.Activation.Status.FAILED,
                        failureReason = PaymentAccount.Activation.FailureReason.RAIN_REJECTED,
                    ),
                    account = account(
                        status = MoneyAccount.Activation.Status.FAILED,
                        failureReason = MoneyAccount.Activation.FailureReason.RAIN_REJECTED,
                    ),
                ),
                expected = properties(kyc = "declined", card = "verification_declined", account = "verification_declined"),
            ),
            SummaryModel(
                name = "technical failure",
                summary = summary(
                    card = card(
                        status = PaymentAccount.Activation.Status.FAILED,
                        failureReason = PaymentAccount.Activation.FailureReason.TECHNICAL,
                    ),
                    account = account(
                        status = MoneyAccount.Activation.Status.FAILED,
                        failureReason = MoneyAccount.Activation.FailureReason.TECHNICAL,
                    ),
                ),
                expected = properties(card = "failed", account = "failed"),
            ),
            SummaryModel(
                name = "active card, closed account",
                summary = summary(
                    card = card(PaymentAccount.Activation.Status.COMPLETED),
                    account = account(MoneyAccount.Activation.Status.DEACTIVATED),
                ),
                expected = properties(card = "active", account = "closed"),
            ),
            SummaryModel(
                name = "not available here",
                summary = summary(
                    card = card(PaymentAccount.Activation.Status.UNAVAILABLE),
                    account = account(MoneyAccount.Activation.Status.UNAVAILABLE),
                ),
                expected = properties(card = "not_available", account = "not_available"),
            ),
            SummaryModel(
                name = "unknown statuses",
                summary = summary(
                    kycStatus = CustomerSummary.KycStatus.UNKNOWN,
                    card = card(PaymentAccount.Activation.Status.UNKNOWN),
                    account = account(MoneyAccount.Activation.Status.UNKNOWN),
                ),
                expected = emptyMap(),
            ),
        )

        private fun properties(kyc: String = "approved", card: String, account: String) = mapOf(
            "pay_kyc_status" to kyc,
            "pay_card_status" to card,
            "pay_account_status" to account,
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class FromTariffPlan {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN tariff plan WHEN mapped THEN plan property matches the spec`(model: PlanModel) {
            // Act
            val actual = TangemPayUserProperties.of(model.tariffPlan)

            // Assert
            assertThat(actual).isEqualTo(mapOf("pay_plan" to model.expected))
        }

        private fun provideTestModels() = listOf(
            PlanModel(tariffPlan = customerPlan(isBasicTier = true), expected = "basic"),
            PlanModel(tariffPlan = customerPlan(isBasicTier = false), expected = "plus"),
            PlanModel(tariffPlan = null, expected = "none"),
        )

        private fun customerPlan(isBasicTier: Boolean): TangemPayCustomerTariffPlan = mockk {
            every { plan } returns TangemPayTariffPlan(
                id = "plan",
                tierId = if (isBasicTier) "BASIC" else "PLUS",
                isBasicTier = isBasicTier,
                name = "Plan",
                programName = "program",
                descriptionItems = emptyList(),
            )
        }
    }

    data class SummaryModel(val name: String, val summary: CustomerSummary, val expected: Map<String, String>) {
        override fun toString(): String = name
    }

    data class PlanModel(val tariffPlan: TangemPayCustomerTariffPlan?, val expected: String)

    private fun summary(
        kycStatus: CustomerSummary.KycStatus = CustomerSummary.KycStatus.APPROVED,
        card: PaymentAccount.Activation = card(PaymentAccount.Activation.Status.NOT_STARTED),
        account: MoneyAccount.Activation = account(MoneyAccount.Activation.Status.NOT_STARTED),
    ) = CustomerSummary(
        customerId = "customer",
        customerStatus = CustomerSummary.CustomerStatus.ACTIVE,
        isHidden = false,
        kycStatus = kycStatus,
        paymentAccount = PaymentAccount(
            id = null,
            activation = card,
            cards = CustomerSummary.Cards(active = 0, frozen = 0, pending = 0),
        ),
        moneyAccount = MoneyAccount(id = null, activation = account),
    )

    private fun card(
        status: PaymentAccount.Activation.Status,
        step: PaymentAccount.Activation.Step? = null,
        failureReason: PaymentAccount.Activation.FailureReason? = null,
    ) = PaymentAccount.Activation(status = status, step = step, failureReason = failureReason)

    private fun account(
        status: MoneyAccount.Activation.Status,
        step: MoneyAccount.Activation.Step? = null,
        failureReason: MoneyAccount.Activation.FailureReason? = null,
    ) = MoneyAccount.Activation(status = status, step = step, failureReason = failureReason)
}
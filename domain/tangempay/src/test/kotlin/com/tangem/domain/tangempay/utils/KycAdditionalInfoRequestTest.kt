package com.tangem.domain.tangempay.utils

import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.model.CustomerSummary
import com.tangem.domain.tangempay.model.CustomerSummary.CustomerStatus
import com.tangem.domain.tangempay.model.CustomerSummary.MoneyAccount
import com.tangem.domain.tangempay.model.CustomerSummary.PaymentAccount
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.MockAccounts
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import com.tangem.domain.tangempay.model.CustomerSummary.KycStatus as SummaryKycStatus

internal class KycAdditionalInfoRequestTest {

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CustomerSummaryPredicate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN customer summary WHEN isKycAdditionalInfoRequested THEN matches ACTIVE and IN_PROGRESS only`(
            model: SummaryModel,
        ) {
            // Arrange
            val summary = customerSummary(customerStatus = model.customerStatus, kycStatus = model.kycStatus)

            // Act
            val actual = summary.isKycAdditionalInfoRequested

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            SummaryModel(CustomerStatus.ACTIVE, SummaryKycStatus.IN_PROGRESS, expected = true),
            SummaryModel(CustomerStatus.ACTIVE, SummaryKycStatus.APPROVED, expected = false),
            SummaryModel(CustomerStatus.ACTIVE, SummaryKycStatus.REQUIRED, expected = false),
            SummaryModel(CustomerStatus.ACTIVE, SummaryKycStatus.DECLINED, expected = false),
            SummaryModel(CustomerStatus.ACTIVE, SummaryKycStatus.UNKNOWN, expected = false),
            SummaryModel(CustomerStatus.NEW, SummaryKycStatus.IN_PROGRESS, expected = false),
            SummaryModel(CustomerStatus.BLOCKED, SummaryKycStatus.IN_PROGRESS, expected = false),
            SummaryModel(CustomerStatus.FORMER, SummaryKycStatus.IN_PROGRESS, expected = false),
            SummaryModel(CustomerStatus.UNKNOWN, SummaryKycStatus.IN_PROGRESS, expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class CustomerInfoPredicate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN customer info WHEN isKycAdditionalInfoRequested THEN matches ACTIVE and PENDING only`(
            model: InfoModel,
        ) {
            // Arrange
            val info = customerInfo(state = model.state, kycStatus = model.kycStatus)

            // Act
            val actual = info.isKycAdditionalInfoRequested

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            InfoModel(CustomerInfo.State.ACTIVE, KycStatus.PENDING, expected = true),
            InfoModel(CustomerInfo.State.ACTIVE, KycStatus.REQUIRED, expected = false),
            InfoModel(CustomerInfo.State.ACTIVE, KycStatus.INIT, expected = false),
            InfoModel(CustomerInfo.State.ACTIVE, KycStatus.UNKNOWN, expected = false),
            InfoModel(CustomerInfo.State.ACTIVE, KycStatus.REJECTED, expected = false),
            InfoModel(CustomerInfo.State.ACTIVE, KycStatus.APPROVED, expected = false),
            InfoModel(CustomerInfo.State.NEW, KycStatus.PENDING, expected = false),
            InfoModel(CustomerInfo.State.BLOCKED, KycStatus.PENDING, expected = false),
            InfoModel(CustomerInfo.State.FORMER, KycStatus.PENDING, expected = false),
            InfoModel(CustomerInfo.State.IN_PROGRESS, KycStatus.PENDING, expected = false),
            InfoModel(CustomerInfo.State.UNDEFINED, KycStatus.PENDING, expected = false),
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class LoadedPaymentAccountPredicate {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN loaded payment account WHEN isKycAdditionalInfoActionRequired THEN flag set and not issuing only`(
            model: LoadedModel,
        ) {
            // Arrange
            val loaded = MockAccounts.createPaymentAccountStatus(
                isPaymentAccountIssuing = model.isPaymentAccountIssuing,
                isKycAdditionalInfoRequired = model.isKycAdditionalInfoRequired,
            ).value as PaymentAccountStatusValue.Loaded

            // Act
            val actual = loaded.isKycAdditionalInfoActionRequired

            // Assert
            assertThat(actual).isEqualTo(model.expected)
        }

        private fun provideTestModels() = listOf(
            LoadedModel(isKycAdditionalInfoRequired = true, isPaymentAccountIssuing = false, expected = true),
            LoadedModel(isKycAdditionalInfoRequired = true, isPaymentAccountIssuing = true, expected = false),
            LoadedModel(isKycAdditionalInfoRequired = false, isPaymentAccountIssuing = false, expected = false),
            LoadedModel(isKycAdditionalInfoRequired = false, isPaymentAccountIssuing = true, expected = false),
        )
    }

    private fun customerSummary(customerStatus: CustomerStatus, kycStatus: SummaryKycStatus) = CustomerSummary(
        customerId = CUSTOMER_ID,
        customerStatus = customerStatus,
        isHidden = false,
        kycStatus = kycStatus,
        paymentAccount = PaymentAccount(
            id = null,
            activation = PaymentAccount.Activation(
                status = PaymentAccount.Activation.Status.NOT_STARTED,
                step = null,
                failureReason = null,
            ),
            cards = CustomerSummary.Cards(active = 0, frozen = 0, pending = 0),
        ),
        moneyAccount = MoneyAccount(
            id = null,
            activation = MoneyAccount.Activation(
                status = MoneyAccount.Activation.Status.NOT_STARTED,
                step = null,
                failureReason = null,
            ),
        ),
    )

    private fun customerInfo(state: CustomerInfo.State, kycStatus: KycStatus) = CustomerInfo(
        customerId = CUSTOMER_ID,
        paymentAccount = null,
        productInstances = emptyList(),
        cards = emptyList(),
        kycStatus = kycStatus,
        state = state,
        fiatBalance = null,
        tariffPlan = null,
    )

    data class SummaryModel(
        val customerStatus: CustomerStatus,
        val kycStatus: SummaryKycStatus,
        val expected: Boolean,
    )

    data class InfoModel(
        val state: CustomerInfo.State,
        val kycStatus: KycStatus,
        val expected: Boolean,
    )

    data class LoadedModel(
        val isKycAdditionalInfoRequired: Boolean,
        val isPaymentAccountIssuing: Boolean,
        val expected: Boolean,
    )

    private companion object {
        const val CUSTOMER_ID = "customer_id"
    }
}
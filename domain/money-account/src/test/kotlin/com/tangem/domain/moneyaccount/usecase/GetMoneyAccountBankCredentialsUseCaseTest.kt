package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.pay.MockBankCredentials
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMoneyAccountBankCredentialsUseCaseTest {

    private val onboardingRepository: OnboardingRepository = mockk()

    private val useCase = GetMoneyAccountBankCredentialsUseCase(onboardingRepository = onboardingRepository)

    private val userWalletId = UserWalletId("011")

    @BeforeEach
    fun resetMocks() {
        clearMocks(onboardingRepository)
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns null
    }

    @Test
    fun `GIVEN saved profile with active account WHEN invoke THEN credentials requested without profile fetch`() =
        runTest {
            // Arrange
            every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns customerInfo()
            coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
                CREDENTIALS.right()

            // Act
            val actual = useCase(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(CREDENTIALS.right())
            coVerify(exactly = 0) { onboardingRepository.getCustomerInfo(any()) }
        }

    @Test
    fun `GIVEN no saved profile WHEN invoke THEN credentials of the fetched active product instance returned`() =
        runTest {
            // Arrange
            coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
            coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
                CREDENTIALS.right()

            // Act
            val actual = useCase(userWalletId)

            // Assert
            assertThat(actual).isEqualTo(CREDENTIALS.right())
            coVerify(exactly = 1) { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) }
        }

    @Test
    fun `GIVEN saved profile without money account WHEN invoke THEN the profile is fetched`() = runTest {
        // Arrange
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns customerInfo(instances = emptyList())
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns customerInfo().right()
        coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            CREDENTIALS.right()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS.right())
        coVerify(exactly = 1) { onboardingRepository.getCustomerInfo(userWalletId) }
    }

    @Test
    fun `GIVEN customer info fails WHEN invoke THEN its error returned without credentials request`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns VisaApiError.ServerUnavailable.left()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
        coVerify(exactly = 0) { onboardingRepository.getBankCredentials(any(), any()) }
    }

    @Test
    fun `GIVEN credentials request fails WHEN invoke THEN its error returned`() = runTest {
        // Arrange
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns customerInfo()
        coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            VisaApiError.ServerUnavailable.left()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ServerUnavailable.left())
    }

    @Test
    fun `GIVEN invoked twice WHEN invoke THEN credentials requested every time`() = runTest {
        // Arrange
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns customerInfo()
        coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            CREDENTIALS.right()

        // Act
        useCase(userWalletId)
        useCase(userWalletId)

        // Assert
        coVerify(exactly = 2) { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) }
    }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN no money account instance WHEN invoke THEN activation required error without credentials request`(
        info: CustomerInfo,
    ) = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns info.right()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(VisaApiError.ProductInstanceNotFoundActivationRequired.left())
        coVerify(exactly = 0) { onboardingRepository.getBankCredentials(any(), any()) }
    }

    @ParameterizedTest
    @MethodSource("provideProfilesWithInactiveAccount")
    fun `GIVEN money account instance in any status WHEN invoke THEN its credentials requested`(
        info: CustomerInfo,
    ) = runTest {
        // Arrange
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns info
        coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            CREDENTIALS.right()

        // Act
        val actual = useCase(userWalletId)

        // Assert
        assertThat(actual).isEqualTo(CREDENTIALS.right())
    }

    private fun provideTestModels() = listOf(
        customerInfo(instances = emptyList()),
        customerInfo(
            instances = listOf(
                accountInstance(paymentAccountType = CustomerInfo.ProductInstance.PaymentAccountType.COLLATERAL_CARD),
            ),
        ),
    )

    private fun provideProfilesWithInactiveAccount() = listOf(
        customerInfo(instances = listOf(accountInstance(status = CustomerInfo.ProductInstance.Status.DEACTIVATED))),
        customerInfo(instances = listOf(accountInstance(status = CustomerInfo.ProductInstance.Status.NEW))),
        customerInfo(state = CustomerInfo.State.FORMER),
    )

    private fun customerInfo(
        instances: List<CustomerInfo.ProductInstance> = listOf(accountInstance()),
        state: CustomerInfo.State = CustomerInfo.State.ACTIVE,
    ) = CustomerInfo(
        customerId = "customer",
        paymentAccount = null,
        productInstances = instances,
        cards = emptyList(),
        kycStatus = KycStatus.APPROVED,
        state = state,
        fiatBalance = null,
        tariffPlan = null,
    )

    private fun accountInstance(
        status: CustomerInfo.ProductInstance.Status = CustomerInfo.ProductInstance.Status.ACTIVE,
        paymentAccountType: CustomerInfo.ProductInstance.PaymentAccountType =
            CustomerInfo.ProductInstance.PaymentAccountType.COLLATERAL_BANKING_ACCOUNT,
    ) = CustomerInfo.ProductInstance(
        id = PRODUCT_INSTANCE_ID,
        cardId = "",
        frozenState = TangemPayCardFrozenState.Unfrozen,
        displayName = null,
        actualCardLimit = null,
        adminCardLimit = null,
        status = status,
        specificationDataType = CustomerInfo.ProductInstance.SpecificationDataType.ACCOUNT,
        paymentAccountType = paymentAccountType,
    )

    private companion object {
        const val PRODUCT_INSTANCE_ID = "pi_account"

        val CREDENTIALS = MockBankCredentials.bankCredentials()
    }
}
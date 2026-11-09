package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.tangempay.TangemPayCardFrozenState
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.tangempay.model.CustomerInfo
import com.tangem.domain.tangempay.repository.OnboardingRepository
import com.tangem.domain.tangempay.error.TangemPayApiError
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.mock.tangempay.MockBankCredentials
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
import org.junit.jupiter.params.provider.ValueSource

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

    @ParameterizedTest
    @MethodSource("provideRailsModels")
    fun `GIVEN saved profile with active account WHEN invoke THEN routes of the requested rails returned`(
        model: RailsModel,
    ) = runTest {
        // Arrange
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns customerInfo()
        coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            CREDENTIALS.right()
        coEvery { onboardingRepository.getAllBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            ROUTES.right()

        // Act
        val actual = useCase(userWalletId, includeAllRails = model.includeAllRails)

        // Assert
        assertThat(actual).isEqualTo(model.expected.right())
        coVerify(exactly = model.singleRouteRequests) {
            onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID)
        }
        coVerify(exactly = model.allRailsRequests) {
            onboardingRepository.getAllBankCredentials(userWalletId, PRODUCT_INSTANCE_ID)
        }
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
            val actual = useCase(userWalletId, includeAllRails = false)

            // Assert
            assertThat(actual).isEqualTo(listOf(CREDENTIALS).right())
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
        val actual = useCase(userWalletId, includeAllRails = false)

        // Assert
        assertThat(actual).isEqualTo(listOf(CREDENTIALS).right())
        coVerify(exactly = 1) { onboardingRepository.getCustomerInfo(userWalletId) }
    }

    @Test
    fun `GIVEN customer info fails WHEN invoke THEN its error returned without credentials request`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns TangemPayApiError.ServerUnavailable.left()

        // Act
        val actual = useCase(userWalletId, includeAllRails = false)

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.ServerUnavailable.left())
        coVerify(exactly = 0) { onboardingRepository.getBankCredentials(any(), any()) }
    }

    @ParameterizedTest
    @ValueSource(booleans = [false, true])
    fun `GIVEN credentials request fails WHEN invoke THEN its error returned`(includeAllRails: Boolean) = runTest {
        // Arrange
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns customerInfo()
        coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            TangemPayApiError.ServerUnavailable.left()
        coEvery { onboardingRepository.getAllBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            TangemPayApiError.ServerUnavailable.left()

        // Act
        val actual = useCase(userWalletId, includeAllRails = includeAllRails)

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.ServerUnavailable.left())
    }

    @Test
    fun `GIVEN invoked twice WHEN invoke THEN credentials requested every time`() = runTest {
        // Arrange
        every { onboardingRepository.getSavedCustomerInfo(userWalletId) } returns customerInfo()
        coEvery { onboardingRepository.getBankCredentials(userWalletId, PRODUCT_INSTANCE_ID) } returns
            CREDENTIALS.right()

        // Act
        useCase(userWalletId, includeAllRails = false)
        useCase(userWalletId, includeAllRails = false)

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
        val actual = useCase(userWalletId, includeAllRails = true)

        // Assert
        assertThat(actual).isEqualTo(TangemPayApiError.ProductInstanceNotFoundActivationRequired.left())
        coVerify(exactly = 0) { onboardingRepository.getBankCredentials(any(), any()) }
        coVerify(exactly = 0) { onboardingRepository.getAllBankCredentials(any(), any()) }
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
        val actual = useCase(userWalletId, includeAllRails = false)

        // Assert
        assertThat(actual).isEqualTo(listOf(CREDENTIALS).right())
    }

    private fun provideTestModels() = listOf(
        customerInfo(instances = emptyList()),
        customerInfo(
            instances = listOf(
                accountInstance(paymentAccountType = CustomerInfo.ProductInstance.PaymentAccountType.COLLATERAL_CARD),
            ),
        ),
    )

    private fun provideRailsModels() = listOf(
        RailsModel(
            includeAllRails = false,
            expected = listOf(CREDENTIALS),
            singleRouteRequests = 1,
            allRailsRequests = 0,
        ),
        RailsModel(includeAllRails = true, expected = ROUTES, singleRouteRequests = 0, allRailsRequests = 1),
    )

    data class RailsModel(
        val includeAllRails: Boolean,
        val expected: List<BankCredentials>,
        val singleRouteRequests: Int,
        val allRailsRequests: Int,
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
        val ROUTES = listOf(CREDENTIALS, MockBankCredentials.speiBankCredentials())
    }
}
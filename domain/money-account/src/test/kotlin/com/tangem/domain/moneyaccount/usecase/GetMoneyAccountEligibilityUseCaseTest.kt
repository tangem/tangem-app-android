package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.security.DeviceSecurityInfoProvider
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMoneyAccountEligibilityUseCaseTest {

    private val getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase = mockk()
    private val getMoneyAccountStatusUseCase: GetMoneyAccountStatusUseCase = mockk()
    private val deviceSecurityInfoProvider: DeviceSecurityInfoProvider = mockk()

    private val useCase = GetMoneyAccountEligibilityUseCase(
        getMoneyAccountSuitableWalletsUseCase = getMoneyAccountSuitableWalletsUseCase,
        getMoneyAccountStatusUseCase = getMoneyAccountStatusUseCase,
        deviceSecurityInfoProvider = deviceSecurityInfoProvider,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            getMoneyAccountSuitableWalletsUseCase,
            getMoneyAccountStatusUseCase,
            deviceSecurityInfoProvider,
        )
        every { deviceSecurityInfoProvider.isRooted } returns false
        every { deviceSecurityInfoProvider.isBootloaderUnlocked } returns false
        every { deviceSecurityInfoProvider.isXposed } returns false
    }

    @Test
    fun `GIVEN device is rooted WHEN invoke THEN returns NotAvailable`() = runTest {
        // Arrange
        every { deviceSecurityInfoProvider.isRooted } returns true

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
    }

    @Test
    fun `GIVEN no suitable wallets WHEN invoke THEN returns NotAvailable`() = runTest {
        // Arrange
        every { getMoneyAccountSuitableWalletsUseCase() } returns emptyList()

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
    }

    @Test
    fun `GIVEN wallet without money account WHEN invoke THEN returns Available with wallet`() = runTest {
        // Arrange
        val wallet = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
        mockStatus(wallet, MoneyAccountStatusValue.NotCreated)

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.Available(listOf(wallet)))
    }

    @Test
    fun `GIVEN status loading fails WHEN invoke THEN returns NotAvailable`() = runTest {
        // Arrange
        val wallet = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
        coEvery { getMoneyAccountStatusUseCase(wallet.walletId) } returns IllegalStateException().left()

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
    }

    @ParameterizedTest
    @MethodSource("provideNotEligibleStatuses")
    fun `GIVEN wallet with money account or without actual status WHEN invoke THEN returns NotAvailable`(
        status: MoneyAccountStatusValue,
    ) = runTest {
        // Arrange
        val wallet = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
        mockStatus(wallet, status)

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
    }

    @Test
    fun `GIVEN only some wallets have money account WHEN invoke THEN returns Available with the rest`() = runTest {
        // Arrange
        val walletWithAccount = mockWallet()
        val walletWithoutAccount = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(walletWithAccount, walletWithoutAccount)
        mockStatus(walletWithAccount, activeStatus())
        mockStatus(walletWithoutAccount, MoneyAccountStatusValue.NotCreated)

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.Available(listOf(walletWithoutAccount)))
    }

    private fun provideNotEligibleStatuses() = listOf(
        MoneyAccountStatusValue.Empty,
        MoneyAccountStatusValue.Loading,
        MoneyAccountStatusValue.Error.Unavailable,
        MoneyAccountStatusValue.Onboarding.CountryNotSupported,
        MoneyAccountStatusValue.Account.Deactivated(
            source = StatusSource.ACTUAL,
            balance = null,
            customerId = CUSTOMER_ID,
            productInstanceId = PRODUCT_INSTANCE_ID,
        ),
        MoneyAccountStatusValue.Onboarding.Issuing(
            source = StatusSource.ACTUAL,
            balance = null,
            customerId = CUSTOMER_ID,
            step = MoneyAccountStatusValue.IssuingStep.STARTING,
        ),
        MoneyAccountStatusValue.Onboarding.UnderReview(
            source = StatusSource.ACTUAL,
            balance = null,
            customerId = CUSTOMER_ID,
            kycStatus = mockk(),
        ),
        MoneyAccountStatusValue.Onboarding.VerificationDeclined(
            source = StatusSource.ACTUAL,
            balance = null,
            customerId = CUSTOMER_ID,
        ),
        MoneyAccountStatusValue.Onboarding.Activating(
            source = StatusSource.ACTUAL,
            balance = null,
            customerId = CUSTOMER_ID,
        ),
        activeStatus(),
    )

    private fun activeStatus() = MoneyAccountStatusValue.Account.Active(
        source = StatusSource.ACTUAL,
        balance = null,
        customerId = CUSTOMER_ID,
        productInstanceId = PRODUCT_INSTANCE_ID,
    )

    private fun mockStatus(wallet: UserWallet, status: MoneyAccountStatusValue) {
        coEvery { getMoneyAccountStatusUseCase(wallet.walletId) } returns status.right()
    }

    private fun mockWallet(): UserWallet {
        val id = mockk<UserWalletId>()
        return mockk { every { walletId } returns id }
    }

    private companion object {
        const val CUSTOMER_ID = "customer_id"
        const val PRODUCT_INSTANCE_ID = "product_instance_id"
    }
}
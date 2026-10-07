package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.account.models.AccountList
import com.tangem.domain.account.supplier.SingleAccountListSupplier
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.security.DeviceSecurityInfoProvider
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
internal class GetMoneyAccountEligibilityUseCaseTest {

    private val getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase = mockk()
    private val getMoneyAccountStatusUseCase: GetMoneyAccountStatusUseCase = mockk()
    private val deviceSecurityInfoProvider: DeviceSecurityInfoProvider = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val singleAccountListSupplier: SingleAccountListSupplier = mockk()

    private val useCase = GetMoneyAccountEligibilityUseCase(
        getMoneyAccountSuitableWalletsUseCase = getMoneyAccountSuitableWalletsUseCase,
        getMoneyAccountStatusUseCase = getMoneyAccountStatusUseCase,
        deviceSecurityInfoProvider = deviceSecurityInfoProvider,
        onboardingRepository = onboardingRepository,
        singleAccountListSupplier = singleAccountListSupplier,
    )

    @BeforeEach
    fun setup() {
        clearMocks(
            getMoneyAccountSuitableWalletsUseCase,
            getMoneyAccountStatusUseCase,
            deviceSecurityInfoProvider,
            onboardingRepository,
            singleAccountListSupplier,
        )
        coEvery { singleAccountListSupplier.getSyncOrNull(any<UserWalletId>()) } returns null
        coEvery { onboardingRepository.isTangemPayInitialDataProduced(any()) } returns true
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
    fun `GIVEN wallet without auth tokens WHEN invoke THEN returns Available without status loading`() = runTest {
        // Arrange
        val wallet = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
        coEvery { onboardingRepository.isTangemPayInitialDataProduced(wallet.walletId) } returns false

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.Available(listOf(wallet)))
        coVerify(exactly = 0) { getMoneyAccountStatusUseCase(any()) }
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

    @Test
    fun `GIVEN user currency on money account path WHEN invoke THEN wallet is excluded`() = runTest {
        // Arrange
        val walletWithConflict = mockWallet()
        val walletWithoutConflict = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(walletWithConflict, walletWithoutConflict)
        coEvery { onboardingRepository.isTangemPayInitialDataProduced(any()) } returns false
        mockCurrencies(walletWithConflict, derivationPath = "m/44'/60'/999998'/0/0")
        mockCurrencies(walletWithoutConflict, derivationPath = "m/44'/60'/1'/0/0")

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.Available(listOf(walletWithoutConflict)))
    }

    @Test
    fun `GIVEN only wallet has currency on money account path WHEN invoke THEN returns NotAvailable`() = runTest {
        // Arrange
        val wallet = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
        mockCurrencies(wallet, derivationPath = "m/44'/60'/999998'/0/0")

        // Act
        val result = useCase()

        // Assert
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
        coVerify(exactly = 0) { getMoneyAccountStatusUseCase(any()) }
    }

    private fun provideNotEligibleStatuses() = listOf(
        MoneyAccountStatusValue.Empty,
        MoneyAccountStatusValue.Loading,
        MoneyAccountStatusValue.Error.Unavailable,
        MoneyAccountStatusValue.Onboarding.CountryNotSupported(countryCode = null),
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

    private fun mockCurrencies(wallet: UserWallet, derivationPath: String) {
        val currency = mockk<CryptoCurrency> {
            every { network.derivationPath } returns Network.DerivationPath.Custom(value = derivationPath)
        }
        val accountList = mockk<AccountList> { every { flattenCurrencies() } returns listOf(currency) }
        coEvery { singleAccountListSupplier.getSyncOrNull(wallet.walletId) } returns accountList
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
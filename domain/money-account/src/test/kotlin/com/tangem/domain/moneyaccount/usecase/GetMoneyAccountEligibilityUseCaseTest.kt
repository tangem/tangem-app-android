package com.tangem.domain.moneyaccount.usecase

import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.moneyaccount.model.MoneyAccountEntryPoint
import com.tangem.domain.visa.error.VisaApiError
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

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class GetMoneyAccountEligibilityUseCaseTest {

    private val getMoneyAccountSuitableWalletsUseCase: GetMoneyAccountSuitableWalletsUseCase = mockk()
    private val onboardingRepository: OnboardingRepository = mockk()
    private val deviceSecurityInfoProvider: DeviceSecurityInfoProvider = mockk()

    private val useCase = GetMoneyAccountEligibilityUseCase(
        getMoneyAccountSuitableWalletsUseCase = getMoneyAccountSuitableWalletsUseCase,
        onboardingRepository = onboardingRepository,
        deviceSecurityInfoProvider = deviceSecurityInfoProvider,
    )

    @BeforeEach
    fun setup() {
        clearMocks(getMoneyAccountSuitableWalletsUseCase, onboardingRepository, deviceSecurityInfoProvider)
        every { deviceSecurityInfoProvider.isRooted } returns false
        every { deviceSecurityInfoProvider.isBootloaderUnlocked } returns false
        every { deviceSecurityInfoProvider.isXposed } returns false
    }

    @Test
    fun `GIVEN device is rooted WHEN invoke THEN returns NotAvailable`() = runTest {
        // GIVEN
        every { deviceSecurityInfoProvider.isRooted } returns true

        // WHEN
        val result = useCase(MoneyAccountEntryPoint.BANNER)

        // THEN
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
    }

    @Test
    fun `GIVEN no suitable wallets WHEN invoke THEN returns NotAvailable`() = runTest {
        // GIVEN
        every { getMoneyAccountSuitableWalletsUseCase() } returns emptyList()

        // WHEN
        val result = useCase(MoneyAccountEntryPoint.BANNER)

        // THEN
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
    }

    @Test
    fun `GIVEN entry point eligibility passes WHEN invoke THEN returns Available with all suitable wallets`() = runTest {
        // GIVEN
        val wallets = listOf(mockWallet(), mockWallet())
        every { getMoneyAccountSuitableWalletsUseCase() } returns wallets
        coEvery {
            onboardingRepository.getCustomerEligibility()
        } returns listOf(TangemPayEligibilityType.BANNER_VIRTUAL_ACCOUNT)

        // WHEN
        val result = useCase(MoneyAccountEntryPoint.BANNER)

        // THEN
        assertThat(result).isEqualTo(MoneyAccountEligibility.Available(wallets))
    }

    @Test
    fun `GIVEN null entry point AND any VA eligibility present WHEN invoke THEN returns Available`() = runTest {
        // GIVEN
        val wallets = listOf(mockWallet())
        every { getMoneyAccountSuitableWalletsUseCase() } returns wallets
        coEvery {
            onboardingRepository.getCustomerEligibility()
        } returns listOf(TangemPayEligibilityType.DEEPLINK_VIRTUAL_ACCOUNT)

        // WHEN
        val result = useCase(entryPoint = null)

        // THEN
        assertThat(result).isEqualTo(MoneyAccountEligibility.Available(wallets))
    }

    @Test
    fun `GIVEN no cached eligibility WHEN invoke THEN falls back to fetched eligibility`() = runTest {
        // GIVEN
        val wallets = listOf(mockWallet())
        every { getMoneyAccountSuitableWalletsUseCase() } returns wallets
        coEvery { onboardingRepository.getCustomerEligibility() } returns null
        coEvery {
            onboardingRepository.checkCustomerEligibility()
        } returns listOf(TangemPayEligibilityType.BANNER_VIRTUAL_ACCOUNT)

        // WHEN
        val result = useCase(MoneyAccountEntryPoint.BANNER)

        // THEN
        assertThat(result).isEqualTo(MoneyAccountEligibility.Available(wallets))
    }

    @Test
    fun `GIVEN cached eligibility empty WHEN invoke THEN eligibility is not fetched again`() = runTest {
        // GIVEN
        val wallet = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
        coEvery { onboardingRepository.getCustomerEligibility() } returns emptyList()
        coEvery { onboardingRepository.hasTangemPayInWallet(wallet.walletId) } returns false.right()

        // WHEN
        val result = useCase(MoneyAccountEntryPoint.BANNER)

        // THEN
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
        coVerify(exactly = 0) { onboardingRepository.checkCustomerEligibility() }
    }

    @Test
    fun `GIVEN eligibility fails AND wallet is existing customer WHEN invoke THEN returns Available with wallet`() =
        runTest {
            // GIVEN
            val wallet = mockWallet()
            every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
            coEvery { onboardingRepository.getCustomerEligibility() } returns listOf(TangemPayEligibilityType.BANNER)
            coEvery { onboardingRepository.hasTangemPayInWallet(wallet.walletId) } returns true.right()

            // WHEN
            val result = useCase(MoneyAccountEntryPoint.BANNER)

            // THEN
            assertThat(result).isEqualTo(MoneyAccountEligibility.Available(listOf(wallet)))
        }

    @Test
    fun `GIVEN eligibility fails AND wallet is not a customer WHEN invoke THEN returns NotAvailable`() = runTest {
        // GIVEN
        val wallet = mockWallet()
        every { getMoneyAccountSuitableWalletsUseCase() } returns listOf(wallet)
        coEvery { onboardingRepository.getCustomerEligibility() } returns listOf(TangemPayEligibilityType.BANNER)
        coEvery {
            onboardingRepository.hasTangemPayInWallet(wallet.walletId)
        } returns VisaApiError.NotFound.left()

        // WHEN
        val result = useCase(MoneyAccountEntryPoint.BANNER)

        // THEN
        assertThat(result).isEqualTo(MoneyAccountEligibility.NotAvailable)
    }

    @Test
    fun `GIVEN eligibility fails AND only some wallets are customers WHEN invoke THEN returns Available with customers`() =
        runTest {
            // GIVEN
            val customerWallet = mockWallet()
            val nonCustomerWallet = mockWallet()
            every {
                getMoneyAccountSuitableWalletsUseCase()
            } returns listOf(customerWallet, nonCustomerWallet)
            coEvery { onboardingRepository.getCustomerEligibility() } returns listOf(TangemPayEligibilityType.BANNER)
            coEvery { onboardingRepository.hasTangemPayInWallet(customerWallet.walletId) } returns true.right()
            coEvery { onboardingRepository.hasTangemPayInWallet(nonCustomerWallet.walletId) } returns false.right()

            // WHEN
            val result = useCase(MoneyAccountEntryPoint.BANNER)

            // THEN
            assertThat(result).isEqualTo(MoneyAccountEligibility.Available(listOf(customerWallet)))
        }

    private fun mockWallet(): UserWallet {
        val id = mockk<UserWalletId>()
        return mockk { every { walletId } returns id }
    }
}
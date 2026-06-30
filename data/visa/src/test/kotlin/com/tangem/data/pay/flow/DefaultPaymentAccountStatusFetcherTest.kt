package com.tangem.data.pay.flow

import arrow.core.Either
import arrow.core.left
import com.google.common.truth.Truth.assertThat
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.repository.*
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.virtualaccount.VirtualAccountFeatureToggles
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultPaymentAccountStatusFetcherTest {

    private val paymentAccountStatusesStore: PaymentAccountStatusesStore = mockk(relaxed = true)
    private val onboardingRepository: OnboardingRepository = mockk()
    private val customerOrderRepository: CustomerOrderRepository = mockk()
    private val deviceSecurity: DeviceSecurityInfoProvider = mockk(relaxed = true)
    private val dispatchers = TestingCoroutineDispatcherProvider()
    private val tangemPayCurrencyFactory: TangemPayCurrencyFactory = mockk()
    private val eligibilityManager: TangemPayEligibilityManager = mockk(relaxed = true)
    private val reissueCardRepository: TangemPayReissueCardRepository = mockk()
    private val singleQuoteSupplier: SingleQuoteStatusSupplier = mockk()
    private val closeCardRepository: TangemPayCloseCardRepository = mockk()
    private val cardDetailsRepository: TangemPayCardDetailsRepository = mockk()
    private val issueCardRepository: TangemPayIssueCardRepository = mockk()
    private val virtualAccountFeatureToggles: VirtualAccountFeatureToggles = mockk()

    private val fetcher = DefaultPaymentAccountStatusFetcher(
        paymentAccountStatusesStore = paymentAccountStatusesStore,
        onboardingRepository = onboardingRepository,
        customerOrderRepository = customerOrderRepository,
        deviceSecurity = deviceSecurity,
        dispatchers = dispatchers,
        tangemPayCurrencyFactory = tangemPayCurrencyFactory,
        eligibilityManager = eligibilityManager,
        reissueCardRepository = reissueCardRepository,
        singleQuoteSupplier = singleQuoteSupplier,
        closeCardRepository = closeCardRepository,
        cardDetailsRepository = cardDetailsRepository,
        issueCardRepository = issueCardRepository,
        virtualAccountFeatureToggles = virtualAccountFeatureToggles,
    )

    private val userWalletId = UserWalletId("011")
    private val params = PaymentAccountStatusFetcher.Params(userWalletId)

    private val bankCredentialsFixture = BankCredentials(
        type = "ACH",
        beneficiaryName = "Test Beneficiary",
        beneficiaryAddress = "123 Main St",
        beneficiaryBankName = "Test Bank",
        beneficiaryBankAddress = "456 Bank Ave",
        accountNumber = "1234567890",
        routingNumber = "021000021",
    )

    private val cardProductInstance = CustomerInfo.ProductInstance(
        id = "pi_card",
        cardId = "card_1",
        frozenState = TangemPayCardFrozenState.Unfrozen,
        displayName = null,
        actualCardLimit = null,
        adminCardLimit = null,
        status = CustomerInfo.ProductInstance.Status.ACTIVE,
        specificationDataType = CustomerInfo.ProductInstance.SpecificationDataType.CARD,
    )

    private val accountProductInstance = CustomerInfo.ProductInstance(
        id = "pi_account",
        cardId = "",
        frozenState = TangemPayCardFrozenState.Unfrozen,
        displayName = null,
        actualCardLimit = null,
        adminCardLimit = null,
        status = CustomerInfo.ProductInstance.Status.ACTIVE,
        specificationDataType = CustomerInfo.ProductInstance.SpecificationDataType.ACCOUNT,
    )

    private val cardInfo = CustomerInfo.CardInfo(
        cardId = "card_1",
        cardStatus = TangemPayCard.Status.ACTIVE,
        lastFourDigits = "1234",
        isPinSet = true,
    )

    private fun buildCustomerInfo(
        productInstances: List<CustomerInfo.ProductInstance> = listOf(cardProductInstance),
    ) = CustomerInfo(
        customerId = "cust_1",
        kycStatus = KycStatus.APPROVED,
        state = CustomerInfo.State.ACTIVE,
        fiatBalance = PaymentAccountStatusValue.FiatBalance(
            availableBalance = BigDecimal.TEN,
            currency = "USD",
        ),
        cryptoBalance = PaymentAccountStatusValue.CryptoBalance(
            id = "usdc",
            chainId = 137L,
            depositAddress = "0xdeposit",
            tokenContractAddress = "0xcontract",
            balance = BigDecimal.TEN,
        ),
        availableForWithdrawal = BigDecimal.TEN,
        cards = listOf(cardInfo),
        productInstances = productInstances,
        tariffPlan = null,
    )

    @BeforeEach
    fun setUp() {
        clearMocks(
            onboardingRepository,
            customerOrderRepository,
            tangemPayCurrencyFactory,
            reissueCardRepository,
            singleQuoteSupplier,
            closeCardRepository,
            cardDetailsRepository,
            issueCardRepository,
            virtualAccountFeatureToggles,
        )
        // Relaxed mocks don't need clearing — deviceSecurity, eligibilityManager, paymentAccountStatusesStore
        // are relaxed and consistent with their relaxed defaults (false, empty, etc.)
        clearMocks(paymentAccountStatusesStore, answers = false)
    }

    /**
     * Stubs the full happy-path chain up to [CustomerInfo.convertToContentState] so the fetcher
     * can produce a [PaymentAccountStatusValue.Loaded] result. Only the [customerInfo] parameter is
     * varied per test to exercise different VA on-ramp branches.
     */
    private suspend fun stubHappyPath(customerInfo: CustomerInfo) {
        val token: CryptoCurrency.Token = mockk(relaxed = true)

        coEvery { onboardingRepository.hasTangemPayInWallet(userWalletId) } returns Either.Right(true)
        coEvery { onboardingRepository.isTangemPayInitialDataProduced(userWalletId) } returns true
        coEvery { onboardingRepository.getOrderId(userWalletId) } returns null
        coEvery { onboardingRepository.getCustomerInfo(userWalletId) } returns Either.Right(customerInfo)

        coEvery { paymentAccountStatusesStore.getSyncOrNull(userWalletId) } returns null
        coEvery { paymentAccountStatusesStore.store(any(), any()) } just Runs

        every { tangemPayCurrencyFactory.create(userWalletId) } returns token

        coEvery { singleQuoteSupplier.getSyncOrNull(any()) } returns null

        coEvery { cardDetailsRepository.cardFrozenStateSync(any()) } returns TangemPayCardFrozenState.Unfrozen

        coEvery { closeCardRepository.getCloseOrderId(any(), any()) } returns Either.Right(null)
        coEvery { reissueCardRepository.getReissueOrderId(any(), any()) } returns Either.Right(null)

        coEvery { issueCardRepository.getIssueOrderIds(any()) } returns emptyList()
    }

    /** Collects all [AccountStatus.Payment] values stored via [PaymentAccountStatusesStore.store]. */
    private fun captureStoredStatuses(): MutableList<AccountStatus.Payment> {
        val captured = mutableListOf<AccountStatus.Payment>()
        coEvery { paymentAccountStatusesStore.store(any(), capture(captured)) } just Runs
        return captured
    }

    private fun MutableList<AccountStatus.Payment>.lastLoaded(): PaymentAccountStatusValue.Loaded {
        val loaded = filterIsInstance<AccountStatus.Payment>()
            .map { it.value }
            .filterIsInstance<PaymentAccountStatusValue.Loaded>()
            .lastOrNull()
        return requireNotNull(loaded) { "Expected at least one Loaded status to be stored; stored: ${map { it.value::class.simpleName }}" }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class `resolveVirtualAccountOnramp` {

        @Test
        fun `GIVEN feature toggle is off WHEN invoke THEN virtualAccount is null`() = runTest {
            // Arrange
            val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
            stubHappyPath(customerInfo)
            every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns false
            val storedStatuses = captureStoredStatuses()

            // Act
            fetcher.invoke(params)

            // Assert
            val loaded = storedStatuses.lastLoaded()
            assertThat(loaded.virtualAccount).isNull()
        }

        @Test
        fun `GIVEN toggle on and ACCOUNT instance with bank credentials WHEN invoke THEN virtualAccount is Available`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(
                    productInstances = listOf(cardProductInstance, accountProductInstance),
                )
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery {
                    onboardingRepository.getBankCredentials(userWalletId, "pi_account")
                } returns Either.Right(bankCredentialsFixture)
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                val loaded = storedStatuses.lastLoaded()
                assertThat(loaded.virtualAccount).isEqualTo(
                    VirtualAccountOnramp.Available(
                        productInstanceId = "pi_account",
                        bankCredentials = bankCredentialsFixture,
                    ),
                )
            }

        @Test
        fun `GIVEN toggle on and ACCOUNT instance but bank credentials fetch fails WHEN invoke THEN virtualAccount is null`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(
                    productInstances = listOf(cardProductInstance, accountProductInstance),
                )
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery {
                    onboardingRepository.getBankCredentials(userWalletId, "pi_account")
                } returns VisaApiError.UnknownWithoutCode.left()
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                val loaded = storedStatuses.lastLoaded()
                assertThat(loaded.virtualAccount).isNull()
            }

        @Test
        fun `GIVEN toggle on and no ACCOUNT instance and customer is eligible WHEN invoke THEN virtualAccount is Eligible`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery {
                    onboardingRepository.fetchCustomerEligibility(userWalletId)
                } returns Either.Right(listOf(TangemPayEligibilityType.VISA_VIRTUAL_ACCOUNT))
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                val loaded = storedStatuses.lastLoaded()
                assertThat(loaded.virtualAccount).isEqualTo(VirtualAccountOnramp.Eligible)
            }

        @Test
        fun `GIVEN toggle on and no ACCOUNT instance and eligibility fetch fails WHEN invoke THEN virtualAccount is null`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery {
                    onboardingRepository.fetchCustomerEligibility(userWalletId)
                } returns VisaApiError.UnknownWithoutCode.left()
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                val loaded = storedStatuses.lastLoaded()
                assertThat(loaded.virtualAccount).isNull()
            }
    }
}
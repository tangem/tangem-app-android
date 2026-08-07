package com.tangem.data.pay.flow

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.pay.converter.PaymentAccountStatusValueDMConverter
import com.tangem.data.pay.store.PaymentAccountStatusesStore
import com.tangem.data.pay.store.WalletIdWithPaymentStatus
import com.tangem.data.pay.store.WalletIdWithPaymentStatusDM
import com.tangem.datasource.local.datastore.RuntimeSharedStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.BankCredentials
import com.tangem.domain.models.account.PaymentAccountStatusValue
import com.tangem.domain.models.account.TangemPayCustomerTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlan
import com.tangem.domain.models.account.TangemPayTariffPlanState
import com.tangem.domain.models.account.VirtualAccountOnramp
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCard
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayCardState
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.pay.TangemPayCurrencyFactory
import com.tangem.domain.pay.TangemPayEligibilityManager
import com.tangem.domain.pay.flow.PaymentAccountStatusFetcher
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.OrderData
import com.tangem.domain.pay.model.OrderStatus
import com.tangem.domain.pay.repository.*
import com.tangem.domain.pay.usecase.GetTangemPayTariffPlanStateUseCase
import com.tangem.domain.quotes.single.SingleQuoteStatusSupplier
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.tangempay.TangemPayFeatureToggles
import com.tangem.features.virtualaccount.VirtualAccountFeatureToggles
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.test.core.TestAppCoroutineScope
import com.tangem.test.core.datastore.MockStateDataStore
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
    private val tangemPayFeatureToggles: TangemPayFeatureToggles = mockk()
    private val getTangemPayTariffPlanStateUseCase: GetTangemPayTariffPlanStateUseCase = mockk()

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
        tangemPayFeatureToggles = tangemPayFeatureToggles,
        getTangemPayTariffPlanStateUseCase = getTangemPayTariffPlanStateUseCase,
    )

    private val userWalletId = UserWalletId("011")
    private val params = PaymentAccountStatusFetcher.Params(userWalletId)

    private companion object {
        const val STALE_ORDER_ID = "order-gone"
    }

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
        images = emptyList(),
    )

    private val basicPlan = TangemPayTariffPlan(
        id = "plan_basic",
        tierId = "BASIC",
        isBasicTier = true,
        name = "Basic",
        programName = "program_basic",
        descriptionItems = emptyList(),
        images = emptyList(),
        fees = emptyList(),
    )

    private val customerTariffPlan = TangemPayCustomerTariffPlan(
        status = TangemPayCustomerTariffPlan.Status.ACTIVE,
        source = TangemPayCustomerTariffPlan.Source.CUSTOMER,
        plan = basicPlan,
        nextBillingAt = null,
        pendingPlan = null,
        pendingTransitionAt = null,
    )

    private fun buildCustomerInfo(
        productInstances: List<CustomerInfo.ProductInstance> = listOf(cardProductInstance),
        fiatBalance: PaymentAccountStatusValue.FiatBalance? = PaymentAccountStatusValue.FiatBalance(
            availableBalance = BigDecimal.TEN,
            currency = "USD",
        ),
        cryptoBalance: PaymentAccountStatusValue.CryptoBalance? = PaymentAccountStatusValue.CryptoBalance(
            id = "usdc",
            chainId = 137L,
            depositAddress = "0xdeposit",
            tokenContractAddress = "0xcontract",
            balance = BigDecimal.TEN,
        ),
        tariffPlan: TangemPayCustomerTariffPlan? = null,
    ) = CustomerInfo(
        customerId = "cust_1",
        kycStatus = KycStatus.APPROVED,
        state = CustomerInfo.State.ACTIVE,
        fiatBalance = fiatBalance,
        cryptoBalance = cryptoBalance,
        availableForWithdrawal = BigDecimal.TEN,
        cards = listOf(cardInfo),
        productInstances = productInstances,
        tariffPlan = tariffPlan,
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
            tangemPayFeatureToggles,
            getTangemPayTariffPlanStateUseCase,
        )
        // Relaxed mocks don't need clearing — deviceSecurity, eligibilityManager, paymentAccountStatusesStore
        // are relaxed and consistent with their relaxed defaults (false, empty, etc.)
        clearMocks(paymentAccountStatusesStore, answers = false)
        // Tiers off by default — legacy auto-order-creation behavior. Individual tests override.
        every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns false
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
        return requireNotNull(
            loaded,
        ) { "Expected at least one Loaded status to be stored; stored: ${map { it.value::class.simpleName }}" }
    }

    /** Builds a [PaymentAccountStatusValue.Loaded] fixture with every field defaulted except [virtualAccount]. */
    private fun loadedFixture(virtualAccount: VirtualAccountOnramp? = null): PaymentAccountStatusValue.Loaded {
        val token: CryptoCurrency.Token = mockk(relaxed = true)
        return PaymentAccountStatusValue.Loaded(
            source = StatusSource.ACTUAL,
            customerId = "cust_1",
            depositAddress = "0xdeposit",
            balance = PaymentAccountStatusValue.Balance(
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
            ),
            cryptoCurrency = token,
            cards = emptyList(),
            fiatRate = null,
            error = null,
            virtualAccount = virtualAccount,
            tariffPlan = null
        )
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class ResolveVirtualAccountOnramp {

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
                coEvery { onboardingRepository.clearVirtualAccountOrderId(userWalletId) } just Runs
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
                coVerify(exactly = 1) { onboardingRepository.clearVirtualAccountOrderId(userWalletId) }
            }

        @Test
        fun `GIVEN toggle on and ACCOUNT instance but bank credentials fetch fails WHEN invoke THEN virtualAccount is Error`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(
                    productInstances = listOf(cardProductInstance, accountProductInstance),
                )
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery { onboardingRepository.clearVirtualAccountOrderId(userWalletId) } just Runs
                coEvery {
                    onboardingRepository.getBankCredentials(userWalletId, "pi_account")
                } returns VisaApiError.UnknownWithoutCode.left()
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                val loaded = storedStatuses.lastLoaded()
                assertThat(loaded.virtualAccount).isEqualTo(VirtualAccountOnramp.BankCredentialsError)
                coVerify(exactly = 1) { onboardingRepository.clearVirtualAccountOrderId(userWalletId) }
            }

        @Test
        fun `GIVEN toggle on and no ACCOUNT instance and customer is eligible WHEN invoke THEN virtualAccount is Eligible`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns null
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
                coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns null
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

        @Test
        fun `GIVEN no instance and va order PROCESSING WHEN invoke THEN virtualAccount is Processing`() = runTest {
            // Arrange
            val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
            stubHappyPath(customerInfo)
            every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
            coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns "va-1"
            coEvery {
                customerOrderRepository.getOrderData(userWalletId, "va-1")
            } returns OrderData(customerId = "c1", status = OrderStatus.PROCESSING, withdrawTxHash = null).right()
            val storedStatuses = captureStoredStatuses()

            // Act
            fetcher.invoke(params)

            // Assert
            assertThat(storedStatuses.lastLoaded().virtualAccount).isEqualTo(VirtualAccountOnramp.Processing)
        }

        @Test
        fun `GIVEN no instance and va order COMPLETED but instance absent WHEN invoke THEN virtualAccount is Processing`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns "va-1"
                coEvery {
                    customerOrderRepository.getOrderData(userWalletId, "va-1")
                } returns OrderData(customerId = "c1", status = OrderStatus.COMPLETED, withdrawTxHash = null).right()
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                assertThat(storedStatuses.lastLoaded().virtualAccount).isEqualTo(VirtualAccountOnramp.Processing)
            }

        @Test
        fun `GIVEN no instance and va getOrderData fails WHEN invoke THEN virtualAccount is Processing`() = runTest {
            // Arrange
            val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
            stubHappyPath(customerInfo)
            every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
            coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns "va-1"
            coEvery {
                customerOrderRepository.getOrderData(userWalletId, "va-1")
            } returns VisaApiError.UnknownWithoutCode.left()
            val storedStatuses = captureStoredStatuses()

            // Act
            fetcher.invoke(params)

            // Assert
            assertThat(storedStatuses.lastLoaded().virtualAccount).isEqualTo(VirtualAccountOnramp.Processing)
        }

        @Test
        fun `GIVEN no instance and va order CANCELED WHEN invoke THEN id cleared and falls back to eligibility`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns "va-1"
                coEvery {
                    customerOrderRepository.getOrderData(userWalletId, "va-1")
                } returns OrderData(customerId = "c1", status = OrderStatus.CANCELED, withdrawTxHash = null).right()
                coEvery { onboardingRepository.clearVirtualAccountOrderId(userWalletId) } just Runs
                coEvery {
                    onboardingRepository.fetchCustomerEligibility(userWalletId)
                } returns Either.Right(listOf(TangemPayEligibilityType.VISA_VIRTUAL_ACCOUNT))
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                coVerify(exactly = 1) { onboardingRepository.clearVirtualAccountOrderId(userWalletId) }
                assertThat(storedStatuses.lastLoaded().virtualAccount).isEqualTo(VirtualAccountOnramp.Eligible)
            }

        @Test
        fun `GIVEN no instance and va order is gone on backend WHEN invoke THEN id cleared and falls back to eligibility`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = listOf(cardProductInstance))
                stubHappyPath(customerInfo)
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns true
                coEvery { onboardingRepository.getVirtualAccountOrderId(userWalletId) } returns "va-1"
                coEvery {
                    customerOrderRepository.getOrderData(userWalletId, "va-1")
                } returns VisaApiError.OrderNotFound.left()
                coEvery { onboardingRepository.clearVirtualAccountOrderId(userWalletId) } just Runs
                coEvery {
                    onboardingRepository.fetchCustomerEligibility(userWalletId)
                } returns Either.Right(listOf(TangemPayEligibilityType.VISA_VIRTUAL_ACCOUNT))
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                coVerify(exactly = 1) { onboardingRepository.clearVirtualAccountOrderId(userWalletId) }
                assertThat(storedStatuses.lastLoaded().virtualAccount).isEqualTo(VirtualAccountOnramp.Eligible)
            }
    }

    /**
     * A locally persisted `orderId` can go stale (the order is removed on the backend) while the wallet is still a
     * valid Paera customer. Before the fix, the resulting 404 was read as "not a Paera customer" and collapsed the
     * whole Tangem Pay block to [PaymentAccountStatusValue.Empty] on every refresh, with the stale id never
     * cleared — so the card stayed invisible until app reinstall.
     */
    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class StaleOrderIdRecovery {

        @Test
        fun `GIVEN persisted order is gone on backend WHEN invoke THEN id cleared and status rebuilt from customer info`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = emptyList())
                stubHappyPath(customerInfo)
                coEvery { onboardingRepository.getOrderId(userWalletId) } returns STALE_ORDER_ID
                coEvery {
                    customerOrderRepository.getOrderData(userWalletId, STALE_ORDER_ID)
                } returns VisaApiError.OrderNotFound.left()
                coEvery { onboardingRepository.clearOrderId(userWalletId) } just Runs
                coEvery { onboardingRepository.createOrder(userWalletId) } returns "order-2".right()
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                coVerify(exactly = 1) { onboardingRepository.clearOrderId(userWalletId) }
                assertThat(storedStatuses.map { it.value }.last())
                    .isEqualTo(PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL))
            }

        @Test
        fun `GIVEN order lookup fails transiently WHEN invoke THEN id kept and status is Unavailable`() = runTest {
            // Arrange
            val customerInfo = buildCustomerInfo(productInstances = emptyList())
            stubHappyPath(customerInfo)
            coEvery { onboardingRepository.getOrderId(userWalletId) } returns STALE_ORDER_ID
            coEvery {
                customerOrderRepository.getOrderData(userWalletId, STALE_ORDER_ID)
            } returns VisaApiError.ServerUnavailable.left()
            val storedStatuses = captureStoredStatuses()

            // Act
            fetcher.invoke(params)

            // Assert
            coVerify(exactly = 0) { onboardingRepository.clearOrderId(userWalletId) }
            assertThat(storedStatuses.map { it.value }.last())
                .isEqualTo(PaymentAccountStatusValue.Error.Unavailable)
        }
    }

    /**
     * [markVirtualAccountProcessing] now delegates entirely to the atomic
     * [PaymentAccountStatusesStore.markVirtualAccountProcessing] (read-modify-write happens inside the store's
     * `runtimeStore.update` lambda, see [REDACTED_TASK_KEY] review). A mocked store can't exercise that internal branching,
     * so these tests wire the fetcher to a real [PaymentAccountStatusesStore] (real [RuntimeSharedStore] +
     * in-memory persistence fake) and assert on its resulting state — exercising the delegate wiring and the
     * store's atomic logic together.
     */
    @Nested
    inner class MarkVirtualAccountProcessing {

        private val runtimeStore = RuntimeSharedStore<WalletIdWithPaymentStatus>()
        private val persistenceStore = MockStateDataStore<WalletIdWithPaymentStatusDM>(default = emptyMap())
        private val converter: PaymentAccountStatusValueDMConverter = mockk(relaxed = true)

        private val realStore = PaymentAccountStatusesStore(
            runtimeStore = runtimeStore,
            persistenceDataStore = persistenceStore,
            converter = converter,
            scope = TestAppCoroutineScope(),
        )

        private val realFetcher = DefaultPaymentAccountStatusFetcher(
            paymentAccountStatusesStore = realStore,
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
            tangemPayFeatureToggles = tangemPayFeatureToggles,
            getTangemPayTariffPlanStateUseCase = getTangemPayTariffPlanStateUseCase,
        )

        private val account = Account.Payment(userWalletId = userWalletId)

        @Test
        fun `GIVEN cached Loaded with eligible onramp WHEN mark THEN virtualAccount becomes Processing`() = runTest {
            // Arrange
            val loaded = loadedFixture(virtualAccount = VirtualAccountOnramp.Eligible)
            realStore.store(userWalletId, AccountStatus.Payment(account = account, value = loaded))

            // Act
            realFetcher.markVirtualAccountProcessing(userWalletId)

            // Assert
            val updated = realStore.getSyncOrNull(userWalletId)?.value
            assertThat(updated).isEqualTo(loaded.copy(virtualAccount = VirtualAccountOnramp.Processing))
        }

        @Test
        fun `GIVEN no cached value WHEN mark THEN store stays empty`() = runTest {
            // Act
            realFetcher.markVirtualAccountProcessing(userWalletId)

            // Assert
            assertThat(realStore.getSyncOrNull(userWalletId)).isNull()
        }

        @Test
        fun `GIVEN cached non-Loaded value WHEN mark THEN value stays unchanged`() = runTest {
            // Arrange
            val issuingCard = PaymentAccountStatusValue.IssuingCard(source = StatusSource.ACTUAL)
            realStore.store(userWalletId, AccountStatus.Payment(account = account, value = issuingCard))

            // Act
            realFetcher.markVirtualAccountProcessing(userWalletId)

            // Assert
            assertThat(realStore.getSyncOrNull(userWalletId)?.value).isEqualTo(issuingCard)
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class TiersPlanSelection {

        @Test
        fun `GIVEN tiers on and KYC approved and no plan order WHEN invoke THEN stores AwaitingPlanSelection`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(
                    productInstances = emptyList(),
                    fiatBalance = null,
                    cryptoBalance = null,
                    tariffPlan = customerTariffPlan,
                )
                stubHappyPath(customerInfo)
                every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns true
                coEvery { issueCardRepository.getIssueOrderIds(userWalletId) } returns emptyList()
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                assertThat(storedStatuses.last().value)
                    .isInstanceOf(PaymentAccountStatusValue.AwaitingPlanSelection::class.java)
                coVerify(exactly = 0) { onboardingRepository.createOrder(userWalletId) }
            }

        @Test
        fun `GIVEN tiers on and fallback plan is missing WHEN invoke THEN stores IssuingCard without order`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(
                    productInstances = emptyList(),
                    fiatBalance = null,
                    cryptoBalance = null,
                    tariffPlan = null,
                )
                stubHappyPath(customerInfo)
                every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns true
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                assertThat(storedStatuses.last().value)
                    .isInstanceOf(PaymentAccountStatusValue.IssuingCard::class.java)
                coVerify(exactly = 0) { onboardingRepository.createOrder(userWalletId) }
            }

        @Test
        fun `GIVEN tiers on and plan selected but no balance yet WHEN invoke THEN stores Inactive`() = runTest {
            // Arrange
            val customerInfo = buildCustomerInfo(
                productInstances = emptyList(),
                fiatBalance = null,
                cryptoBalance = null,
                tariffPlan = customerTariffPlan,
            )
            stubHappyPath(customerInfo)
            every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns true
            coEvery { issueCardRepository.getIssueOrderIds(userWalletId) } returns listOf("order_1")
            coEvery {
                getTangemPayTariffPlanStateUseCase(userWalletId = userWalletId, tariff = customerTariffPlan)
            } returns TangemPayTariffPlanState(tariff = customerTariffPlan, order = null)
            val storedStatuses = captureStoredStatuses()

            // Act
            fetcher.invoke(params)

            // Assert
            assertThat(storedStatuses.last().value)
                .isInstanceOf(PaymentAccountStatusValue.Inactive::class.java)
            coVerify(exactly = 0) { onboardingRepository.createOrder(userWalletId) }
        }

        @Test
        fun `GIVEN tiers off and KYC approved without card WHEN invoke THEN creates order and stays issuing`() = runTest {
            // Arrange
            val customerInfo = buildCustomerInfo(productInstances = emptyList())
            stubHappyPath(customerInfo)
            every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns false
            coEvery { onboardingRepository.createOrder(userWalletId) } returns Either.Right("order_1")
            val storedStatuses = captureStoredStatuses()

            // Act
            fetcher.invoke(params)

            // Assert
            assertThat(storedStatuses.last().value)
                .isInstanceOf(PaymentAccountStatusValue.IssuingCard::class.java)
            coVerify(exactly = 1) { onboardingRepository.createOrder(userWalletId) }
        }

        @Test
        fun `GIVEN tiers off and balance without instances but local issue order WHEN invoke THEN stores IssuingCard`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = emptyList())
                stubHappyPath(customerInfo)
                every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns false
                coEvery { issueCardRepository.getIssueOrderIds(userWalletId) } returns listOf("order_1")
                coEvery { onboardingRepository.createOrder(userWalletId) } returns Either.Right("order_1")
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                assertThat(storedStatuses.last().value)
                    .isInstanceOf(PaymentAccountStatusValue.IssuingCard::class.java)
            }

        @Test
        fun `GIVEN tiers on and balance without instances but local issue order WHEN invoke THEN stores Loaded with placeholder`() =
            runTest {
                // Arrange
                val customerInfo = buildCustomerInfo(productInstances = emptyList())
                stubHappyPath(customerInfo)
                every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns true
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns false
                coEvery { issueCardRepository.getIssueOrderIds(userWalletId) } returns listOf("order_1")
                coEvery {
                    cardDetailsRepository.getOrderInfo(userWalletId, "order_1")
                } returns VisaApiError.UnknownWithoutCode.left()
                val storedStatuses = captureStoredStatuses()

                // Act
                fetcher.invoke(params)

                // Assert
                val loaded = storedStatuses.lastLoaded()
                assertThat(loaded.cards).hasSize(1)
                assertThat(loaded.cards.single().state).isEqualTo(TangemPayCardState.Issuing)
            }

        @Test
        fun `GIVEN local issue order missing on backend WHEN invoke THEN placeholder dropped and order forgotten`() =
            runTest {
                // GIVEN
                val customerInfo = buildCustomerInfo()
                stubHappyPath(customerInfo)
                every { tangemPayFeatureToggles.isTiersPlusPlanEnabled } returns true
                every { virtualAccountFeatureToggles.isVaMvp0Enabled } returns false
                coEvery { issueCardRepository.getIssueOrderIds(userWalletId) } returns listOf("order_gone")
                coEvery {
                    cardDetailsRepository.getOrderInfo(userWalletId, "order_gone")
                } returns VisaApiError.OrderNotFound.left()
                coEvery { issueCardRepository.removeIssueOrderId(userWalletId, "order_gone") } just Runs
                val storedStatuses = captureStoredStatuses()

                // WHEN
                fetcher.invoke(params)

                // THEN
                val loaded = storedStatuses.lastLoaded()
                assertThat(loaded.cards.map { it.state }).doesNotContain(TangemPayCardState.Issuing)
                coVerify(exactly = 1) { issueCardRepository.removeIssueOrderId(userWalletId, "order_gone") }
            }
    }
}
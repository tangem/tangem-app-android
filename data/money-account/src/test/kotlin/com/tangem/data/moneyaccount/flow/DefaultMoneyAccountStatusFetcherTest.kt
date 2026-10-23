package com.tangem.data.moneyaccount.flow

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.data.moneyaccount.store.MoneyAccountStatusesStore
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.Account
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.account.MoneyAccountStatusValue
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.kyc.KycStatus
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.domain.models.pay.TangemPayEligibilityType
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.currency.MoneyAccountCurrencyFactory
import com.tangem.domain.moneyaccount.flow.MoneyAccountStatusFetcher
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountBalanceUseCase
import com.tangem.domain.pay.model.CachePolicy
import com.tangem.domain.pay.model.CustomerInfo
import com.tangem.domain.pay.model.CustomerSummary
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.pay.usecase.GetCustomerSummaryUseCase
import com.tangem.domain.pay.usecase.ResolveMoneyAccountStatusFromSummaryUseCase
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.security.DeviceSecurityInfoProvider
import com.tangem.test.core.ProvideTestModels
import com.tangem.utils.coroutines.TestingCoroutineDispatcherProvider
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
import java.math.BigDecimal

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class DefaultMoneyAccountStatusFetcherTest {

    private val moneyAccountStatusesStore: MoneyAccountStatusesStore = mockk(relaxed = true)
    private val onboardingRepository: OnboardingRepository = mockk()
    private val deviceSecurity: DeviceSecurityInfoProvider = mockk(relaxed = true)
    private val getMoneyAccountBalanceUseCase: GetMoneyAccountBalanceUseCase = mockk()
    private val moneyAccountCurrencyFactory: MoneyAccountCurrencyFactory = mockk()
    private val cryptoCurrency: CryptoCurrency.Token = mockk()
    private val resolveMoneyAccountStatusFromSummaryUseCase: ResolveMoneyAccountStatusFromSummaryUseCase = mockk()
    private val getCustomerSummaryUseCase: GetCustomerSummaryUseCase = mockk()
    private val summary: CustomerSummary = mockk()

    private val fetcher = DefaultMoneyAccountStatusFetcher(
        moneyAccountStatusesStore = moneyAccountStatusesStore,
        onboardingRepository = onboardingRepository,
        dispatchers = TestingCoroutineDispatcherProvider(),
        getMoneyAccountBalanceUseCase = getMoneyAccountBalanceUseCase,
        deviceSecurity = deviceSecurity,
        moneyAccountCurrencyFactory = moneyAccountCurrencyFactory,
        resolveMoneyAccountStatusFromSummaryUseCase = resolveMoneyAccountStatusFromSummaryUseCase,
        getCustomerSummaryUseCase = getCustomerSummaryUseCase,
    )

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            moneyAccountStatusesStore,
            onboardingRepository,
            getMoneyAccountBalanceUseCase,
            moneyAccountCurrencyFactory,
            resolveMoneyAccountStatusFromSummaryUseCase,
            getCustomerSummaryUseCase,
        )
        every { deviceSecurity.isRooted } returns false
        every { moneyAccountCurrencyFactory.create(USER_WALLET_ID) } returns cryptoCurrency
        coEvery { getMoneyAccountBalanceUseCase(USER_WALLET_ID, cryptoCurrency) } returns IllegalStateException().left()
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.REFRESH) } returns summary.right()
        every { resolveMoneyAccountStatusFromSummaryUseCase(summary) } returns null
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo().right()
        coEvery {
            onboardingRepository.fetchCustomerEligibility(USER_WALLET_ID)
        } returns VisaApiError.ServerUnavailable.left()
    }

    @Test
    fun `GIVEN summary resolves status WHEN invoke THEN it is stored without customer me`() = runTest {
        // Arrange
        val resolved = MoneyAccountStatusValue.Onboarding.Activating(
            source = StatusSource.ACTUAL,
            customerId = CUSTOMER_ID,
        )
        every { resolveMoneyAccountStatusFromSummaryUseCase(summary) } returns resolved

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(resolved)
        coVerify(exactly = 0) { onboardingRepository.getCustomerInfo(any()) }
    }

    @Test
    fun `GIVEN summary request fails WHEN invoke THEN empty is stored without customer me`() = runTest {
        // Arrange
        coEvery { getCustomerSummaryUseCase(USER_WALLET_ID, CachePolicy.REFRESH) } returns VisaApiError.Unspecified.left()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(MoneyAccountStatusValue.Empty)
        coVerify(exactly = 0) { resolveMoneyAccountStatusFromSummaryUseCase(any()) }
        coVerify(exactly = 0) { onboardingRepository.getCustomerInfo(any()) }
    }

    @Test
    fun `GIVEN customer info is unavailable WHEN invoke THEN empty is stored`() = runTest {
        // Arrange
        coEvery {
            onboardingRepository.getCustomerInfo(USER_WALLET_ID)
        } returns VisaApiError.ServerUnavailable.left()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(MoneyAccountStatusValue.Empty)
    }

    @Test
    fun `GIVEN exposed device WHEN invoke THEN exposed device is stored without requests`() = runTest {
        // Arrange
        every { deviceSecurity.isRooted } returns true

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(MoneyAccountStatusValue.Error.ExposedDevice)
        coVerify(exactly = 0) { getCustomerSummaryUseCase(any(), any()) }
    }

    @Test
    fun `GIVEN former customer without account WHEN invoke THEN empty is stored`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            state = CustomerInfo.State.FORMER,
        ).right()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(MoneyAccountStatusValue.Empty)
    }

    @Test
    fun `GIVEN active product instance WHEN invoke THEN active account with balance is stored`() = runTest {
        // Arrange
        val balance = MoneyAccountStatusValue.Balance(
            cryptoAmount = BigDecimal.TEN,
            depositAddress = "0xdeposit",
            currencyCode = "USD",
            fiatRate = BigDecimal.ONE,
        )
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.ACTIVE,
        ).right()
        coEvery { getMoneyAccountBalanceUseCase(USER_WALLET_ID, cryptoCurrency) } returns balance.right()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(
            MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                cryptoCurrency = cryptoCurrency,
                balance = balance,
                customerId = CUSTOMER_ID,
                productInstanceId = PRODUCT_INSTANCE_ID,
            ),
        )
    }

    @Test
    fun `GIVEN active product instance AND balance failed WHEN invoke THEN active account without balance is stored`() =
        runTest {
            // Arrange
            coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
                accountStatus = CustomerInfo.ProductInstance.Status.ACTIVE,
            ).right()

            // Act
            fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

            // Assert
            verifyStored(
                MoneyAccountStatusValue.Account.Active(
                    source = StatusSource.ACTUAL,
                    cryptoCurrency = cryptoCurrency,
                    balance = null,
                    customerId = CUSTOMER_ID,
                    productInstanceId = PRODUCT_INSTANCE_ID,
                ),
            )
        }

    @ParameterizedTest
    @ProvideTestModels
    fun `GIVEN active product instance AND channels WHEN invoke THEN bank off-ramp availability is stored`(
        model: ChannelsModel,
    ) = runTest {
        // Arrange
        val balance = MoneyAccountStatusValue.Balance(
            cryptoAmount = BigDecimal.TEN,
            depositAddress = "0xdeposit",
            currencyCode = "USD",
            fiatRate = BigDecimal.ONE,
        )
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.ACTIVE,
        ).right()
        coEvery { getMoneyAccountBalanceUseCase(USER_WALLET_ID, cryptoCurrency) } returns balance.right()
        coEvery { onboardingRepository.fetchCustomerEligibility(USER_WALLET_ID) } returns model.channels

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(
            MoneyAccountStatusValue.Account.Active(
                source = StatusSource.ACTUAL,
                cryptoCurrency = cryptoCurrency,
                balance = balance,
                customerId = CUSTOMER_ID,
                isBankOfframpAvailable = model.expected,
                productInstanceId = PRODUCT_INSTANCE_ID,
            ),
        )
    }

    @Test
    fun `GIVEN deactivated product instance WHEN invoke THEN deactivated account is stored`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.DEACTIVATED,
        ).right()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(deactivated())
    }

    @Test
    fun `GIVEN former customer with active account WHEN invoke THEN deactivated account is stored`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.ACTIVE,
            state = CustomerInfo.State.FORMER,
        ).right()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(deactivated())
    }

    @Test
    fun `GIVEN no account instance WHEN invoke THEN activating is stored`() = runTest {
        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(activating())
    }

    @Test
    fun `GIVEN account instance still provisioning WHEN invoke THEN activating is stored`() = runTest {
        // Arrange
        coEvery { onboardingRepository.getCustomerInfo(USER_WALLET_ID) } returns createCustomerInfo(
            accountStatus = CustomerInfo.ProductInstance.Status.ACTIVATING,
        ).right()

        // Act
        fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        verifyStored(activating())
    }

    @Test
    fun `GIVEN the store fails WHEN invoke THEN the error is returned instead of being thrown`() = runTest {
        // Arrange
        coEvery { moneyAccountStatusesStore.store(any(), any()) } throws IllegalStateException()

        // Act
        val actual = fetcher.invoke(MoneyAccountStatusFetcher.Params(USER_WALLET_ID))

        // Assert
        assertThat(actual.isLeft()).isTrue()
    }

    private suspend fun verifyStored(value: MoneyAccountStatusValue) {
        coVerify(exactly = 1) {
            moneyAccountStatusesStore.store(
                userWalletId = USER_WALLET_ID,
                status = AccountStatus.Money(
                    account = Account.Money(userWalletId = USER_WALLET_ID),
                    value = value,
                ),
            )
        }
    }

    private fun deactivated() = MoneyAccountStatusValue.Account.Deactivated(
        source = StatusSource.ACTUAL,
        cryptoCurrency = cryptoCurrency,
        balance = null,
        customerId = CUSTOMER_ID,
    )

    private fun activating() = MoneyAccountStatusValue.Onboarding.Activating(
        source = StatusSource.ACTUAL,
        customerId = CUSTOMER_ID,
    )

    private fun createCustomerInfo(
        accountStatus: CustomerInfo.ProductInstance.Status? = null,
        state: CustomerInfo.State = CustomerInfo.State.ACTIVE,
    ) = CustomerInfo(
        customerId = CUSTOMER_ID,
        paymentAccount = null,
        productInstances = listOfNotNull(accountStatus?.let(::createAccountInstance)),
        cards = emptyList(),
        kycStatus = KycStatus.APPROVED,
        state = state,
        fiatBalance = null,
        tariffPlan = null,
    )

    private fun createAccountInstance(status: CustomerInfo.ProductInstance.Status) = CustomerInfo.ProductInstance(
        id = PRODUCT_INSTANCE_ID,
        cardId = "",
        frozenState = TangemPayCardFrozenState.Unfrozen,
        displayName = null,
        actualCardLimit = null,
        adminCardLimit = null,
        status = status,
        specificationDataType = CustomerInfo.ProductInstance.SpecificationDataType.ACCOUNT,
        paymentAccountType = CustomerInfo.ProductInstance.PaymentAccountType.COLLATERAL_BANKING_ACCOUNT,
    )

    private fun provideTestModels() = listOf(
        ChannelsModel(
            channels = listOf(
                TangemPayEligibilityType.VISA_VIRTUAL_ACCOUNT,
                TangemPayEligibilityType.BANK_OFFRAMP,
            ).right(),
            expected = true,
        ),
        ChannelsModel(channels = listOf(TangemPayEligibilityType.VISA_VIRTUAL_ACCOUNT).right(), expected = false),
        ChannelsModel(channels = VisaApiError.ServerUnavailable.left(), expected = null),
    )

    data class ChannelsModel(
        val channels: Either<VisaApiError, List<TangemPayEligibilityType>>,
        val expected: Boolean?,
    )

    private companion object {
        val USER_WALLET_ID = UserWalletId("011")
        const val CUSTOMER_ID = "customer-1"
        const val PRODUCT_INSTANCE_ID = "pi-1"
    }
}
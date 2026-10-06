package com.tangem.features.moneyaccount.onboarding.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.models.pay.TangemPayProduct
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.moneyaccount.model.MoneyAccountActivationError
import com.tangem.domain.moneyaccount.model.MoneyAccountEligibility
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.usecase.ActivateMoneyAccountUseCase
import com.tangem.domain.moneyaccount.usecase.CreateMoneyAccountOrderUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountEligibilityUseCase
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountFeeTableUseCase
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.domain.visa.model.MoneyAccountDepositCredentials
import com.tangem.features.moneyaccount.onboarding.component.MoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingNavigation
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM.FeeRowUM
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM.FeesUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.math.BigDecimal

internal class MoneyAccountOnboardingModelTest {

    private val router: Router = mockk(relaxed = true)
    private val urlOpener: UrlOpener = mockk(relaxed = true)
    private val onboardingRepository: OnboardingRepository = mockk()
    private val getMoneyAccountFeeTableUseCase: GetMoneyAccountFeeTableUseCase = mockk()
    private val getMoneyAccountEligibilityUseCase: GetMoneyAccountEligibilityUseCase = mockk()
    private val activateMoneyAccountUseCase: ActivateMoneyAccountUseCase = mockk()
    private val createMoneyAccountOrderUseCase: CreateMoneyAccountOrderUseCase = mockk()
    private val uiMessageSender: UiMessageSender = mockk(relaxed = true)

    @BeforeEach
    fun resetMocks() {
        clearMocks(
            router,
            urlOpener,
            onboardingRepository,
            getMoneyAccountFeeTableUseCase,
            getMoneyAccountEligibilityUseCase,
            activateMoneyAccountUseCase,
            createMoneyAccountOrderUseCase,
            uiMessageSender,
        )
    }

    @Test
    fun `GIVEN single eligible wallet WHEN get started clicked THEN account created AND residency opened`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("011")
        coEvery { getMoneyAccountEligibilityUseCase() } returns
            MoneyAccountEligibility.Available(wallets = listOf(mockWallet(userWalletId)))
        mockAccountCreation(userWalletId)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.content().onGetStartedClick()
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 1) {
            createMoneyAccountOrderUseCase(
                userWalletId = userWalletId,
                depositAddress = CREDENTIALS.address,
                signature = CREDENTIALS.signature,
            )
        }
        verify(exactly = 1) { router.push(eligibilityRoute(userWalletId), any()) }
        assertThat(model.content().isLoading).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN several eligible wallets WHEN get started clicked THEN wallet selector opened`() = runTest {
        // Arrange
        val walletsIds = listOf(UserWalletId("011"), UserWalletId("022"))
        coEvery { getMoneyAccountEligibilityUseCase() } returns
            MoneyAccountEligibility.Available(wallets = walletsIds.map(::mockWallet))
        val model = createModel(testScope = this)
        val openedSheets = mutableListOf<MoneyAccountOnboardingNavigation?>()
        model.bottomSheetNavigation.subscribe { event -> openedSheets += event.transformer(null) }
        advanceUntilIdle()

        // Act
        model.content().onGetStartedClick()
        advanceUntilIdle()

        // Assert
        assertThat(openedSheets).containsExactly(MoneyAccountOnboardingNavigation.WalletSelector(walletsIds))
        coVerify(exactly = 0) { activateMoneyAccountUseCase(any()) }
        verify(exactly = 0) { router.push(any(), any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN wallet selector shown WHEN wallet selected THEN account created AND residency opened`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("022")
        mockAccountCreation(userWalletId)
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onWalletSelected(userWalletId)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { router.push(eligibilityRoute(userWalletId), any()) }

        model.onDestroy()
    }

    @Test
    fun `GIVEN user cancelled activation WHEN wallet selected THEN stays on screen without error`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("022")
        coEvery { activateMoneyAccountUseCase(userWalletId) } returns MoneyAccountActivationError.UserCancelled.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onWalletSelected(userWalletId)
        advanceUntilIdle()

        // Assert
        coVerify(exactly = 0) { createMoneyAccountOrderUseCase(any(), any(), any()) }
        verify(exactly = 0) { uiMessageSender.send(any()) }
        verify(exactly = 0) { router.push(any(), any()) }
        assertThat(model.content().isLoading).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN order creation fails WHEN wallet selected THEN error shown AND residency step not opened`() = runTest {
        // Arrange
        val userWalletId = UserWalletId("022")
        coEvery { activateMoneyAccountUseCase(userWalletId) } returns CREDENTIALS.right()
        coEvery { createMoneyAccountOrderUseCase(userWalletId, any(), any()) } returns
            VisaApiError.ServerUnavailable.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.onWalletSelected(userWalletId)
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any()) }
        verify(exactly = 0) { router.push(any(), any()) }
        assertThat(model.content().isLoading).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN no eligible wallets WHEN get started clicked THEN error shown AND screen kept`() = runTest {
        // Arrange
        coEvery { getMoneyAccountEligibilityUseCase() } returns MoneyAccountEligibility.NotAvailable
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.content().onGetStartedClick()
        advanceUntilIdle()

        // Assert
        verify(exactly = 1) { uiMessageSender.send(any()) }
        verify(exactly = 0) { router.pop(any()) }
        verify(exactly = 0) { router.push(any(), any()) }
        assertThat(model.content().isLoading).isFalse()

        model.onDestroy()
    }

    @Test
    fun `GIVEN payment methods WHEN terms and fees clicked THEN fee rows shown`() = runTest {
        // Arrange
        val ach = paymentMethod(
            id = "ACH",
            iconUrl = ICON_URL,
            receiveFee = fee(amount = BigDecimal("1.00")),
            sendFee = fee(amount = BigDecimal("1.50")),
        )
        val fedWire = paymentMethod(id = "FEDWIRE", iconUrl = null, receiveFee = fee(amount = BigDecimal("11")))
        coEvery { getMoneyAccountFeeTableUseCase() } returns listOf(ach, fedWire).right()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.content().onTermsAndFeesClick()
        advanceUntilIdle()

        // Assert
        val expected = FeesUM.Content(
            rows = persistentListOf(
                FeeRowUM(
                    icon = TangemIconUM.Url(url = ICON_URL, fallbackRes = R.drawable.ic_empty_64),
                    provider = "ACH",
                    receive = formatUsd(BigDecimal("1.00")),
                    send = formatUsd(BigDecimal("1.50")),
                ),
                FeeRowUM(
                    icon = TangemIconUM.Empty,
                    provider = "FEDWIRE",
                    receive = formatUsd(BigDecimal("11")),
                    send = DASH_SIGN,
                ),
            ),
        )
        assertThat(model.content().fees).isEqualTo(expected)

        model.onDestroy()
    }

    @Test
    fun `GIVEN failing request WHEN terms and fees clicked THEN error shown`() = runTest {
        // Arrange
        coEvery { getMoneyAccountFeeTableUseCase() } returns VisaApiError.ServerUnavailable.left()
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.content().onTermsAndFeesClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.content().fees).isInstanceOf(FeesUM.Error::class.java)

        model.onDestroy()
    }

    @Test
    fun `GIVEN error shown WHEN retry clicked THEN fees reloaded`() = runTest {
        // Arrange
        coEvery { getMoneyAccountFeeTableUseCase() } returnsMany listOf(
            VisaApiError.ServerUnavailable.left(),
            listOf(paymentMethod()).right(),
        )
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.content().onTermsAndFeesClick()
        advanceUntilIdle()
        val error = model.content().fees as FeesUM.Error

        // Act
        error.onRetryClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.content().fees).isInstanceOf(FeesUM.Content::class.java)
        coVerify(exactly = 2) { getMoneyAccountFeeTableUseCase() }

        model.onDestroy()
    }

    @Test
    fun `GIVEN request in flight WHEN sheet dismissed THEN sheet stays hidden after load`() = runTest {
        // Arrange
        val pending = CompletableDeferred<Either<VisaApiError, List<MoneyAccountPaymentMethod>>>()
        coEvery { getMoneyAccountFeeTableUseCase() } coAnswers { pending.await() }
        val model = createModel(testScope = this)
        advanceUntilIdle()
        model.content().onTermsAndFeesClick()
        advanceUntilIdle()

        // Act
        model.content().onFeesDismiss()
        pending.complete(listOf(paymentMethod()).right())
        advanceUntilIdle()

        // Assert
        assertThat(model.content().fees).isNull()

        model.onDestroy()
    }

    @Test
    fun `GIVEN content shown WHEN terms clicked THEN terms of use opened`() = runTest {
        // Arrange
        val model = createModel(testScope = this)
        advanceUntilIdle()

        // Act
        model.content().onTermsClick()

        // Assert
        verify(exactly = 1) { urlOpener.openUrl("https://tangem.com/docs/en/virtual-account-terms.pdf") }

        model.onDestroy()
    }

    private fun createModel(testScope: TestScope) = MoneyAccountOnboardingModel(
        paramsContainer = MutableParamsContainer(value = MoneyAccountOnboardingComponent.Params.FromMain),
        dispatchers = testScope.createTestingCoroutineDispatcherProvider(),
        router = router,
        urlOpener = urlOpener,
        onboardingRepository = onboardingRepository,
        getMoneyAccountFeeTableUseCase = getMoneyAccountFeeTableUseCase,
        getMoneyAccountEligibilityUseCase = getMoneyAccountEligibilityUseCase,
        activateMoneyAccountUseCase = activateMoneyAccountUseCase,
        createMoneyAccountOrderUseCase = createMoneyAccountOrderUseCase,
        uiMessageSender = uiMessageSender,
    )

    private fun mockAccountCreation(userWalletId: UserWalletId) {
        coEvery { activateMoneyAccountUseCase(userWalletId) } returns CREDENTIALS.right()
        coEvery { createMoneyAccountOrderUseCase(userWalletId, any(), any()) } returns Unit.right()
    }

    private fun eligibilityRoute(userWalletId: UserWalletId) = AppRoute.TangemPayEligibility(
        product = TangemPayProduct.BANKING_ACCOUNT,
        userWalletId = userWalletId,
    )

    private fun mockWallet(userWalletId: UserWalletId): UserWallet = mockk {
        every { walletId } returns userWalletId
    }

    private fun MoneyAccountOnboardingModel.content() = uiState.value as MoneyAccountOnboardingUM.Content

    private fun formatUsd(amount: BigDecimal): String {
        val currency = getJavaCurrencyByCode("USD")
        return amount.format { fiat(currency.currencyCode, currency.symbol).optionalDecimals() }
    }

    private fun paymentMethod(
        id: String = "ACH",
        iconUrl: String? = ICON_URL,
        receiveFee: MoneyAccountPaymentMethod.Fee? = fee(),
        sendFee: MoneyAccountPaymentMethod.Fee? = null,
    ) = MoneyAccountPaymentMethod(
        id = id,
        name = id,
        countryCode = "US",
        iconUrl = iconUrl,
        receiveFee = receiveFee,
        sendFee = sendFee,
    )

    private fun fee(amount: BigDecimal = BigDecimal.ONE) = MoneyAccountPaymentMethod.Fee(
        type = "ACH_ONRAMP",
        amount = amount,
        currency = "USD",
    )

    private companion object {
        const val ICON_URL = "https://tangem.com/flags/us.png"
        val CREDENTIALS = MoneyAccountDepositCredentials(address = "0xdeposit", signature = "0xsignature")
    }
}
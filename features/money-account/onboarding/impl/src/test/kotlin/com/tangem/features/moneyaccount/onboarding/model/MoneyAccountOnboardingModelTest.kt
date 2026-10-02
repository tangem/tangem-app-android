package com.tangem.features.moneyaccount.onboarding.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.navigation.url.UrlOpener
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.core.ui.format.bigdecimal.getJavaCurrencyByCode
import com.tangem.core.ui.format.bigdecimal.optionalDecimals
import com.tangem.domain.moneyaccount.model.MoneyAccountPaymentMethod
import com.tangem.domain.moneyaccount.usecase.GetMoneyAccountFeeTableUseCase
import com.tangem.domain.pay.repository.OnboardingRepository
import com.tangem.domain.visa.error.VisaApiError
import com.tangem.features.moneyaccount.onboarding.component.MoneyAccountOnboardingComponent
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM.FeeRowUM
import com.tangem.features.moneyaccount.onboarding.ui.MoneyAccountOnboardingUM.FeesUM
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.StringsSigns.DASH_SIGN
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
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

    @BeforeEach
    fun resetMocks() {
        clearMocks(router, urlOpener, onboardingRepository, getMoneyAccountFeeTableUseCase)
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
    )

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
    }
}
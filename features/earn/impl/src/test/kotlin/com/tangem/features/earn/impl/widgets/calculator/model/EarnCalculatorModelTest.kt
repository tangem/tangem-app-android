package com.tangem.features.earn.impl.widgets.calculator.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.format.bigdecimal.fiat
import com.tangem.core.ui.format.bigdecimal.format
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.R
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorComponent
import com.tangem.features.earn.impl.widgets.calculator.EarnCalculatorTexts
import com.tangem.features.earn.product.api.product.DefaultEarnProjection
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class EarnCalculatorModelTest {

    private val getSelectedAppCurrency: GetSelectedAppCurrencyUseCase = mockk()

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        clearMocks(getSelectedAppCurrency)
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
    }

    @Test
    fun `GIVEN projection WHEN model created THEN uiState has MAC-27 values`() = runTest {
        // Arrange
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))

        // Act
        advanceUntilIdle()

        // Assert
        val actual = requireNotNull(model.uiState.value)
        assertThat(actual.total).isEqualTo(stringReference("$22,124.34"))
        assertThat(actual.earned).isEqualTo(stringReference("+$1,624.34"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN default input WHEN onInitialStep 1 THEN initial balance is 3500 and total recalculated`() = runTest {
        // Arrange
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
        advanceUntilIdle()

        // Act
        model.onInitialStep(direction = 1)
        advanceUntilIdle()

        // Assert
        val actual = requireNotNull(model.uiState.value)
        assertThat(actual.initialBalance.value).isEqualTo(stringReference("$3,500"))
        assertThat(actual.total).isEqualTo(totalText(initial = 3500, topUp = 500, rate = "0.0468"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN stepped input WHEN projection emits another rate THEN input is kept and only the rate changes`() =
        runTest {
            // Arrange
            val projection = MutableStateFlow<EarnProjection?>(projectionOf("0.0468"))
            val model = createModel(projection = projection)
            advanceUntilIdle()
            model.onInitialStep(direction = 1)
            advanceUntilIdle()

            // Act
            projection.value = projectionOf("0.032")
            advanceUntilIdle()

            // Assert
            val actual = requireNotNull(model.uiState.value)
            assertThat(actual.initialBalance.value).isEqualTo(stringReference("$3,500"))
            assertThat(actual.total).isEqualTo(totalText(initial = 3500, topUp = 500, rate = "0.032"))
            model.onDestroy()
        }

    @Test
    fun `GIVEN projection null WHEN emitted THEN uiState is null and restored with kept input when projection returns`() =
        runTest {
            // Arrange
            val projection = MutableStateFlow<EarnProjection?>(projectionOf("0.0468"))
            val model = createModel(projection = projection)
            advanceUntilIdle()
            model.onInitialStep(direction = 1)
            advanceUntilIdle()

            // Act
            projection.value = null
            advanceUntilIdle()
            val hidden = model.uiState.value
            projection.value = projectionOf("0.0468")
            advanceUntilIdle()

            // Assert
            assertThat(hidden).isNull()
            val restored = requireNotNull(model.uiState.value)
            assertThat(restored.initialBalance.value).isEqualTo(stringReference("$3,500"))
            assertThat(restored.total).isEqualTo(totalText(initial = 3500, topUp = 500, rate = "0.0468"))
            model.onDestroy()
        }

    @Test
    fun `GIVEN currency change WHEN app currency emits EUR THEN texts use the new symbol`() = runTest {
        // Arrange
        val currency = MutableStateFlow(AppCurrency.Default)
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")), currency = currency)
        advanceUntilIdle()

        // Act
        currency.value = AppCurrency.Default.copy(code = "EUR", symbol = "€")
        advanceUntilIdle()

        // Assert
        val actual = requireNotNull(model.uiState.value)
        assertThat((actual.total as TextReference.Str).value).contains("€")
        assertThat((actual.initialBalance.value as TextReference.Str).value).contains("€")
        model.onDestroy()
    }

    @Test
    fun `GIVEN onHorizonSelect Year10 WHEN input updated THEN total is 79881 12 and Year10 item is selected`() =
        runTest {
            // Arrange
            val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
            advanceUntilIdle()

            // Act
            model.onHorizonSelect(EarnHorizon.Year10)
            advanceUntilIdle()

            // Assert
            val actual = requireNotNull(model.uiState.value)
            assertThat(actual.total).isEqualTo(stringReference("$79,881.12"))
            assertThat(actual.horizon.items.single { it.isSelected }.label)
                .isEqualTo(EarnCalculatorTexts.horizon(EarnHorizon.Year10))
            model.onDestroy()
        }

    @Test
    fun `GIVEN default input WHEN onTopUpStep -1 THEN monthly top-up text is 400`() = runTest {
        // Arrange
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
        advanceUntilIdle()

        // Act
        model.onTopUpStep(direction = -1)
        advanceUntilIdle()

        // Assert
        val actual = requireNotNull(model.uiState.value)
        assertThat(actual.monthlyTopUp.value).isEqualTo(stringReference("$400"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN incentive rate WHEN model created THEN uiState has the badge`() = runTest {
        // Arrange
        val model = createModel(
            projection = MutableStateFlow(projectionOf("0.0468")),
            incentiveRate = MutableStateFlow(incentiveOf("0.014")),
        )

        // Act
        advanceUntilIdle()

        // Assert
        val actual = requireNotNull(model.uiState.value)
        assertThat(actual.incentive?.text).isEqualTo(incentiveText("1.4%"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN badge shown WHEN incentive rate becomes null THEN badge disappears and input is kept`() = runTest {
        // Arrange
        val incentiveRate = MutableStateFlow<EarnProductRate?>(incentiveOf("0.014"))
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")), incentiveRate = incentiveRate)
        advanceUntilIdle()
        model.onInitialStep(direction = 1)
        advanceUntilIdle()

        // Act
        incentiveRate.value = null
        advanceUntilIdle()

        // Assert
        val actual = requireNotNull(model.uiState.value)
        assertThat(actual.incentive).isNull()
        assertThat(actual.initialBalance.value).isEqualTo(stringReference("$3,500"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN params without incentive WHEN incentive rate collected THEN it is null`() = runTest {
        // Act
        val actual = EarnCalculatorComponent.Params(projection = flowOf(null)).incentiveRate.first()

        // Assert
        assertThat(actual).isNull()
    }

    private fun incentiveOf(fraction: String) = EarnProductRate(fraction = BigDecimal(fraction), type = EarnRateType.APY)

    private fun incentiveText(percent: String) = resourceReference(
        id = R.string.yield_module_promo_screen_incentivised_chip,
        formatArgs = wrappedList(percent),
    )

    private fun TestScope.createModel(
        projection: Flow<EarnProjection?>,
        currency: Flow<AppCurrency> = MutableStateFlow(AppCurrency.Default),
        incentiveRate: Flow<EarnProductRate?> = flowOf(null),
    ): EarnCalculatorModel {
        every { getSelectedAppCurrency.invokeOrDefault() } returns currency

        return EarnCalculatorModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                EarnCalculatorComponent.Params(projection = projection, incentiveRate = incentiveRate),
            ),
            getSelectedAppCurrency = getSelectedAppCurrency,
            converter = EarnCalculatorConverter(),
        )
    }

    private fun projectionOf(rate: String): EarnProjection = DefaultEarnProjection(netRate = BigDecimal(rate))

    private fun totalText(initial: Long, topUp: Long, rate: String): TextReference {
        val total = projectionOf(rate).project(BigDecimal(initial), BigDecimal(topUp), EarnHorizon.Year3).total

        return stringReference(total.format { fiat(AppCurrency.Default.code, AppCurrency.Default.symbol) })
    }
}
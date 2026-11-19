package com.tangem.features.earn.impl.widgets.calculator.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
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
import com.tangem.features.earn.product.api.product.DefaultEarnProjection
import com.tangem.features.earn.product.api.product.EarnHorizon
import com.tangem.features.earn.product.api.product.EarnProjection
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
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
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
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
                .isEqualTo(pluralReference(R.plurals.earn_calculator_years, count = 10, formatArgs = wrappedList(10)))
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
    fun `GIVEN 3Y bars WHEN onBarSelected 5 THEN selectedBar is 5`() = runTest {
        // Arrange
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
        advanceUntilIdle()

        // Act
        model.onBarSelected(index = 5)
        advanceUntilIdle()

        // Assert
        assertThat(requireNotNull(model.uiState.value).selectedBar).isEqualTo(5)
        model.onDestroy()
    }

    @Test
    fun `GIVEN bar selected WHEN onBarSelected null THEN selectedBar is null`() = runTest {
        // Arrange
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
        advanceUntilIdle()
        model.onBarSelected(index = 5)
        advanceUntilIdle()

        // Act
        model.onBarSelected(index = null)
        advanceUntilIdle()

        // Assert
        assertThat(requireNotNull(model.uiState.value).selectedBar).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN 24 bars WHEN onBarSelected 24 THEN selectedBar stays null`() = runTest {
        // Arrange
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
        advanceUntilIdle()

        // Act
        model.onBarSelected(index = 24)
        advanceUntilIdle()

        // Assert
        assertThat(requireNotNull(model.uiState.value).selectedBar).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN bar 23 selected on 3Y WHEN horizon becomes Months6 THEN selectedBar is null and onBarSelected 23 is ignored`() =
        runTest {
            // Arrange
            val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
            advanceUntilIdle()
            model.onBarSelected(index = 23)
            advanceUntilIdle()

            // Act
            model.onHorizonSelect(EarnHorizon.Months6)
            advanceUntilIdle()
            val afterHorizonChange = requireNotNull(model.uiState.value)
            model.onBarSelected(index = 23)
            advanceUntilIdle()

            // Assert
            assertThat(afterHorizonChange.bars).hasSize(6)
            assertThat(afterHorizonChange.selectedBar).isNull()
            assertThat(requireNotNull(model.uiState.value).selectedBar).isNull()
            model.onDestroy()
        }

    @Test
    fun `GIVEN bar selected WHEN onInitialStep 1 THEN selectedBar is null`() = runTest {
        // Arrange
        val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
        advanceUntilIdle()
        model.onBarSelected(index = 5)
        advanceUntilIdle()

        // Act
        model.onInitialStep(direction = 1)
        advanceUntilIdle()

        // Assert
        assertThat(requireNotNull(model.uiState.value).selectedBar).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN bar 5 selected WHEN projection re-emits an equal projection THEN selectedBar stays 5`() = runTest {
        // Arrange
        val projection = MutableStateFlow<EarnProjection?>(projectionOf("0.0468"))
        val model = createModel(projection = projection)
        advanceUntilIdle()
        model.onBarSelected(index = 5)
        advanceUntilIdle()

        // Act
        projection.value = projectionOf("0.0468")
        advanceUntilIdle()

        // Assert
        assertThat(requireNotNull(model.uiState.value).selectedBar).isEqualTo(5)
        model.onDestroy()
    }

    @Test
    fun `GIVEN bar 5 selected WHEN projection emits another rate THEN selectedBar is null`() = runTest {
        // Arrange
        val projection = MutableStateFlow<EarnProjection?>(projectionOf("0.0468"))
        val model = createModel(projection = projection)
        advanceUntilIdle()
        model.onBarSelected(index = 5)
        advanceUntilIdle()

        // Act
        projection.value = projectionOf("0.032")
        advanceUntilIdle()

        // Assert
        assertThat(requireNotNull(model.uiState.value).selectedBar).isNull()
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
        assertThat(actual.incentive?.badge?.text).isEqualTo(incentiveText("1.4%"))
        model.onDestroy()
    }

    @Test
    fun `GIVEN incentive rate WHEN the badge is clicked THEN the callback is notified once`() = runTest {
        // Arrange
        val callback: EarnCalculatorComponent.ModelCallback = mockk(relaxed = true)
        val model = createModel(
            projection = MutableStateFlow(projectionOf("0.0468")),
            incentiveRate = MutableStateFlow(incentiveOf("0.014")),
            callback = callback,
        )
        advanceUntilIdle()

        // Act
        requireNotNull(model.uiState.value?.incentive).onClick()

        // Assert
        verify(exactly = 1) { callback.onIncentiveClick() }
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
        val actual = EarnCalculatorComponent.Params(projection = flowOf(null), callback = mockk()).incentiveRate.first()

        // Assert
        assertThat(actual).isNull()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class Callback {

        private val callback: EarnCalculatorComponent.ModelCallback = mockk(relaxed = true)

        @BeforeEach
        fun resetCallback() {
            clearMocks(callback)
        }

        @Test
        fun `GIVEN default horizon WHEN selecting another horizon THEN onHorizonChange with it once`() = runTest {
            // Arrange
            val other = EarnHorizon.entries.first { it != CalculatorInput.Default.horizon }
            val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")), callback = callback)
            advanceUntilIdle()

            // Act
            model.onHorizonSelect(other)
            advanceUntilIdle()

            // Assert
            verify(exactly = 1) { callback.onHorizonChange(other) }
            verify(exactly = 0) { callback.onInputChange(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN default horizon WHEN selecting the same horizon THEN no callback`() = runTest {
            // Arrange
            val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")), callback = callback)
            advanceUntilIdle()

            // Act
            model.onHorizonSelect(CalculatorInput.Default.horizon)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { callback.onHorizonChange(any()) }
            model.onDestroy()
        }

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN default input WHEN stepping up THEN onInputChange with the stepped input`(model: StepModel) =
            runTest {
                // Arrange
                val earnModel = createModel(projection = MutableStateFlow(projectionOf("0.0468")), callback = callback)
                advanceUntilIdle()

                // Act
                model.step(earnModel)
                advanceUntilIdle()

                // Assert
                verify(exactly = 1) { callback.onInputChange(model.expected) }
                verify(exactly = 0) { callback.onHorizonChange(any()) }
                earnModel.onDestroy()
            }

        @Test
        fun `GIVEN initial at its lower bound WHEN step down THEN no callback`() = runTest {
            // Arrange
            val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")), callback = callback)
            advanceUntilIdle()
            while (requireNotNull(model.uiState.value).initialBalance.isMinusEnabled) {
                model.onInitialStep(direction = -1)
                advanceUntilIdle()
            }
            clearMocks(callback)

            // Act
            model.onInitialStep(direction = -1)
            advanceUntilIdle()

            // Assert
            verify(exactly = 0) { callback.onInputChange(any()) }
            model.onDestroy()
        }

        @Test
        fun `GIVEN no callback in params WHEN input changed THEN uiState is updated`() = runTest {
            // Arrange
            val model = createModel(projection = MutableStateFlow(projectionOf("0.0468")))
            advanceUntilIdle()

            // Act
            model.onInitialStep(direction = 1)
            advanceUntilIdle()

            // Assert
            val actual = requireNotNull(model.uiState.value)
            assertThat(actual.initialBalance.value).isEqualTo(stringReference("$3,500"))
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            StepModel(
                step = { it.onInitialStep(direction = 1) },
                expected = EarnCalculatorComponent.Input.InitialBalance,
            ),
            StepModel(
                step = { it.onTopUpStep(direction = 1) },
                expected = EarnCalculatorComponent.Input.MonthlyTopUp,
            ),
        )
    }

    internal data class StepModel(
        val step: (EarnCalculatorModel) -> Unit,
        val expected: EarnCalculatorComponent.Input,
    ) {
        override fun toString() = expected.name
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
        callback: EarnCalculatorComponent.ModelCallback = mockk(relaxed = true),
    ): EarnCalculatorModel {
        every { getSelectedAppCurrency.invokeOrDefault() } returns currency

        return EarnCalculatorModel(
            dispatchers = createTestingCoroutineDispatcherProvider(),
            paramsContainer = MutableParamsContainer(
                EarnCalculatorComponent.Params(
                    projection = projection,
                    incentiveRate = incentiveRate,
                    callback = callback,
                ),
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
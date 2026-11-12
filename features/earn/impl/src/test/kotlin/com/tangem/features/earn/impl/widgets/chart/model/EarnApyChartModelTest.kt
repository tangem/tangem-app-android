package com.tangem.features.earn.impl.widgets.chart.model

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.earn.api.overview.EarnProductRate
import com.tangem.features.earn.api.overview.EarnRateType
import com.tangem.features.earn.impl.widgets.chart.EarnApyChartComponent
import com.tangem.features.earn.product.api.product.EarnApyHistory
import com.tangem.features.earn.product.api.product.EarnApyPoint
import com.tangem.features.earn.product.api.product.EarnPeriod
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import com.tangem.utils.logging.Severity
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
internal class EarnApyChartModelTest {

    private val periods = listOf(
        EarnPeriod(id = "1m", label = stringReference("1M")),
        EarnPeriod(id = "3m", label = stringReference("3M")),
        EarnPeriod(id = "6m", label = stringReference("6M")),
    )

    private val requested = mutableListOf<String>()
    private val logs = mutableListOf<Pair<Severity, String>>()

    private lateinit var originalLocale: Locale

    @BeforeEach
    fun setUp() {
        originalLocale = Locale.getDefault()
        Locale.setDefault(Locale.US)
        requested.clear()
        logs.clear()
        TangemLogger.setLogWriters(
            listOf(
                object : TangemLogger.LogWriter {
                    override fun write(
                        severity: Severity,
                        tag: String,
                        message: String,
                        throwable: Throwable?,
                        shouldSanitize: Boolean,
                    ) {
                        logs += severity to message
                    }
                },
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        Locale.setDefault(originalLocale)
        TangemLogger.setLogWriters(emptyList())
    }

    @Test
    fun `GIVEN model created WHEN idle THEN history requested for the default period and bars shown`() = runTest {
        // Arrange
        val model = createModel { period ->
            requested += period.id
            historyOf(count = 3).right()
        }

        // Act
        advanceUntilIdle()

        // Assert
        val actual = model.uiState.value
        assertThat(requested).containsExactly("6m")
        assertThat(actual.bars).hasSize(3)
        assertThat(actual.isLoading).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN loaded WHEN second period clicked THEN it is selected at once and loading until the answer`() = runTest {
        // Arrange
        val answer = CompletableDeferred<Either<Throwable, EarnApyHistory>>()
        val model = createModel { period -> if (period.id == "3m") answer.await() else historyOf(count = 3).right() }
        advanceUntilIdle()

        // Act
        model.uiState.value.periods[1].onClick()
        runCurrent()

        // Assert
        val loading = model.uiState.value
        assertThat(loading.isLoading).isTrue()
        assertThat(loading.periods.map { it.isSelected }).containsExactly(false, true, false).inOrder()

        answer.complete(historyOf(count = 2).right())
        advanceUntilIdle()
        assertThat(model.uiState.value.isLoading).isFalse()
        model.onDestroy()
    }

    @Test
    fun `GIVEN first request pending WHEN second period answered and then the first THEN only the second shows`() =
        runTest {
            // Arrange
            val first = CompletableDeferred<Either<Throwable, EarnApyHistory>>()
            val second = CompletableDeferred<Either<Throwable, EarnApyHistory>>()
            val model = createModel(defaultPeriod = periods[0]) { period ->
                if (period.id == "1m") first.await() else second.await()
            }
            runCurrent()

            // Act
            model.uiState.value.periods[1].onClick()
            runCurrent()
            second.complete(historyOf(count = 2).right())
            advanceUntilIdle()
            first.complete(historyOf(count = 5).right())
            advanceUntilIdle()

            // Assert
            assertThat(model.uiState.value.bars).hasSize(2)
            model.onDestroy()
        }

    @Test
    fun `GIVEN history that ignores cancellation WHEN superseded THEN the stale answer is not published`() = runTest {
        // Arrange
        val first = CompletableDeferred<Either<Throwable, EarnApyHistory>>()
        val model = createModel(defaultPeriod = periods[0]) { period ->
            if (period.id == "1m") withContext(NonCancellable) { first.await() } else historyOf(count = 2).right()
        }
        runCurrent()

        // Act
        model.uiState.value.periods[1].onClick()
        advanceUntilIdle()
        first.complete(historyOf(count = 5).right())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.bars).hasSize(2)
        model.onDestroy()
    }

    @Test
    fun `GIVEN Left WHEN load THEN empty chart and the error is logged and a repeat request is made`() = runTest {
        // Arrange
        val model = createModel { period ->
            requested += period.id
            IllegalStateException("boom").left()
        }

        // Act
        advanceUntilIdle()

        // Assert
        val failed = model.uiState.value
        assertThat(failed.bars).isEmpty()
        assertThat(failed.isLoading).isFalse()
        assertThat(logs.filter { it.first == Severity.Error }.map { it.second })
            .containsExactly("Earn APY history failed, period 6m")

        failed.periods[2].onClick()
        advanceUntilIdle()
        assertThat(requested).containsExactly("6m", "6m").inOrder()
        model.onDestroy()
    }

    @Test
    fun `GIVEN loaded WHEN onBarSelected 1 THEN selectedBar is 1 and null clears it`() = runTest {
        // Arrange
        val model = createModel { historyOf(count = 3).right() }
        advanceUntilIdle()

        // Act
        model.uiState.value.onBarSelected(1)
        val selected = model.uiState.value.selectedBar
        model.uiState.value.onBarSelected(null)
        val cleared = model.uiState.value.selectedBar

        // Assert
        assertThat(selected).isEqualTo(1)
        assertThat(cleared).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN selected bar WHEN period changed THEN selection is reset`() = runTest {
        // Arrange
        val model = createModel { historyOf(count = 3).right() }
        advanceUntilIdle()
        model.uiState.value.onBarSelected(1)

        // Act
        model.uiState.value.periods[0].onClick()
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.selectedBar).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN loaded WHEN a bar is selected THEN the bars are not rebuilt`() = runTest {
        // Arrange
        val model = createModel { historyOf(count = 3).right() }
        advanceUntilIdle()
        val before = model.uiState.value

        // Act
        before.onBarSelected(1)

        // Assert
        val after = model.uiState.value
        assertThat(after.selectedBar).isEqualTo(1)
        assertThat(after.bars).isSameInstanceAs(before.bars)
        model.onDestroy()
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnBarSelected {

        @ParameterizedTest
        @ProvideTestModels
        fun `GIVEN three loaded bars WHEN onBarSelected THEN only an index inside the bars is kept`(
            model: SelectedBarModel,
        ) = runTest {
            // Arrange
            val chart = createModel { historyOf(count = 3).right() }
            advanceUntilIdle()

            // Act
            chart.uiState.value.onBarSelected(model.selected)

            // Assert
            assertThat(chart.uiState.value.selectedBar).isEqualTo(model.expected)
            chart.onDestroy()
        }

        private fun provideTestModels() = listOf(
            SelectedBarModel(selected = 0, expected = 0),
            SelectedBarModel(selected = 1, expected = 1),
            SelectedBarModel(selected = 2, expected = 2),
            SelectedBarModel(selected = 3, expected = null),
            SelectedBarModel(selected = 9, expected = null),
            SelectedBarModel(selected = -1, expected = null),
            SelectedBarModel(selected = null, expected = null),
        )
    }

    data class SelectedBarModel(val selected: Int?, val expected: Int?)

    @Test
    fun `GIVEN loading WHEN onBarSelected THEN it is ignored`() = runTest {
        // Arrange
        val answer = CompletableDeferred<Either<Throwable, EarnApyHistory>>()
        val model = createModel { answer.await() }
        runCurrent()
        val loading = model.uiState.value

        // Act
        loading.onBarSelected(1)
        answer.complete(historyOf(count = 3).right())
        advanceUntilIdle()

        // Assert
        assertThat(model.uiState.value.selectedBar).isNull()
        model.onDestroy()
    }

    @Test
    fun `GIVEN no periods and an unknown default WHEN created THEN no exception and history still requested`() =
        runTest {
            // Arrange
            val model = createModel(periods = emptyList(), defaultPeriod = periods[1]) { period ->
                requested += period.id
                historyOf(count = 3).right()
            }

            // Act
            advanceUntilIdle()

            // Assert
            val actual = model.uiState.value
            assertThat(requested).containsExactly("3m")
            assertThat(actual.periods).isEmpty()
            assertThat(actual.bars).hasSize(3)
            model.onDestroy()
        }

    @Test
    fun `GIVEN default period outside a non-empty list WHEN created THEN it is requested and no segment is selected`() =
        runTest {
            // Arrange
            val unlisted = EarnPeriod(id = "12m", label = stringReference("12M"))
            val model = createModel(defaultPeriod = unlisted) { period ->
                requested += period.id
                historyOf(count = 3).right()
            }

            // Act
            advanceUntilIdle()

            // Assert
            val actual = model.uiState.value
            assertThat(requested).containsExactly("12m")
            assertThat(actual.periods.map { it.isSelected }).containsExactly(false, false, false).inOrder()
            assertThat(actual.bars).hasSize(3)
            model.onDestroy()
        }

    @Test
    fun `GIVEN model just created WHEN nothing has run yet THEN it is loading for the default period`() = runTest {
        // Arrange
        val model = createModel { historyOf(count = 3).right() }

        // Act
        val actual = model.uiState.value

        // Assert
        assertThat(actual.isLoading).isTrue()
        assertThat(actual.periods.map { it.isSelected }).containsExactly(false, false, true).inOrder()
        model.onDestroy()
    }

    private fun TestScope.createModel(
        periods: List<EarnPeriod> = this@EarnApyChartModelTest.periods,
        defaultPeriod: EarnPeriod = periods.last(),
        history: suspend (EarnPeriod) -> Either<Throwable, EarnApyHistory>,
    ): EarnApyChartModel = EarnApyChartModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            EarnApyChartComponent.Params(
                periods = periods,
                defaultPeriod = defaultPeriod,
                history = history,
                currentRate = EarnProductRate(fraction = BigDecimal("0.055"), type = EarnRateType.APY),
            ),
        ),
        converter = EarnApyChartConverter(),
    )

    private fun historyOf(count: Int) = EarnApyHistory(
        points = List(count) { index ->
            EarnApyPoint(
                at = Instant.parse("2026-03-10T12:00:00Z"),
                rate = EarnProductRate(
                    fraction = BigDecimal("0.03").add(BigDecimal("0.01").multiply(BigDecimal(index))),
                    type = EarnRateType.APY,
                ),
            )
        },
        average = BigDecimal("0.0512"),
    )
}
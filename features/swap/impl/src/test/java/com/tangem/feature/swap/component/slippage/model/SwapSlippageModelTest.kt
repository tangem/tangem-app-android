package com.tangem.feature.swap.component.slippage.model

import com.google.common.truth.Truth.assertThat
import com.tangem.core.decompose.model.MutableParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.swap.component.slippage.SwapSlippageComponent
import com.tangem.feature.swap.domain.models.domain.ExchangeProviderType
import com.tangem.feature.swap.domain.models.domain.SwapProvider
import com.tangem.feature.swap.model.formatSlippagePercent
import com.tangem.feature.swap.models.states.SwapSlippageBottomSheetConfig
import com.tangem.feature.swap.presentation.R
import com.tangem.test.core.ProvideTestModels
import com.tangem.test.core.createTestingCoroutineDispatcherProvider
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import java.math.BigDecimal

internal class SwapSlippageModelTest {

    private val defaultPercent = BigDecimal("0.5")

    @Nested
    inner class InitialState {

        @Test
        fun `GIVEN provider without custom slippage WHEN created THEN read only`() = runTest {
            // Arrange
            val model = createModel(provider = createProvider(isCustomSlippageSupported = false))

            // Act
            val actual = model.uiState.value.valueState

            // Assert
            assertThat(actual).isEqualTo(
                SwapSlippageBottomSheetConfig.Value.ReadOnly(
                    slippage = stringReference(defaultPercent.formatSlippagePercent()),
                ),
            )
            model.onDestroy()
        }

        @Test
        fun `GIVEN accepted custom slippage WHEN created THEN field shows it and confirm disabled`() = runTest {
            // Arrange
            val model = createModel(customSlippage = BigDecimal("0.70"))

            // Act
            val actual = model.uiState.value.valueState as SwapSlippageBottomSheetConfig.Value.Edit

            // Assert
            assertThat(actual.value to actual.isConfirmEnabled).isEqualTo("0.7" to false)
            assertThat(actual.presets.map { it.value }).containsExactly("0.25", "0.5", "1").inOrder()
            model.onDestroy()
        }

        @Test
        fun `GIVEN low custom slippage WHEN created THEN warning shown`() = runTest {
            // Arrange
            val model = createModel(customSlippage = BigDecimal("0.11"))

            // Act
            val actual = model.uiState.value.warning

            // Assert
            assertThat(actual).isEqualTo(LOW_SLIPPAGE_WARNING)
            model.onDestroy()
        }

        @Test
        fun `GIVEN custom slippage the provider does not accept WHEN created THEN field shows default`() = runTest {
            // Arrange
            val model = createModel(customSlippage = BigDecimal("1.5"))

            // Act
            val actual = model.uiState.value.valueState as SwapSlippageBottomSheetConfig.Value.Edit

            // Assert
            assertThat(actual.value).isEqualTo("0.5")
            model.onDestroy()
        }
    }

    @Nested
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    inner class OnValueChange {

        @ParameterizedTest
        @ProvideTestModels
        fun onValueChange(testModel: ValueChangeModel) = runTest {
            // Arrange
            val model = createModel()

            // Act
            model.onValueChange(testModel.input)

            // Assert
            val state = model.uiState.value
            val valueState = state.valueState as SwapSlippageBottomSheetConfig.Value.Edit
            assertThat(ValueChangeResult(valueState.value, valueState.isConfirmEnabled, state.error, state.warning))
                .isEqualTo(testModel.expected)
            model.onDestroy()
        }

        private fun provideTestModels() = listOf(
            ValueChangeModel(
                input = "0.8",
                expected = ValueChangeResult("0.8", true, error = null, warning = null),
            ),
            ValueChangeModel(
                input = "0.25",
                expected = ValueChangeResult("0.25", true, error = null, warning = null),
            ),
            ValueChangeModel(
                input = "0.11",
                expected = ValueChangeResult("0.11", true, error = null, warning = LOW_SLIPPAGE_WARNING),
            ),
            ValueChangeModel(
                input = "5",
                expected = ValueChangeResult(
                    value = "5",
                    isConfirmEnabled = false,
                    error = resourceReference(R.string.swap_slippage_error),
                    warning = null,
                ),
            ),
            ValueChangeModel(
                input = "",
                expected = ValueChangeResult("", false, error = null, warning = null),
            ),
        )
    }

    @Nested
    inner class OnConfirmClick {

        @Test
        fun `GIVEN default value WHEN confirmed THEN reports no custom slippage`() = runTest {
            // Arrange
            val results = mutableListOf<BigDecimal?>()
            val model = createModel(onConfirm = { results += it })
            model.onValueChange("0.5")

            // Act
            model.onConfirmClick()

            // Assert
            assertThat(results).containsExactly(null)
            model.onDestroy()
        }

        @Test
        fun `GIVEN custom value WHEN confirmed THEN reports custom slippage`() = runTest {
            // Arrange
            val results = mutableListOf<BigDecimal?>()
            val model = createModel(onConfirm = { results += it })
            model.onValueChange("0.8")

            // Act
            model.onConfirmClick()

            // Assert
            assertThat(results).containsExactly(BigDecimal("0.8"))
            model.onDestroy()
        }

        @Test
        fun `GIVEN invalid value WHEN confirmed THEN nothing reported`() = runTest {
            // Arrange
            val results = mutableListOf<BigDecimal?>()
            val model = createModel(onConfirm = { results += it })
            model.onValueChange("5")

            // Act
            model.onConfirmClick()

            // Assert
            assertThat(results).isEmpty()
            model.onDestroy()
        }

        @Test
        fun `GIVEN read only WHEN confirmed THEN sheet closed without result`() = runTest {
            // Arrange
            val results = mutableListOf<BigDecimal?>()
            var closeCount = 0
            val model = createModel(
                provider = createProvider(isCustomSlippageSupported = false),
                onConfirm = { results += it },
                onCloseClick = { closeCount++ },
            )

            // Act
            model.onConfirmClick()

            // Assert
            assertThat(results to closeCount).isEqualTo(emptyList<BigDecimal?>() to 1)
            model.onDestroy()
        }
    }

    private fun TestScope.createModel(
        provider: SwapProvider = createProvider(),
        customSlippage: BigDecimal? = null,
        onConfirm: (BigDecimal?) -> Unit = {},
        onCloseClick: () -> Unit = {},
    ) = SwapSlippageModel(
        dispatchers = createTestingCoroutineDispatcherProvider(),
        paramsContainer = MutableParamsContainer(
            SwapSlippageComponent.Params(
                provider = provider,
                defaultPercent = defaultPercent,
                customSlippage = customSlippage,
                onConfirm = onConfirm,
                onCloseClick = onCloseClick,
            ),
        ),
    )

    private fun createProvider(isCustomSlippageSupported: Boolean = true) = SwapProvider(
        providerId = PROVIDER_ID,
        name = "1inch",
        type = ExchangeProviderType.DEX,
        imageLarge = "",
        termsOfUse = null,
        privacyPolicy = null,
        slippage = defaultPercent,
        isCustomSlippageSupported = isCustomSlippageSupported,
    )

    data class ValueChangeModel(val input: String, val expected: ValueChangeResult)

    data class ValueChangeResult(
        val value: String,
        val isConfirmEnabled: Boolean,
        val error: TextReference?,
        val warning: TextReference?,
    )

    private companion object {
        const val PROVIDER_ID = "1inch"
        val LOW_SLIPPAGE_WARNING = resourceReference(R.string.swap_slippage_warning_text)
    }
}
package com.tangem.feature.swap.component.slippage.model

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.combinedReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.feature.swap.component.slippage.SwapSlippageComponent
import com.tangem.feature.swap.domain.models.domain.acceptedSlippageOrNull
import com.tangem.feature.swap.model.SwapSlippage
import com.tangem.feature.swap.model.formatSlippagePercent
import com.tangem.feature.swap.models.states.SwapSlippageBottomSheetConfig
import com.tangem.feature.swap.models.states.SwapSlippagePresetUM
import com.tangem.feature.swap.presentation.R
import com.tangem.utils.StringsSigns
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import javax.inject.Inject

@ModelScoped
internal class SwapSlippageModel @Inject constructor(
    override val dispatchers: CoroutineDispatcherProvider,
    paramsContainer: ParamsContainer,
) : Model() {

    private val params: SwapSlippageComponent.Params = paramsContainer.require()

    val uiState: StateFlow<SwapSlippageBottomSheetConfig>
        field = MutableStateFlow(createInitialState())

    fun onValueChange(value: String) {
        uiState.update { state ->
            val valueState = state.valueState as? SwapSlippageBottomSheetConfig.Value.Edit ?: return@update state
            val percent = SwapSlippage.parseValid(value, params.provider)
            val isValid = percent != null
            state.copy(
                valueState = valueState.copy(value = value, isConfirmEnabled = isValid),
                error = resourceReference(R.string.swap_slippage_error).takeIf { value.isNotBlank() && !isValid },
                warning = lowSlippageWarning(percent),
            )
        }
    }

    fun onConfirmClick() {
        when (val valueState = uiState.value.valueState) {
            is SwapSlippageBottomSheetConfig.Value.Edit -> {
                val percent = SwapSlippage.parseValid(valueState.value, params.provider) ?: return
                val isDefault = percent.compareTo(params.defaultPercent) == 0
                params.onConfirm(percent.takeUnless { isDefault })
            }
            is SwapSlippageBottomSheetConfig.Value.ReadOnly -> params.onCloseClick()
        }
    }

    private fun createInitialState(): SwapSlippageBottomSheetConfig {
        val valueState = if (params.provider.isCustomSlippageSupported) {
            createEditState()
        } else {
            SwapSlippageBottomSheetConfig.Value.ReadOnly(
                slippage = stringReference(params.defaultPercent.formatSlippagePercent()),
            )
        }
        return SwapSlippageBottomSheetConfig(
            valueState = valueState,
            error = null,
            warning = lowSlippageWarning(params.provider.acceptedSlippageOrNull(params.customSlippage)),
            onConfirmClick = ::onConfirmClick,
        )
    }

    private fun lowSlippageWarning(percent: BigDecimal?): TextReference? {
        if (percent == null || !SwapSlippage.isLow(percent, params.defaultPercent)) return null
        return resourceReference(R.string.swap_slippage_warning_text)
    }

    private fun createEditState(): SwapSlippageBottomSheetConfig.Value.Edit {
        val defaultPercent = params.defaultPercent
        val currentPercent = params.provider.acceptedSlippageOrNull(params.customSlippage) ?: defaultPercent
        val presets = SwapSlippage.presets(defaultPercent).map { percent ->
            val value = percent.toPlainString()
            SwapSlippagePresetUM(
                value = value,
                label = if (percent.compareTo(defaultPercent) == 0) {
                    combinedReference(
                        resourceReference(R.string.common_default),
                        stringReference("${StringsSigns.NON_BREAKING_SPACE}${percent.formatSlippagePercent()}"),
                    )
                } else {
                    stringReference(percent.formatSlippagePercent())
                },
                onClick = { onValueChange(value) },
            )
        }
        return SwapSlippageBottomSheetConfig.Value.Edit(
            value = currentPercent.stripTrailingZeros().toPlainString(),
            isConfirmEnabled = false,
            presets = presets.toImmutableList(),
            onValueChange = ::onValueChange,
        )
    }
}
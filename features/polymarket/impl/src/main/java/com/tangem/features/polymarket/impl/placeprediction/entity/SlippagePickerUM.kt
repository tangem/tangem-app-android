package com.tangem.features.polymarket.impl.placeprediction.entity

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.utils.parseBigDecimalOrNull
import com.tangem.features.polymarket.impl.common.formatSlippagePercent
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import java.math.BigDecimal

internal data class SlippagePickerUM(
    val value: String,
    val error: TextReference?,
    val isConfirmEnabled: Boolean,
) {

    companion object {

        fun empty(): SlippagePickerUM = SlippagePickerUM(value = "", error = null, isConfirmEnabled = false)

        fun of(value: String): SlippagePickerUM {
            val percent = value.parseBigDecimalOrNull()
            val isAccepted = percent != null && percent >= MIN_SLIPPAGE_PERCENT && percent <= MAX_SLIPPAGE_PERCENT

            return SlippagePickerUM(
                value = value,
                error = if (percent != null && !isAccepted) {
                    resourceReference(R.string.prediction_place_slippage_error)
                } else {
                    null
                },
                isConfirmEnabled = isAccepted,
            )
        }
    }
}

internal val DEFAULT_SLIPPAGE_PERCENT: BigDecimal = BigDecimal("3")
internal val MIN_SLIPPAGE_PERCENT: BigDecimal = BigDecimal("0.1")
internal val MAX_SLIPPAGE_PERCENT: BigDecimal = BigDecimal("30")

internal const val SLIPPAGE_DECIMALS = 2

@Immutable
internal data class SlippagePresetUM(val percent: BigDecimal, val role: Role) {

    enum class Role { Default, Plain }
}

internal val SLIPPAGE_PRESETS: ImmutableList<SlippagePresetUM> = persistentListOf(
    SlippagePresetUM(percent = BigDecimal("0.1"), role = SlippagePresetUM.Role.Plain),
    SlippagePresetUM(percent = BigDecimal("0.25"), role = SlippagePresetUM.Role.Plain),
    SlippagePresetUM(percent = BigDecimal("0.5"), role = SlippagePresetUM.Role.Plain),
    SlippagePresetUM(percent = BigDecimal("1"), role = SlippagePresetUM.Role.Plain),
    SlippagePresetUM(percent = BigDecimal("2"), role = SlippagePresetUM.Role.Plain),
    SlippagePresetUM(percent = DEFAULT_SLIPPAGE_PERCENT, role = SlippagePresetUM.Role.Default),
)

internal fun SlippagePresetUM.label(): TextReference = when (role) {
    SlippagePresetUM.Role.Default -> resourceReference(
        id = R.string.prediction_place_slippage_default,
        formatArgs = wrappedList(percent.formatSlippagePercent()),
    )
    SlippagePresetUM.Role.Plain -> stringReference(percent.formatSlippagePercent())
}
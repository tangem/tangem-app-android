package com.tangem.features.tangempay.limit.setup

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
internal data class TangemPayCardLimitSetupUM(
    val isInitialDataLoading: Boolean,
    val amountFieldModel: AmountFieldModel,
    val subtitle: TextReference,
    val currencyCode: String,
    val presets: ImmutableList<LimitPresetUM>,
    val isSubmitButtonEnabled: Boolean,
    val isSubmitButtonLoading: Boolean,
    val onSubmitClick: () -> Unit,
    val onBackClick: () -> Unit,
) {

    @Immutable
    internal data class AmountFieldModel(
        val value: String,
        val decimals: Int,
        val onValueChange: (String) -> Unit,
    )

    @Immutable
    internal data class LimitPresetUM(
        val label: String,
        val onClick: () -> Unit,
    )

    companion object {
        fun stub(
            isLoading: Boolean = false,
            amountFieldModel: AmountFieldModel = AmountFieldModel(
                value = "5000",
                decimals = 2,
                onValueChange = {},
            ),
            subtitle: TextReference = TextReference.Str("Set a limit from $0 to $50,000"),
            currencyCode: String = "$",
            presets: ImmutableList<LimitPresetUM> = persistentListOf(
                LimitPresetUM(label = "$0", onClick = {}),
                LimitPresetUM(label = "$5,000", onClick = {}),
                LimitPresetUM(label = "$10,000", onClick = {}),
                LimitPresetUM(label = "$25,000", onClick = {}),
            ),
            submitButtonEnabled: Boolean = true,
            submitButtonLoading: Boolean = false,
        ): TangemPayCardLimitSetupUM = TangemPayCardLimitSetupUM(
            isInitialDataLoading = isLoading,
            amountFieldModel = amountFieldModel,
            subtitle = subtitle,
            currencyCode = currencyCode,
            presets = presets,
            isSubmitButtonEnabled = submitButtonEnabled,
            isSubmitButtonLoading = submitButtonLoading,
            onSubmitClick = {},
            onBackClick = {},
        )
    }
}
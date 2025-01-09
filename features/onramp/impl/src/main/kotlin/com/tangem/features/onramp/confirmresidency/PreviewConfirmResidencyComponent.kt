package com.tangem.features.onramp.confirmresidency

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.onramp.confirmresidency.entity.ConfirmResidencyUM
import com.tangem.features.onramp.confirmresidency.ui.ConfirmResidencyBottomSheet
import com.tangem.features.onramp.confirmresidency.ui.ConfirmResidencyBottomSheetContent
import kotlinx.coroutines.flow.MutableStateFlow

internal class PreviewConfirmResidencyComponent(
    initialState: ConfirmResidencyUM = ConfirmResidencyUM(
        country = "United States",
        countryFlagUrl = "https://hatscripts.github.io/circle-flags/flags/us.svg",
        isCountrySupported = true,
        primaryButtonConfig = ConfirmResidencyUM.ActionButtonConfig(onClick = {}, text = stringReference("Confirm")),
        secondaryButtonConfig = ConfirmResidencyUM.ActionButtonConfig(onClick = {}, text = stringReference("Change")),
    ),
) : ConfirmResidencyComponent {

    private val previewState: MutableStateFlow<ConfirmResidencyUM> = MutableStateFlow(initialState)

    override fun dismiss() {
        /* no-op */
    }

    @Composable
    override fun BottomSheet() {
        val state by previewState.collectAsStateWithLifecycle()
        val bottomSheetConfig = remember(key1 = this) {
            TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = ::dismiss,
                content = TangemBottomSheetConfigContent.Empty,
            )
        }
        ConfirmResidencyBottomSheet(
            config = bottomSheetConfig,
            content = { modifier ->
                ConfirmResidencyBottomSheetContent(model = state, modifier = modifier)
            },
        )
    }
}
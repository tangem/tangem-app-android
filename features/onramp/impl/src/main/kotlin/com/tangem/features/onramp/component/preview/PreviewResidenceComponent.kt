package com.tangem.features.onramp.component.preview

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.onramp.component.ResidenceComponent
import com.tangem.features.onramp.entity.ResidenceUM
import com.tangem.features.onramp.ui.ResidenceBottomSheet
import com.tangem.features.onramp.ui.ResidenceBottomSheetContent
import kotlinx.coroutines.flow.MutableStateFlow

internal class PreviewResidenceComponent(
    initialState: ResidenceUM = ResidenceUM(
        country = "United States",
        countryFlagUrl = "https://hatscripts.github.io/circle-flags/flags/us.svg",
        isCountrySupported = true,
        primaryButtonConfig = ResidenceUM.ActionButtonConfig(onClick = {}, text = stringReference("Confirm")),
        secondaryButtonConfig = ResidenceUM.ActionButtonConfig(onClick = {}, text = stringReference("Change")),
    ),
) : ResidenceComponent {

    private val previewState: MutableStateFlow<ResidenceUM> = MutableStateFlow(initialState)

    override fun dismiss() {
        /* no-op */
    }

    @Composable
    override fun BottomSheet() {
        val state by previewState.collectAsStateWithLifecycle()
        val bottomSheetConfig = TangemBottomSheetConfig(
            isShow = true,
            onDismissRequest = ::dismiss,
            content = TangemBottomSheetConfigContent.Empty,
        )
        ResidenceBottomSheet(
            config = bottomSheetConfig,
            content = { modifier ->
                ResidenceBottomSheetContent(model = state, modifier = modifier)
            },
        )
    }
}

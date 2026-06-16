package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.entity.TangemPayCardPageSettingV2
import com.tangem.features.tangempay.ui.components.TangemPayActionButton
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TangemPayCardPageSettingsButtonsBlock(
    settings: ImmutableList<TangemPayCardPageSettingV2>,
    modifier: Modifier = Modifier,
) {
    if (settings.isEmpty()) return
    Row(
        modifier = modifier.padding(vertical = TangemTheme.dimens2.x6),
        horizontalArrangement = Arrangement.Center,
    ) {
        settings.fastForEach { setting ->
            TangemPayActionButton(
                modifier = Modifier.then(if (setting.testTag != null) Modifier.testTag(setting.testTag) else Modifier),
                title = setting.title,
                iconRes = setting.iconRes,
                onClick = setting.onClick,
                isEnabled = setting.isEnabled,
                isLoading = setting.isLoading,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun TangemPayCardPageSettingsButtonsBlockPreview() {
    TangemThemePreviewRedesign {
        TangemPayCardPageSettingsButtonsBlock(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.secondary),
            settings = TangemPayCardPageSettingV2.stubList(),
        )
    }
}

@Preview(showBackground = true, name = "Frozen card")
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES, name = "Frozen card — dark")
@Composable
private fun TangemPayCardPageSettingsButtonsBlockFrozenPreview() {
    TangemThemePreviewRedesign {
        TangemPayCardPageSettingsButtonsBlock(
            modifier = Modifier
                .fillMaxWidth()
                .background(TangemTheme.colors3.bg.secondary),
            settings = TangemPayCardPageSettingV2.stubList(isFrozen = true),
        )
    }
}
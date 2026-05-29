package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.tangempay.entity.TangemPayCardPageSettingV2
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun TangemPayCardPageSettingsButtonsBlock(
    settings: ImmutableList<TangemPayCardPageSettingV2>,
    modifier: Modifier = Modifier,
) {
    if (settings.isEmpty()) return
    Row(
        modifier = modifier.padding(vertical = TangemTheme.dimens2.x6),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        settings.fastForEach { setting ->
            TangemPaySettingButton(
                setting = setting,
                modifier = Modifier.then(if (setting.testTag != null) Modifier.testTag(setting.testTag) else Modifier),
            )
        }
    }
}

@Composable
private fun TangemPaySettingButton(setting: TangemPayCardPageSettingV2, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(horizontal = TangemTheme.dimens2.x6),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x2),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TangemButton(
            variant = TangemButton.Variant.Material,
            size = TangemButton.Size.X14,
            onClick = setting.onClick,
            iconStart = TangemIconUM.Icon(
                iconRes = setting.iconRes,
                tintReference = { TangemTheme.colors3.icon.primary },
            ),
            isLoading = setting.isLoading,
            isEnabled = setting.isEnabled,
        )

        Text(
            text = setting.title.resolveAnnotatedReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
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
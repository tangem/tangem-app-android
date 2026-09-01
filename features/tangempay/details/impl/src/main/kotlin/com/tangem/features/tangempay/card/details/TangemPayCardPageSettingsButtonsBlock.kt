package com.tangem.features.tangempay.card.details

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
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.common.TangemPayActionButton
import com.tangem.features.tangempay.details.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun TangemPayCardPageSettingsButtonsBlock(
    settings: ImmutableList<TangemPayCardPageSetting>,
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
            settings = previewSettings(isFrozen = false),
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
            settings = previewSettings(isFrozen = true),
        )
    }
}

private fun previewSettings(isFrozen: Boolean): ImmutableList<TangemPayCardPageSetting> = persistentListOf(
    TangemPayCardPageSetting(
        id = TangemPayCardPageSetting.Id.Details,
        title = resourceReference(R.string.details_title),
        onClick = {},
        iconRes = CoreUiR.drawable.ic_visa_card_details_24,
    ),
    TangemPayCardPageSetting(
        id = TangemPayCardPageSetting.Id.Freeze,
        title = resourceReference(
            if (isFrozen) {
                R.string.tangem_pay_freeze_card_unfreeze
            } else {
                R.string.tangem_pay_freeze_card_freeze
            },
        ),
        onClick = {},
        iconRes = CoreUiR.drawable.ic_freeze_24,
        isLoading = true,
    ),
    TangemPayCardPageSetting(
        id = TangemPayCardPageSetting.Id.ChangePin,
        title = resourceReference(R.string.tangem_pay_pin_code_title),
        onClick = {},
        iconRes = CoreUiR.drawable.ic_card_pin_24,
        isEnabled = false,
    ),
)
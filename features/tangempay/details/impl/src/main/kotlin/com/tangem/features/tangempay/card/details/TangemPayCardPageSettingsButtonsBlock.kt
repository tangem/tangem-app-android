package com.tangem.features.tangempay.card.details

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.menu.TangemMenu
import com.tangem.core.ui.res.LocalHazeState
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.tangempay.common.TangemPayActionButton
import com.tangem.features.tangempay.details.impl.R
import dev.chrisbanes.haze.HazeState
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import com.tangem.core.ui.R as CoreUiR

@Composable
internal fun TangemPayCardPageSettingsButtonsBlock(
    settings: ImmutableList<TangemPayCardPageSetting>,
    modifier: Modifier = Modifier,
    actionsMenu: TangemMenu.UiState? = null,
    buttonsHazeState: HazeState = LocalHazeState.current,
) {
    if (settings.isEmpty()) return
    Row(
        modifier = modifier.padding(vertical = 24.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        settings.fastForEach { setting ->
            val buttonModifier = Modifier
                .weight(1f)
                .then(if (setting.testTag != null) Modifier.testTag(setting.testTag) else Modifier)
            if (setting.id == TangemPayCardPageSetting.Id.Actions && actionsMenu != null) {
                ActionsMenuButton(
                    setting = setting,
                    menu = actionsMenu,
                    buttonsHazeState = buttonsHazeState,
                    modifier = buttonModifier,
                )
            } else {
                SettingButton(
                    setting = setting,
                    onClick = setting.onClick,
                    hazeState = buttonsHazeState,
                    modifier = buttonModifier,
                )
            }
        }
    }
}

@Composable
private fun ActionsMenuButton(
    setting: TangemPayCardPageSetting,
    menu: TangemMenu.UiState,
    buttonsHazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    var isMenuExpanded by rememberSaveable { mutableStateOf(false) }
    Box(modifier = modifier) {
        SettingButton(
            setting = setting,
            onClick = {
                setting.onClick()
                isMenuExpanded = true
            },
            hazeState = buttonsHazeState,
            modifier = Modifier.fillMaxWidth(),
        )
        TangemMenu(
            state = menu,
            isExpanded = isMenuExpanded,
            onDismissRequest = { isMenuExpanded = false },
        )
    }
}

@Composable
private fun SettingButton(
    setting: TangemPayCardPageSetting,
    onClick: () -> Unit,
    hazeState: HazeState,
    modifier: Modifier = Modifier,
) {
    CompositionLocalProvider(LocalHazeState provides hazeState) {
        TangemPayActionButton(
            modifier = modifier,
            title = setting.title,
            icon = setting.iconVector?.let { TangemIconUM.Icon(imageVector = it) }
                ?: TangemIconUM.Icon(iconRes = setting.iconRes),
            onClick = onClick,
            isEnabled = setting.isEnabled,
            isLoading = setting.isLoading,
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
        iconRes = CoreUiR.drawable.ic_tangem_pay_card_details_24,
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
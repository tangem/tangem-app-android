package com.tangem.features.staking.impl.presentation.ui.bottomsheet

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.sheet.TangemBottomSheetTitle
import com.tangem.core.ui.components.inputrow.InputRowDefault
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.domain.staking.model.stakekit.PendingAction
import com.tangem.domain.staking.model.stakekit.action.StakingActionType
import com.tangem.features.staking.impl.R
import com.tangem.features.staking.impl.presentation.state.bottomsheet.StakingActionSelectionBottomSheetConfig
import com.tangem.features.staking.impl.presentation.state.utils.getPendingActionTitle
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun StakingActionSelectorBottomSheet(config: TangemBottomSheetConfig) {
    TangemBottomSheet<StakingActionSelectionBottomSheetConfig>(
        config = config,
        title = { content ->
            TangemBottomSheetTitle(title = content.title)
        },
        containerColor = TangemTheme.colors.background.tertiary,
    ) { content ->
        Column(
            modifier = Modifier
                .padding(
                    start = TangemTheme.dimens.spacing16,
                    end = TangemTheme.dimens.spacing16,
                    bottom = TangemTheme.dimens.spacing32,
                )
                .clip(TangemTheme.shapes.roundedCornersXMedium)
                .background(TangemTheme.colors.background.action),
        ) {
            content.actions.forEachIndexed { index, action ->
                InputRowDefault(
                    text = action.type.getPendingActionTitle(),
                    textColor = TangemTheme.colors.text.primary1,
                    showDivider = index != content.actions.lastIndex,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            content.onActionSelect(action)
                        },
                )
            }
        }
    }
}

// region Preview
@Composable
@Preview(showBackground = true)
@Preview(showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_StakingActionSelectorBottomSheet() {
    TangemThemePreview {
        StakingActionSelectorBottomSheet(
            config = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = {},
                content = StakingActionSelectionBottomSheetConfig(
                    title = resourceReference(R.string.common_select_action),
                    actions = persistentListOf(
                        PendingAction(
                            type = StakingActionType.CLAIM_REWARDS,
                            passthrough = "",
                            args = null,
                        ),
                        PendingAction(
                            type = StakingActionType.RESTAKE_REWARDS,
                            passthrough = "",
                            args = null,
                        ),
                    ),
                    onActionSelect = {},
                ),
            ),
        )
    }
}
// endregion Preview
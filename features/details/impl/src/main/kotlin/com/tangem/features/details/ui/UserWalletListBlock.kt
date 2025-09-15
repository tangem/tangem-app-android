package com.tangem.features.details.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.core.ui.R.*
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.components.bottomsheets.OptionsBottomSheet
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.component.preview.PreviewUserWalletListComponent
import com.tangem.features.details.entity.UserWalletListUM
import com.tangem.features.details.impl.R

@Composable
internal fun UserWalletListBlock(state: UserWalletListUM, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
    ) {
        state.userWallets.forEach { state ->
            key(state.id) {
                UserWalletItem(
                    modifier = Modifier.fillMaxWidth(),
                    state = state,
                )
            }
        }
        AddWalletButton(
            text = state.addNewWalletText,
            isInProgress = state.isWalletSavingInProgress,
            onClick = state.onAddNewWalletClick,
        )
    }

    AddWalletBottomSheet(state.addWalletBottomSheet)
}

@Composable
private fun AddWalletButton(
    text: TextReference,
    isInProgress: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BlockCard(
        modifier = modifier,
        onClick = onClick,
        enabled = !isInProgress,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            AnimatedContent(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                targetState = isInProgress,
                label = "Add wallet progress",
            ) { isInProgress ->
                if (isInProgress) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(TangemTheme.dimens.size24),
                        color = TangemTheme.colors.icon.accent,
                    )
                } else {
                    Icon(
                        modifier = Modifier.size(TangemTheme.dimens.size24),
                        painter = painterResource(id = R.drawable.ic_plus_24),
                        tint = TangemTheme.colors.icon.accent,
                        contentDescription = null,
                    )
                }
            }

            Text(
                text = text.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.accent,
            )
        }
    }
}

@Composable
private fun AddWalletBottomSheet(config: TangemBottomSheetConfig) {
    OptionsBottomSheet(
        config = config,
        title = resourceReference(string.auth_info_add_wallet_title),
        containerColor = TangemTheme.colors.background.tertiary,
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_UserWalletListBlock(
    @PreviewParameter(UserWalletListComponentPreviewProvider::class) component: UserWalletListComponent,
) {
    TangemThemePreview {
        component.Content(Modifier)
    }
}

private class UserWalletListComponentPreviewProvider : PreviewParameterProvider<UserWalletListComponent> {
    override val values: Sequence<UserWalletListComponent>
        get() = sequenceOf(
            PreviewUserWalletListComponent(),
        )
}
// endregion Preview
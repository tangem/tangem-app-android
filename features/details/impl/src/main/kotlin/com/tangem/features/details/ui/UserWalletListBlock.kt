package com.tangem.features.details.ui

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
import com.tangem.common.ui.userwallet.UserWalletItem
import com.tangem.core.ui.components.block.BlockCard
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
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
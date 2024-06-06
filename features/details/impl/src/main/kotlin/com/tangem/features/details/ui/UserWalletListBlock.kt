package com.tangem.features.details.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
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
        state.userWallets.forEach { model ->
            key(model.id) {
                UserWalletItem(
                    modifier = Modifier.fillMaxWidth(),
                    model = model,
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
private fun UserWalletItem(model: UserWalletListUM.UserWalletUM, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        onClick = model.onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = TangemTheme.dimens.size68)
                .padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            Image(
                modifier = Modifier
                    .width(TangemTheme.dimens.size24)
                    .height(TangemTheme.dimens.size36),
                painter = painterResource(id = model.imageResId),
                contentScale = ContentScale.FillBounds,
                contentDescription = null,
            )

            Column(
                modifier = Modifier.heightIn(min = TangemTheme.dimens.size40),
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.SpaceEvenly,
            ) {
                Text(
                    text = model.name,
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = model.information.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
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
package com.tangem.features.details.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.details.component.UserWalletListComponent
import com.tangem.features.details.impl.R

@Composable
internal fun UserWalletListBlock(component: UserWalletListComponent, modifier: Modifier = Modifier) {
    val state by component.state.collectAsStateWithLifecycle()

    BlockCard(
        modifier = modifier,
    ) {
        state.userWallets.forEach { model ->
            UserWalletItem(
                modifier = Modifier.fillMaxWidth(),
                model = model,
            )
        }
        AddWalletButton(
            text = state.addNewWalletText,
            onClick = state.onAddNewWalletClick,
        )
    }
}

@Composable
private fun UserWalletItem(model: UserWalletListComponent.State.UserWallet, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
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
                )
                Text(
                    text = model.information.resolveReference(),
                    style = TangemTheme.typography.caption2,
                    color = TangemTheme.colors.text.tertiary,
                )
            }
        }
    }
}

@Composable
private fun AddWalletButton(text: TextReference, onClick: () -> Unit, modifier: Modifier = Modifier) {
    BlockCard(
        modifier = modifier,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(all = TangemTheme.dimens.spacing12),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing12),
        ) {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = R.drawable.ic_plus_24),
                tint = TangemTheme.colors.icon.accent,
                contentDescription = null,
            )

            Text(
                text = text.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.accent,
            )
        }
    }
}

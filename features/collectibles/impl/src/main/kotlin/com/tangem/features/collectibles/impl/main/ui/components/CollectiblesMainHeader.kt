package com.tangem.features.collectibles.impl.main.ui.components

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.button.action.ActionButtons
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.row.TangemRow
import com.tangem.core.ui.ds2.row.TangemRowContentLead
import com.tangem.core.ui.ds2.row.TangemRowText
import com.tangem.core.ui.ds2.row.TangemRowTextRole
import com.tangem.core.ui.ds2.row.TangemRowVerticalAlignment
import com.tangem.core.ui.ds2.surface.TangemSurface
import com.tangem.core.ui.extensions.resolveAnnotatedReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chevron_right_20
import com.tangem.core.ui.res.generated.icons.ic_grid_20
import com.tangem.features.collectibles.impl.main.ui.preview.CollectiblesMainPreviewData
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun CollectiblesBalanceBlock(balance: CollectiblesMainUM.BalanceUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 56.dp, bottom = 32.dp)
            .padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = balance.fiatStyled.resolveAnnotatedReference(),
            style = TangemTheme.typography3.display.medium,
            color = TangemTheme.colors3.text.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            autoSize = TextAutoSize.StepBased(
                minFontSize = TangemTheme.typography3.heading.medium.fontSize,
                maxFontSize = TangemTheme.typography3.display.medium.fontSize,
            ),
        )
        Text(
            text = balance.crypto.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            maxLines = 1,
        )
    }
}

@Composable
internal fun CollectiblesActionsBlock(actions: ImmutableList<TangemButtonUM>, modifier: Modifier = Modifier) {
    ActionButtons(
        buttons = actions,
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
    )
}

@Composable
internal fun CollectiblesCollectionWidget(state: CollectiblesMainUM.CollectionUM, modifier: Modifier = Modifier) {
    TangemSurface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp),
        color = TangemTheme.colors3.bg.secondary,
        shape = RoundedCornerShape(24.dp),
    ) {
        TangemRow(
            contentLead = TangemRowContentLead.Start,
            verticalAlignment = TangemRowVerticalAlignment.Center,
            subtitleSlot = { TangemRowText(text = state.title, role = TangemRowTextRole.Title) },
            titleSlot = { TangemRowText(text = state.subtitle, role = TangemRowTextRole.Subtitle) },
            startSlot = {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(TangemTheme.colors3.bg.status.infoSubtle),
                    contentAlignment = Alignment.Center,
                ) {
                    TangemIcon(
                        tangemIconUM = TangemIconUM.Icon(
                            imageVector = Icons.ic_grid_20,
                            tintReference = { TangemTheme.colors3.icon.brand },
                        ),
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            endSlot = {
                TangemIcon(
                    tangemIconUM = TangemIconUM.Icon(
                        imageVector = Icons.ic_chevron_right_20,
                        tintReference = { TangemTheme.colors3.icon.secondary },
                    ),
                    modifier = Modifier.size(20.dp),
                )
            },
            onClick = state.onClick,
        )
    }
}

@Preview(widthDp = 360, showBackground = true)
@Preview(widthDp = 360, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun CollectiblesMainHeaderPreview() {
    val state = CollectiblesMainPreviewData.default
    TangemThemePreviewRedesign {
        Column {
            CollectiblesBalanceBlock(balance = state.balance)
            CollectiblesActionsBlock(actions = state.actions)
            state.collection?.let { CollectiblesCollectionWidget(state = it) }
        }
    }
}
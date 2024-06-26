package com.tangem.features.managetokens.ui

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.tangem.core.ui.components.buttons.SecondarySmallButton
import com.tangem.core.ui.components.buttons.SmallButtonConfig
import com.tangem.core.ui.components.rows.ChainRow
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.managetokens.component.preview.PreviewManageTokensComponent
import com.tangem.features.managetokens.entity.CurrencyItemUM
import com.tangem.features.managetokens.entity.ManageTokensUM
import com.tangem.features.managetokens.impl.R
import kotlinx.collections.immutable.ImmutableList

@Composable
internal fun ManageTokensScreen(state: ManageTokensUM, modifier: Modifier = Modifier) {
    BackHandler(onBack = state.popBack)

    Scaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors.background.primary,
        content = { innerPadding ->
            Currencies(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
                items = state.items,
            )
        },
    )
}

private const val CHEVRON_ROTATION_EXPANDED = 180f
private const val CHEVRON_ROTATION_COLLAPSED = 0f

@Composable
private fun Currencies(items: ImmutableList<CurrencyItemUM>, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
    ) {
        items(
            items = items,
            key = CurrencyItemUM::id,
        ) { item ->
            when (item) {
                is CurrencyItemUM.Basic -> {
                    ChainRow(
                        modifier = Modifier.fillMaxWidth(),
                        model = item.model,
                        action = {
                            IconButton(
                                modifier = Modifier.size(TangemTheme.dimens.size32),
                                onClick = item.onExpandClick,
                            ) {
                                val rotation by animateFloatAsState(
                                    targetValue = if (item.isExpanded) {
                                        CHEVRON_ROTATION_EXPANDED
                                    } else {
                                        CHEVRON_ROTATION_COLLAPSED
                                    },
                                    label = "chevron_rotation",
                                )

                                Icon(
                                    modifier = Modifier
                                        .rotate(rotation)
                                        .size(TangemTheme.dimens.size24),
                                    painter = painterResource(id = R.drawable.ic_chevron_24),
                                    tint = TangemTheme.colors.icon.informative,
                                    contentDescription = null,
                                )
                            }
                        },
                    )
                }
                is CurrencyItemUM.Custom -> {
                    ChainRow(
                        modifier = Modifier.fillMaxWidth(),
                        model = item.model,
                        action = {
                            SecondarySmallButton(
                                config = SmallButtonConfig(
                                    text = resourceReference(R.string.manage_tokens_remove),
                                    onClick = item.onRemoveClick,
                                ),
                            )
                        },
                    )
                }
            }
        }
    }
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_ManageTokens() {
    TangemThemePreview {
        PreviewManageTokensComponent().Content(Modifier.fillMaxWidth())
    }
}
// endregion Preview
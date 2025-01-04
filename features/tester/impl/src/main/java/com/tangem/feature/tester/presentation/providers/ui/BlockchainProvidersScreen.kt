package com.tangem.feature.tester.presentation.providers.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.components.rows.RowContentContainer
import com.tangem.core.ui.components.rows.RowText
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM.ProviderUM
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM.ProvidersUM

private const val CHEVRON_ROTATION_EXPANDED = 180f
private const val CHEVRON_ROTATION_COLLAPSED = 0f

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun BlockchainProvidersScreen(state: BlockchainProvidersUM) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.primary),
    ) {
        LazyColumn {
            stickyHeader {
                AppBarWithBackButton(
                    onBackClick = state.onBackClick,
                    text = stringResourceSafe(id = R.string.blockchain_providers),
                    containerColor = TangemTheme.colors.background.primary,
                )
            }

            items(
                items = state.blockchainProviders,
                key = ProvidersUM::blockchainId,
                contentType = { "BlockchainProviders" },
                itemContent = { BlockchainProvidersItem(it) },
            )
        }
    }
}

@Composable
private fun BlockchainProvidersItem(state: ProvidersUM) {
    var isExpanded by remember { mutableStateOf(state.isExpanded) }

    ExpandableBlockchainRow(
        state = state,
        modifier = Modifier.clickable { isExpanded = !isExpanded },
        content = { Providers(state = state, isExpanded = isExpanded) },
    )
}

@Composable
private fun ExpandableBlockchainRow(
    state: ProvidersUM,
    content: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
    ) {
        BlockchainRow(state)

        content()
    }
}

@Composable
private fun BlockchainRow(state: ProvidersUM) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = getActiveIconRes(state.blockchainId)),
                contentDescription = null,
                modifier = Modifier.size(TangemTheme.dimens.size36),
            )

            RowText(
                mainText = state.blockchainName,
                secondText = state.blockchainSymbol,
                accentMainText = true,
                accentSecondText = false,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }

        val rotation by animateFloatAsState(
            targetValue = if (state.isExpanded) {
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
}

@Composable
private fun Providers(state: ProvidersUM, isExpanded: Boolean) {
    AnimatedVisibility(
        visible = isExpanded,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        label = "providers_visibility",
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            state.providers.fastForEachIndexed { index, provider ->
                key(provider.name) {
                    ProviderItem(index = index, state = provider)

                    if (index == state.providers.lastIndex) SpacerH8()
                }
            }
        }
    }
}

@Composable
private fun ProviderItem(index: Int, state: ProviderUM) {
    RowContentContainer(
        icon = {
            Text(
                text = "${index + 1}.",
                color = TangemTheme.colors.text.accent,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = TangemTheme.typography.body2,
            )
        },
        text = {
            Text(
                text = state.name,
                color = TangemTheme.colors.text.primary1,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = TangemTheme.typography.body2,
            )
        },
        action = {},
    )
}
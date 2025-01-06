package com.tangem.feature.tester.presentation.providers.ui

import android.content.ClipData
import android.content.ClipDescription
import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.draganddrop.dragAndDropSource
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draganddrop.*
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.core.ui.components.rows.RowText
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefresh
import com.tangem.feature.tester.presentation.common.components.notification.CustomSetupNotification
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
            stickyHeader { TopBarWithRefresh(state = state.topBar) }

            if (state.topBar.refreshButton.isVisible) {
                item(key = "notification", contentType = "notification") {
                    CustomSetupNotification(
                        subtitle = resourceReference(
                            id = R.string.blockchain_providers_custom_setup_warning_description,
                        ),
                        modifier = Modifier
                            .animateItem()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 8.dp),
                    )
                }
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
        content = { Providers(state = state, isExpanded = isExpanded) },
        onExpandStateChange = { isExpanded = !isExpanded },
    )
}

@Composable
private fun ExpandableBlockchainRow(
    state: ProvidersUM,
    content: @Composable () -> Unit,
    onExpandStateChange: () -> Unit,
) {
    Column {
        BlockchainRow(state = state, onExpandStateChange = onExpandStateChange)

        content()
    }
}

@Composable
private fun BlockchainRow(state: ProvidersUM, onExpandStateChange: () -> Unit) {
    Row(
        modifier = Modifier
            .clickable { onExpandStateChange() }
            .padding(horizontal = 16.dp)
            .fillMaxWidth()
            .padding(vertical = 12.dp),
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
        modifier = Modifier.padding(horizontal = 16.dp),
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically(),
        label = "providers_visibility",
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.providers.fastForEachIndexed { index, provider ->
                key("${provider.name}_$index") {
                    ProviderItem(
                        index = index,
                        state = provider,
                        onDrop = { prev, new -> state.onDrop(state.blockchainId, prev, new) },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ProviderItem(index: Int, state: ProviderUM, onDrop: (Int, Int) -> Unit) {
    var isActive by remember(index) { mutableStateOf(false) }
    val color by animateColorAsState(
        targetValue = if (isActive) {
            TangemTheme.colors.icon.accent
        } else {
            TangemTheme.colors.icon.inactive
        },
        label = "provider_name_background_color",
    )

    val target = remember {
        ProvidersDnDTarget(
            index = index,
            onActiveStateChange = { isActive = it },
            onDrop = onDrop,
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "${index + 1}.",
            color = TangemTheme.colors.text.accent,
            overflow = TextOverflow.Ellipsis,
            maxLines = 1,
            style = TangemTheme.typography.body2,
        )

        Box(
            modifier = Modifier
                .background(color = color, shape = RoundedCornerShape(16.dp))
                .padding(vertical = 4.dp, horizontal = 8.dp)
                .dragAndDropSource {
                    detectTapGestures(
                        onLongPress = {
                            startTransfer(
                                DragAndDropTransferData(
                                    clipData = ClipData.newPlainText("provider index", index.toString()),
                                    flags = View.DRAG_FLAG_GLOBAL,
                                ),
                            )
                        },
                    )
                }
                .dragAndDropTarget(
                    shouldStartDragAndDrop = { event ->
                        event
                            .mimeTypes()
                            .contains(ClipDescription.MIMETYPE_TEXT_PLAIN)
                    },
                    target = target,
                ),
        ) {
            Text(
                text = state.name,
                color = TangemTheme.colors.text.primary1,
                overflow = TextOverflow.Ellipsis,
                maxLines = 1,
                style = TangemTheme.typography.body2,
            )
        }
    }
}

private class ProvidersDnDTarget(
    private val index: Int,
    private val onActiveStateChange: (Boolean) -> Unit,
    private val onDrop: (Int, Int) -> Unit,
) : DragAndDropTarget {

    override fun onEntered(event: DragAndDropEvent) {
        super.onEntered(event)
        onActiveStateChange(true)
    }

    override fun onExited(event: DragAndDropEvent) {
        super.onExited(event)
        onActiveStateChange(false)
    }

    override fun onEnded(event: DragAndDropEvent) {
        super.onEnded(event)
        onActiveStateChange(false)
    }

    override fun onDrop(event: DragAndDropEvent): Boolean {
        val new = event.toAndroidDragEvent().clipData.getItemAt(0).text.toString().toInt()

        if (index == new) return false

        onDrop(new, index)

        return true
    }
}
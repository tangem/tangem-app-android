package com.tangem.feature.tester.presentation.providers.ui

import android.content.ClipData
import android.content.ClipDescription
import android.view.View
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEachIndexed
import com.tangem.blockchain.common.network.providers.ProviderType
import com.tangem.core.ui.components.AdditionalTextInputDialogUM
import com.tangem.core.ui.components.DialogButtonUM
import com.tangem.core.ui.components.TextInputDialog
import com.tangem.core.ui.components.fields.SearchBar
import com.tangem.core.ui.components.fields.entity.SearchBarUM
import com.tangem.core.ui.components.rows.RowText
import com.tangem.core.ui.extensions.getActiveIconRes
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.feature.tester.impl.R
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefresh
import com.tangem.feature.tester.presentation.common.components.appbar.TopBarWithRefreshUM
import com.tangem.feature.tester.presentation.common.components.notification.CustomSetupNotification
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM.ProviderUM
import com.tangem.feature.tester.presentation.providers.entity.BlockchainProvidersUM.ProvidersUM
import kotlinx.collections.immutable.persistentListOf

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
                Header(topBar = state.topBar, searchBar = state.searchBar)
            }

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
private fun Header(topBar: TopBarWithRefreshUM, searchBar: SearchBarUM) {
    Column(
        modifier = Modifier.background(TangemTheme.colors.background.primary),
        verticalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing8),
    ) {
        TopBarWithRefresh(state = topBar)
        SearchBar(
            state = searchBar,
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = TangemTheme.dimens.spacing12),
        )
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
                        onDrop = { prev, new -> state.onDrop(prev, new) },
                    )
                }
            }

            AddProviderButton(state = state.addPublicProviderDialog)
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

    ProviderItemContainer {
        ProviderIndex(index = index)

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

@Composable
private fun ProviderIndex(index: Int) {
    Text(
        text = "${index + 1}.",
        color = TangemTheme.colors.text.accent,
        overflow = TextOverflow.Ellipsis,
        maxLines = 1,
        style = TangemTheme.typography.body2,
    )
}

@Composable
private fun AddProviderButton(state: BlockchainProvidersUM.AddPublicProviderDialogUM) {
    var isDialogVisible by remember { mutableStateOf(value = false) }

    ProviderItemContainer {
        Box(
            modifier = Modifier
                .clip(shape = RoundedCornerShape(16.dp))
                .clickable(onClick = { isDialogVisible = true })
                .weight(1f)
                .border(
                    border = BorderStroke(width = 2.dp, color = TangemTheme.colors.icon.accent),
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(horizontal = 8.dp, vertical = 2.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_plus_24),
                contentDescription = null,
                tint = TangemTheme.colors.icon.accent,
            )
        }
    }

    if (isDialogVisible) {
        AddPublicProviderDialog(
            state = state,
            onDismiss = { isDialogVisible = false },
        )
    }
}

@Composable
private fun AddPublicProviderDialog(state: BlockchainProvidersUM.AddPublicProviderDialogUM, onDismiss: () -> Unit) {
    var value by remember { mutableStateOf(value = TextFieldValue()) }

    TextInputDialog(
        fieldValue = value,
        confirmButton = DialogButtonUM(
            title = stringResourceSafe(R.string.common_save),
            enabled = value.text.isNotBlank() && !state.hasError,
            onClick = {
                state.onSaveClick(
                    buildString {
                        append(value.text.trim())

                        if (!endsWith("/")) append("/")
                    },
                )
                onDismiss()
            },
        ),
        onDismissDialog = onDismiss,
        onValueChange = {
            value = it
            state.onValueChange(it.text)
        },
        textFieldParams = AdditionalTextInputDialogUM(
            label = "Url",
            placeholder = "https://...",
            isError = state.hasError,
            errorText = "Invalid url",
        ),
        title = "Public provider",
        dismissButton = DialogButtonUM(
            title = stringResourceSafe(id = R.string.common_cancel),
            onClick = onDismiss,
        ),
    )
}

@Composable
private fun ProviderItemContainer(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
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

@Preview
@Composable
private fun Preview_BlockchainProvidersScreen() {
    TangemThemePreview {
        BlockchainProvidersScreen(
            state = BlockchainProvidersUM(
                topBar = TopBarWithRefreshUM(
                    titleResId = R.string.blockchain_providers,
                    onBackClick = {},
                    refreshButton = TopBarWithRefreshUM.RefreshButton(
                        isVisible = true,
                        onRefreshClick = { },
                    ),
                ),
                searchBar = SearchBarUM(
                    placeholderText = resourceReference(R.string.manage_tokens_search_placeholder),
                    query = "",
                    onQueryChange = {},
                    isActive = false,
                    onActiveChange = {},
                ),
                blockchainProviders = persistentListOf(
                    ProvidersUM(
                        blockchainId = "ETH",
                        blockchainName = "Ethereum",
                        blockchainSymbol = "ETH",
                        isExpanded = false,
                        onDrop = { _, _ -> },
                        addPublicProviderDialog = BlockchainProvidersUM.AddPublicProviderDialogUM(
                            hasError = false,
                            onValueChange = {},
                            onSaveClick = {},
                        ),
                        providers = persistentListOf(
                            ProviderUM(type = ProviderType.NowNodes),
                            ProviderUM(type = ProviderType.GetBlock),
                        ),
                    ),
                ),
            ),
        )
    }
}
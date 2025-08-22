package com.tangem.features.tokenreceive.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastForEach
import com.tangem.common.ui.notifications.NotificationUM
import com.tangem.core.res.getStringSafe
import com.tangem.core.ui.R
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH8
import com.tangem.core.ui.components.SpacerW12
import com.tangem.core.ui.components.SpacerW8
import com.tangem.core.ui.components.atoms.text.EllipsisText
import com.tangem.core.ui.components.atoms.text.TextEllipsis
import com.tangem.core.ui.components.buttons.small.TangemIconButton
import com.tangem.core.ui.components.icons.identicon.IdentIcon
import com.tangem.core.ui.components.notifications.Notification
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tokenreceive.entity.ReceiveAddress
import com.tangem.features.tokenreceive.ui.state.ReceiveAssetsUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableMap
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.launch

@Composable
internal fun TokenReceiveAssetsContent(assetsUM: ReceiveAssetsUM) {
    val snackbarHostState = remember(::SnackbarHostState)

    ContainerWithSnackbarHost(snackbarHostState = snackbarHostState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(color = TangemTheme.colors.background.tertiary)
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                ),
        ) {
            SpacerH8()

            Info(
                showMemoDisclaimer = assetsUM.showMemoDisclaimer,
                notificationConfigs = assetsUM.notificationConfigs,
            )

            SpacerH12()

            AddressBlock(
                onCopyClick = assetsUM.onCopyClick,
                onOpenQrCodeClick = assetsUM.onOpenQrCodeClick,
                addresses = assetsUM.addresses,
                snackbarHostState = snackbarHostState,
                fullName = assetsUM.fullName,
            )

            if (assetsUM.isEnsResultLoading) {
                LoadingBlock()
            }
        }
    }
}

@Composable
private fun Info(
    notificationConfigs: ImmutableList<NotificationUM>,
    showMemoDisclaimer: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(space = 16.dp),
    ) {
        if (showMemoDisclaimer) {
            Text(
                modifier = Modifier
                    .padding(horizontal = 18.dp)
                    .fillMaxWidth(),
                text = stringResourceSafe(R.string.receive_bottom_sheet_no_memo_required_message),
                style = TangemTheme.typography.caption1,
                color = TangemTheme.colors.text.tertiary,
                textAlign = TextAlign.Center,
            )
        }

        notificationConfigs.fastForEach {
            key(it.hashCode()) {
                Notification(config = it.config)
            }
        }
    }
}

@Composable
private fun AddressBlock(
    onOpenQrCodeClick: (id: Int) -> Unit,
    onCopyClick: (id: Int) -> Unit,
    addresses: ImmutableMap<Int, ReceiveAddress>,
    snackbarHostState: SnackbarHostState,
    fullName: String,
) {
    val hapticFeedback = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val resources = context.resources

    addresses.entries.toList().fastForEach { entry ->
        when (entry.value.type) {
            is ReceiveAddress.Type.Default -> {
                key(entry.key) {
                    AddressItem(
                        onCopyClick = {
                            onCopyClick(entry.key)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = resources.getStringSafe(
                                        R.string.wallet_notification_address_copied,
                                    ),
                                )
                            }
                        },
                        onOpenQrCodeClick = {
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenQrCodeClick(entry.key)
                        },
                        fullName = fullName,
                        address = entry.value.value,
                    )
                    SpacerH8()
                }
            }
            ReceiveAddress.Type.Ens -> {
                key(entry.key) {
                    EnsItem(
                        onCopyClick = {
                            onCopyClick(entry.key)
                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar(
                                    message = resources.getStringSafe(
                                        R.string.wallet_notification_address_copied,
                                    ),
                                )
                            }
                        },
                        address = entry.value.value,
                    )
                    SpacerH8()
                }
            }
        }
    }
}

@Composable
private fun AddressItem(
    onOpenQrCodeClick: () -> Unit,
    fullName: String,
    onCopyClick: () -> Unit,
    address: String,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors.background.action),
        onClick = onOpenQrCodeClick,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IdentIcon(
                address = address,
                modifier = Modifier
                    .size(size = 36.dp)
                    .clip(shape = RoundedCornerShape(18.dp)),
            )

            SpacerW12()

            Column(modifier = Modifier.weight(1f)) {
                EllipsisText(
                    text = stringResourceSafe(R.string.domain_receive_assets_onboarding_network_name, fullName),
                    ellipsis = TextEllipsis.Middle,
                    color = TangemTheme.colors.text.primary1,
                    style = TangemTheme.typography.subtitle1,
                )

                EllipsisText(
                    text = address,
                    ellipsis = TextEllipsis.Middle,
                    color = TangemTheme.colors.text.tertiary,
                    style = TangemTheme.typography.caption2,
                )
            }

            SpacerW12()

            TangemIconButton(
                modifier = Modifier.size(TangemTheme.dimens.size28),
                iconRes = R.drawable.ic_qrcode_new_24,
                onClick = onOpenQrCodeClick,
            )

            SpacerW8()

            TangemIconButton(
                modifier = Modifier.size(TangemTheme.dimens.size28),
                iconRes = R.drawable.ic_copy_new_24,
                onClick = onCopyClick,
            )
        }
    }
}

@Composable
private fun EnsItem(onCopyClick: () -> Unit, address: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors.background.action),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                modifier = Modifier.size(36.dp),
                imageVector = ImageVector.vectorResource(R.drawable.ic_ens_36),
                contentDescription = null,
            )

            SpacerW12()

            EllipsisText(
                modifier = Modifier.weight(1f),
                text = address,
                ellipsis = TextEllipsis.Middle,
                color = TangemTheme.colors.text.primary1,
                style = TangemTheme.typography.subtitle1,
            )

            TangemIconButton(
                modifier = Modifier.size(28.dp),
                iconRes = R.drawable.ic_copy_new_24,
                onClick = onCopyClick,
            )
        }
    }
}

@Composable
private fun LoadingBlock(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = TangemTheme.colors.background.action),
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                color = TangemTheme.colors.icon.informative,
                modifier = Modifier.padding(TangemTheme.dimens.spacing8),
            )
        }
    }
}

@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun Preview_TokenReceiveAssetsContent(
    @PreviewParameter(TokenReceiveAssetsContentProvider::class) params: ReceiveAssetsUM,
) {
    TangemThemePreview {
        TokenReceiveAssetsContent(assetsUM = params)
    }
}

private class TokenReceiveAssetsContentProvider : PreviewParameterProvider<ReceiveAssetsUM> {
    val address = ReceiveAddress(
        value = "0xe5178c7d4d0e861ed2e9414e045b501226b0de8d",
        type = ReceiveAddress.Type.Default(
            displayName = stringReference("Etherium address"),
        ),
    )
    private val config = ReceiveAssetsUM(
        notificationConfigs =
        persistentListOf(
            NotificationUM.Warning(
                title = stringReference("Send only XLM on the Ethereum network"),
                subtitle = resourceReference(R.string.receive_bottom_sheet_warning_message_description),
            ),
        ),
        addresses = persistentMapOf(
            0 to address,
            1 to address.copy(type = ReceiveAddress.Type.Ens, value = "papasha.eth"),
        ),
        showMemoDisclaimer = false,
        onCopyClick = {},
        onOpenQrCodeClick = {},
        isEnsResultLoading = true,
        fullName = "Etherium",
    )

    override val values: Sequence<ReceiveAssetsUM>
        get() = sequenceOf(config)
}
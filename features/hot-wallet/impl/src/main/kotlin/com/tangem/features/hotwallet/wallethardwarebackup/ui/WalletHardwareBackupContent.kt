package com.tangem.features.hotwallet.wallethardwarebackup.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.hotwallet.common.ui.OptionBlock
import com.tangem.features.hotwallet.impl.R
import com.tangem.features.hotwallet.wallethardwarebackup.entity.WalletHardwareBackupUM
import kotlinx.collections.immutable.persistentListOf

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun WalletHardwareBackupContent(state: WalletHardwareBackupUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.secondary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopAppBar(
            modifier = Modifier
                .statusBarsPadding(),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = TangemTheme.colors.background.secondary,
            ),
            navigationIcon = {
                IconButton(onClick = state.onBackClick) {
                    Icon(
                        painter = painterResource(R.drawable.ic_back_24),
                        tint = TangemTheme.colors.icon.primary1,
                        contentDescription = null,
                    )
                }
            },
            title = {
                Text(
                    text = stringResourceSafe(R.string.hw_backup_hardware_title),
                    style = TangemTheme.typography.subtitle1,
                    color = TangemTheme.colors.text.primary1,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(
                    start = 16.dp,
                    top = 4.dp,
                    end = 16.dp,
                ),
        ) {
            state.blocks.forEach { block ->
                OptionBlock(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    title = block.title.resolveReference(),
                    description = block.description.resolveReference(),
                    badge = block.titleLabel?.let {
                        { Label(it) }
                    },
                    enabled = true,
                    backgroundColor = TangemTheme.colors.background.primary,
                    onClick = block.onClick,
                )
            }
        }
        AnimatedVisibility(state.showPurchaseBlock) {
            PurchaseBlock(
                onBuyClick = state.onBuyClick,
            )
        }
    }
}

@Composable
private fun PurchaseBlock(onBuyClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .padding(
                horizontal = 20.dp,
                vertical = 16.dp,
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(end = 16.dp),
            text = stringResourceSafe(R.string.wallet_add_hardware_purchase),
            style = TangemTheme.typography.button,
            color = TangemTheme.colors.text.primary1,
        )

        SecondaryButton(
            text = stringResourceSafe(R.string.wallet_import_buy_title),
            onClick = onBuyClick,
            size = TangemButtonSize.RoundedAction,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewWalletHardwareBackupContent() {
    TangemThemePreview {
        WalletHardwareBackupContent(
            state = WalletHardwareBackupUM(
                onBackClick = { },
                blocks = persistentListOf(
                    WalletHardwareBackupUM.Block(
                        title = stringReference("Create new wallet"),
                        titleLabel = LabelUM(
                            text = resourceReference(R.string.common_recommended),
                            style = LabelStyle.ACCENT,
                        ),
                        description = stringReference(
                            "Create a new secure wallet and transfer your funds for extra protection.",
                        ),
                        onClick = { },
                    ),
                    WalletHardwareBackupUM.Block(
                        title = stringReference("Upgrade current wallet"),
                        titleLabel = null,
                        description = stringReference("Move your current wallet into Tangem Wallet."),
                        onClick = { },
                    ),
                ),
                showPurchaseBlock = true,
                onBuyClick = { },
            ),
        )
    }
}
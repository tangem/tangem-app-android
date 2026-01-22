package com.tangem.features.hotwallet.wallethardwarebackup.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.ForceDarkTheme
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
            Banner(onBuyClick = state.onBuyClick)

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
    }
}

@Suppress("LongMethod")
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Banner(onBuyClick: () -> Unit, modifier: Modifier = Modifier) {
    ForceDarkTheme {
        Column(
            modifier = modifier
                .background(
                    color = TangemTheme.colors.background.primary,
                    shape = RoundedCornerShape(16.dp),
                ),
        ) {
            Column(
                modifier = Modifier
                    .padding(
                        start = 12.dp,
                        top = 20.dp,
                        end = 12.dp,
                    ),
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth(),
                    text = stringResourceSafe(R.string.common_tangem_wallet),
                    style = TangemTheme.typography.h3,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )

                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            top = 4.dp,
                        ),
                    text = stringResourceSafe(R.string.hw_backup_banner_description),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.tertiary,
                    textAlign = TextAlign.Center,
                )

                FlowRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = 24.dp,
                            top = 16.dp,
                            end = 24.dp,
                        ),
                    horizontalArrangement = Arrangement.Center,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FeatureItem(
                        iconResId = R.drawable.ic_shield_check_16,
                        text = resourceReference(R.string.welcome_create_wallet_feature_class),
                    )
                    FeatureItem(
                        iconResId = R.drawable.ic_flash_16,
                        text = resourceReference(R.string.welcome_create_wallet_feature_delivery),
                    )
                    FeatureItem(
                        iconResId = R.drawable.ic_sparkles_16,
                        text = resourceReference(R.string.welcome_create_wallet_feature_seedphrase),
                    )
                }

                Box(
                    modifier = Modifier
                        .padding(
                            start = 8.dp,
                            top = 12.dp,
                            end = 8.dp,
                            bottom = 20.dp,
                        ),
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.img_tangem_cards_vertical),
                        contentDescription = null,
                    )
                    SecondaryButton(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter),
                        text = stringResourceSafe(R.string.details_buy_wallet),
                        onClick = onBuyClick,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureItem(@DrawableRes iconResId: Int, text: TextReference) {
    Row(
        modifier = Modifier
            .wrapContentWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            painter = painterResource(iconResId),
            tint = TangemTheme.colors.icon.accent,
            contentDescription = null,
        )
        Text(
            text = text.resolveReference(),
            style = TangemTheme.typography.caption1,
            color = TangemTheme.colors.text.secondary,
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
                onBuyClick = { },
            ),
        )
    }
}
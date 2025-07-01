package com.tangem.features.createwalletselection.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.buttons.common.TangemButton
import com.tangem.core.ui.components.buttons.common.TangemButtonIconPosition
import com.tangem.core.ui.components.buttons.common.TangemButtonSize
import com.tangem.core.ui.components.buttons.common.TangemButtonsDefaults
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.createwalletselection.entity.CreateWalletSelectionUM
import com.tangem.features.createwalletselection.impl.R

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateWalletSelectionContent(state: CreateWalletSelectionUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(TangemTheme.colors.background.primary)
            .fillMaxSize()
            .systemBarsPadding(),
    ) {
        TopAppBar(
            modifier = Modifier
                .statusBarsPadding(),
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = TangemTheme.colors.background.primary,
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
            title = { },
            actions = {
                Text(
                    modifier = Modifier
                        .padding(16.dp),
                    text = stringResourceSafe(R.string.wallet_create_nav_info_title),
                    style = TangemTheme.typography.body1,
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
                    top = 24.dp,
                    end = 16.dp,
                ),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                text = stringResourceSafe(R.string.wallet_create_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            WalletBlock(
                modifier = Modifier
                    .padding(top = 24.dp),
                title = stringResourceSafe(R.string.wallet_create_mobile_title),
                description = stringResourceSafe(R.string.wallet_create_mobile_description),
                badge = {
                    Box(
                        modifier = Modifier
                            .background(
                                color = TangemTheme.colors.field.focused,
                                shape = TangemTheme.shapes.roundedCorners8,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResourceSafe(R.string.common_free),
                            style = TangemTheme.typography.caption1,
                            color = TangemTheme.colors.text.secondary,
                        )
                    }
                },
                onClick = state.onMobileWalletClick,
            )
            WalletBlock(
                title = stringResourceSafe(R.string.wallet_create_hardware_title),
                description = stringResourceSafe(R.string.wallet_create_hardware_description),
                badge = {
                    Box(
                        modifier = Modifier
                            .background(
                                color = TangemTheme.colors.text.accent.copy(alpha = 0.1f),
                                shape = TangemTheme.shapes.roundedCorners8,
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                    ) {
                        Text(
                            text = stringResourceSafe(R.string.wallet_create_hardware_badge, state.hardwareWalletPrice),
                            style = TangemTheme.typography.caption1,
                            color = TangemTheme.colors.text.accent,
                        )
                    }
                },
                onClick = state.onHardwareWalletClick,
            )
        }
        AlreadyHaveTangemWalletBlock(
            onScanClick = state.onScanClick,
            isScanInProgress = state.isScanInProgress,
        )
    }
}

@Composable
private fun WalletBlock(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
    badge: @Composable () -> Unit,
    onClick: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(
                color = TangemTheme.colors.background.secondary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Row {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            badge()
        }
        Text(
            modifier = Modifier
                .padding(top = 4.dp),
            text = description,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
    }
}

@Composable
private fun AlreadyHaveTangemWalletBlock(
    onScanClick: () -> Unit,
    isScanInProgress: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(
                color = TangemTheme.colors.field.primary,
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
            text = stringResourceSafe(R.string.wallet_create_scan_question),
            style = TangemTheme.typography.button,
            color = TangemTheme.colors.text.primary1,
        )
        TangemButton(
            modifier = Modifier
                .wrapContentWidth(),
            text = stringResourceSafe(R.string.wallet_create_scan_title),
            onClick = onScanClick,
            icon = TangemButtonIconPosition.End(iconResId = R.drawable.ic_tangem_24),
            size = TangemButtonSize.RoundedAction,
            showProgress = isScanInProgress,
            colors = TangemButtonsDefaults.secondaryButtonColors,
            textStyle = TangemTheme.typography.subtitle1,
            enabled = true,
            animateContentChange = true,
        )
    }
}

@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewCreateWalletContent() {
    TangemThemePreview {
        CreateWalletSelectionContent(
            state = CreateWalletSelectionUM(
                onBackClick = {},
                onMobileWalletClick = {},
                onHardwareWalletClick = {},
                onScanClick = {},
            ),
        )
    }
}
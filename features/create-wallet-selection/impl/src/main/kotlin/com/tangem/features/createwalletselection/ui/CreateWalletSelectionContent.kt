package com.tangem.features.createwalletselection.ui

import android.content.res.Configuration
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.tangem.features.createwalletselection.entity.CreateWalletSelectionUM
import com.tangem.features.createwalletselection.impl.R
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Suppress("LongMethod")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CreateWalletSelectionContent(state: CreateWalletSelectionUM, modifier: Modifier = Modifier) {
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
            title = { },
            actions = {
                Text(
                    modifier = Modifier
                        .padding(16.dp),
                    text = stringResourceSafe(R.string.wallet_add_support_title),
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
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 16.dp,
                    top = 24.dp,
                    end = 16.dp,
                ),
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = 16.dp,
                        end = 16.dp,
                        bottom = 24.dp,
                    ),
                text = stringResourceSafe(R.string.wallet_add_common_title),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
                textAlign = TextAlign.Center,
            )
            state.blocks.forEach { block ->
                WalletBlock(
                    modifier = Modifier
                        .padding(top = 8.dp),
                    title = block.title.resolveReference(),
                    description = block.description.resolveReference(),
                    features = block.features,
                    badge = block.titleLabel?.let {
                        { Label(it) }
                    },
                    onClick = block.onClick,
                )
            }
        }
        AnimatedVisibility(state.shouldShowAlreadyHaveWallet) {
            AlreadyHaveTangemWalletBlock(
                onBuyClick = state.onBuyClick,
                isScanInProgress = state.isScanInProgress,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WalletBlock(
    title: String,
    description: String,
    onClick: () -> Unit,
    features: ImmutableList<CreateWalletSelectionUM.Feature>,
    modifier: Modifier = Modifier,
    badge: @Composable (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(TangemTheme.shapes.roundedCornersXMedium)
            .background(
                color = TangemTheme.colors.background.primary,
                shape = TangemTheme.shapes.roundedCornersXMedium,
            )
            .clickable(onClick = onClick)
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = title,
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            badge?.invoke()
        }
        Text(
            modifier = Modifier
                .padding(top = 4.dp),
            text = description,
            style = TangemTheme.typography.body2,
            color = TangemTheme.colors.text.tertiary,
        )
        if (features.isNotEmpty()) {
            HorizontalDivider(
                modifier = Modifier.padding(top = 12.dp),
                thickness = 0.5.dp,
                color = TangemTheme.colors.stroke.primary,
            )
            features.forEach { feature ->
                Feature(
                    feature = feature,
                    modifier = Modifier
                        .padding(top = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun Feature(feature: CreateWalletSelectionUM.Feature, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(TangemTheme.dimens.size16),
            painter = painterResource(id = feature.iconResId),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Text(
            modifier = Modifier
                .weight(1f, fill = false)
                .padding(start = 6.dp),
            text = feature.title.resolveReference(),
            style = TangemTheme.typography.caption2,
            color = TangemTheme.colors.text.secondary,
        )
    }
}

@Composable
private fun AlreadyHaveTangemWalletBlock(
    onBuyClick: () -> Unit,
    isScanInProgress: Boolean,
    modifier: Modifier = Modifier,
) {
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
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )

        SecondaryButton(
            text = stringResourceSafe(R.string.wallet_import_buy_title),
            onClick = onBuyClick,
            size = TangemButtonSize.RoundedAction,
            showProgress = isScanInProgress,
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
                onBackClick = { },
                blocks = persistentListOf(
                    CreateWalletSelectionUM.Block(
                        title = stringReference("Hardware wallet very long title"),
                        titleLabel = LabelUM(
                            text = resourceReference(R.string.common_recommended),
                            style = LabelStyle.ACCENT,
                        ),
                        description = resourceReference(R.string.wallet_add_hardware_description),
                        features = persistentListOf(
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_add_wallet_16,
                                title = resourceReference(R.string.wallet_add_hardware_info_create),
                            ),
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_import_seed_16,
                                title = resourceReference(R.string.wallet_add_import_seed_phrase),
                            ),
                        ),
                        onClick = { },
                    ),
                    CreateWalletSelectionUM.Block(
                        title = resourceReference(R.string.wallet_create_mobile_title),
                        titleLabel = null,
                        description = resourceReference(R.string.wallet_add_mobile_description),
                        features = persistentListOf(
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_mobile_wallet_16,
                                title = resourceReference(R.string.hw_create_title),
                            ),
                            CreateWalletSelectionUM.Feature(
                                iconResId = R.drawable.ic_import_seed_16,
                                title = resourceReference(R.string.wallet_add_import_seed_phrase),
                            ),
                        ),
                        onClick = { },
                    ),
                ),
                shouldShowAlreadyHaveWallet = true,
                onBuyClick = { },
            ),
        )
    }
}
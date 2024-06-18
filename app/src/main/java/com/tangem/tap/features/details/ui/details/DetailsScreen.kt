package com.tangem.tap.features.details.ui.details

import android.content.res.Configuration
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.SpacerHMax
import com.tangem.core.ui.event.EventEffect
import com.tangem.core.ui.event.StateEvent
import com.tangem.core.ui.event.consumedEvent
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R
import kotlinx.collections.immutable.toImmutableList

@Composable
internal fun DetailsScreen(state: DetailsScreenState, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val snackbarHostState = remember { SnackbarHostState() }

    SettingsScreensScaffold(
        modifier = modifier,
        snackbarHostState = snackbarHostState,
        content = { Content(state = state) },
        onBackClick = onBackClick,
    )

    ShowSnackbarIfNeeded(
        snackbarHostState = snackbarHostState,
        messageEvent = state.showSnackbar,
    )
}

@Composable
private fun Content(state: DetailsScreenState, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenTitle(titleRes = R.string.details_title)
            SpacerH(height = TangemTheme.dimens.spacing36)
            SettingsItems(
                items = state.elements,
            )
            SpacerHMax()
            TangemSocialAccounts(
                links = state.tangemLinks,
                onSocialNetworkClick = state.onSocialNetworkClick,
            )
            SpacerH(height = TangemTheme.dimens.spacing12)
            TangemAppVersion(
                appNameRes = state.appNameRes,
                version = state.tangemVersion,
            )
            SpacerH(height = TangemTheme.dimens.spacing16)
        }
    }
}

@Composable
private fun SettingsItems(items: List<SettingsItem>) {
    items.forEach { item ->
        if (item.isLarge) {
            LargeDetailsItem(item)
        } else {
            DetailsItem(item)
        }
    }
}

@Composable
private fun LargeDetailsItem(item: SettingsItem) {
    Row(
        modifier = Modifier
            .clickable(onClick = item.onClick)
            .padding(horizontal = TangemTheme.dimens.spacing20)
            .heightIn(min = TangemTheme.dimens.size84)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing20),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                color = TangemTheme.colors.icon.informative,
            )
        } else {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = item.iconResId),
                contentDescription = item.title.resolveReference(),
                tint = TangemColorPalette.Azure,
            )
        }
        Column(
            modifier = Modifier.heightIn(min = TangemTheme.dimens.size56),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(
                space = TangemTheme.dimens.spacing4,
                alignment = Alignment.CenterVertically,
            ),
        ) {
            Text(
                text = item.title.resolveReference(),
                style = TangemTheme.typography.h3,
                color = TangemTheme.colors.text.primary1,
            )

            if (item.subtitle != null) {
                Text(
                    text = item.subtitle.resolveReference(),
                    style = TangemTheme.typography.body1,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun DetailsItem(item: SettingsItem) {
    Row(
        modifier = Modifier
            .clickable(enabled = !item.showProgress, onClick = item.onClick)
            .padding(horizontal = TangemTheme.dimens.spacing20)
            .heightIn(min = TangemTheme.dimens.size56)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens.spacing20),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.showProgress) {
            CircularProgressIndicator(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                color = TangemTheme.colors.icon.informative,
            )
        } else {
            Icon(
                modifier = Modifier.size(TangemTheme.dimens.size24),
                painter = painterResource(id = item.iconResId),
                contentDescription = item.title.resolveReference(),
                tint = TangemTheme.colors.icon.secondary,
            )
        }

        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.SpaceAround,
        ) {
            Text(
                text = item.title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )

            if (item.subtitle != null) {
                Text(
                    text = item.subtitle.resolveReference(),
                    style = TangemTheme.typography.body2,
                    color = TangemTheme.colors.text.secondary,
                )
            }
        }
    }
}

@Composable
private fun TangemSocialAccounts(links: List<SocialNetworkLink>, onSocialNetworkClick: (SocialNetworkLink) -> Unit) {
    LazyRow(
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = TangemTheme.dimens.spacing8),
    ) {
        items(links) {
            val onClick = remember(it) {
                { onSocialNetworkClick(it) }
            }

            IconButton(
                modifier = Modifier
                    .padding(horizontal = TangemTheme.dimens.spacing4)
                    .size(TangemTheme.dimens.size32),
                onClick = onClick,
            ) {
                Icon(
                    modifier = Modifier.size(TangemTheme.dimens.size24),
                    painter = painterResource(id = it.network.iconRes),
                    tint = TangemTheme.colors.icon.informative,
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
private fun ShowSnackbarIfNeeded(snackbarHostState: SnackbarHostState, messageEvent: StateEvent<TextReference>) {
    var message: TextReference? by remember { mutableStateOf(value = null) }
    val resolvedMessage by rememberUpdatedState(newValue = message?.resolveReference())

    LaunchedEffect(resolvedMessage) {
        resolvedMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    EventEffect(messageEvent) {
        message = it
    }
}

@Composable
private fun TangemAppVersion(appNameRes: Int, version: String, modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.padding(horizontal = TangemTheme.dimens.spacing16),
        text = "${stringResource(id = appNameRes)} $version",
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.tertiary,
    )
}

// region Preview
@Preview(showBackground = true, widthDp = 360, heightDp = 900)
@Preview(showBackground = true, widthDp = 360, heightDp = 900, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun DetailsScreenPreview(@PreviewParameter(DetailsScreenStateProvider::class) param: DetailsScreenState) {
    TangemThemePreview {
        DetailsScreen(param, onBackClick = {})
    }
}

private class DetailsScreenStateProvider : CollectionPreviewParameterProvider<DetailsScreenState>(
    collection = buildList {
        DetailsScreenState(
            elements = buildList {
                SettingsItem.WalletConnect({}).let(::add)
                SettingsItem.AddWallet(showProgress = false, {}).let(::add)
                SettingsItem.LinkMoreCards({}).let(::add)
                SettingsItem.CardSettings({}).let(::add)
                SettingsItem.AppSettings({}).let(::add)
                // removed chat in task [REDACTED_TASK_KEY]
                // SettingsItem.Chat({}).let(::add)
                SettingsItem.SendFeedback({}).let(::add)
                SettingsItem.ReferralProgram({}).let(::add)
                SettingsItem.TermsOfService({}).let(::add)
            }.toImmutableList(),
            tangemLinks = TangemSocialAccounts.accountsEn,
            tangemVersion = "Tangem 2.14.12 (343)",
            showSnackbar = consumedEvent(),
            onSocialNetworkClick = {},
        ).let(::add)

        DetailsScreenState(
            elements = buildList {
                SettingsItem.WalletConnect({}).let(::add)
                SettingsItem.AddWallet(showProgress = true, {}).let(::add)
                SettingsItem.CardSettings({}).let(::add)
                SettingsItem.AppSettings({}).let(::add)
                // removed chat in task [REDACTED_TASK_KEY]
                // SettingsItem.Chat({}).let(::add)
                SettingsItem.SendFeedback({}).let(::add)
                SettingsItem.TermsOfService({}).let(::add)
            }.toImmutableList(),
            tangemLinks = TangemSocialAccounts.accountsRu,
            tangemVersion = "Tangem 2.14.12 (343)",
            showSnackbar = consumedEvent(),
            onSocialNetworkClick = {},
        ).let(::add)
    },
)
// endregion Preview
package com.tangem.tap.features.details.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R

@Composable
fun DetailsScreen(
    state: DetailsScreenState,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    SettingsScreensScaffold(
        content = { Content(state = state, modifier = modifier) },
        onBackClick = onBackPressed,
    )
}

@Composable
fun Content(
    state: DetailsScreenState,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTitle(titleRes = R.string.details_title, modifier.padding(bottom = 52.dp))
        state.elements.map {
            if (it == SettingsElement.WalletConnect) {
                WalletConnectDetailsItem(
                    onItemsClick = state.onItemsClick,
                    modifier = modifier,
                )
            } else {
                DetailsItem(
                    item = it,
                    appCurrency = state.appCurrency,
                    onItemsClick = state.onItemsClick,
                    modifier = modifier,
                )
            }
        }
        Spacer(modifier = modifier.weight(1f))
        TangemSocialAccounts(state.tangemLinks, state.onSocialNetworkClick)
        Spacer(modifier = Modifier.size(12.dp))
        Text(
            text = "${stringResource(id = state.appNameRes)} ${state.tangemVersion}",
            style = TangemTypography.caption,
            color = colorResource(id = R.color.text_tertiary),
            modifier = modifier.padding(start = 16.dp, end = 16.dp, bottom = 40.dp),
        )
    }
}

@Composable
fun WalletConnectDetailsItem(
    onItemsClick: (SettingsElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .defaultMinSize(minHeight = 84.dp)
            .fillMaxWidth()
            .clickable { onItemsClick(SettingsElement.WalletConnect) },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_walletconnect),
            contentDescription = stringResource(id = R.string.wallet_connect_title),
            modifier = modifier.padding(start = 20.dp, end = 20.dp),
            tint = colorResource(id = R.color.all_colors_azure),
        )
        Column(
            modifier = modifier.defaultMinSize(minHeight = 56.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(id = R.string.wallet_connect_title),
                modifier = modifier.padding(end = 20.dp, bottom = 4.dp),
                style = TangemTypography.headline3,
                color = colorResource(id = R.color.text_primary_1),
            )
            Text(
                text = stringResource(id = R.string.wallet_connect_subtitle),
                modifier = modifier.padding(end = 20.dp, bottom = 4.dp),
                style = TangemTypography.body1,
                color = colorResource(id = R.color.text_secondary),
            )
        }
    }
}

@Composable
fun DetailsItem(
    item: SettingsElement,
    appCurrency: String,
    onItemsClick: (SettingsElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .fillMaxWidth()
            .clickable { onItemsClick(item) },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = stringResource(id = item.titleRes),
            modifier = modifier.padding(start = 20.dp, end = 20.dp),
            tint = colorResource(id = R.color.icon_secondary),
        )
        Column(modifier = modifier.padding(end = 20.dp)) {
            Text(
                text = stringResource(id = item.titleRes),
                modifier = modifier,
                style = TangemTypography.subtitle1,
                color = colorResource(id = R.color.text_primary_1),
            )
            if (item == SettingsElement.AppCurrency) {
                Text(
                    text = appCurrency,
                    modifier = modifier,
                    style = TangemTypography.body2,
                    color = colorResource(id = R.color.text_secondary),
                )
            }
        }
    }
}

@Composable
fun TangemSocialAccounts(
    links: List<SocialNetworkLink>,
    onSocialNetworkClick: (SocialNetworkLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.padding(start = 8.dp, end = 8.dp),
    ) {
        items(links) {
            Icon(
                painter = painterResource(id = it.network.iconRes),
                contentDescription = "",
                modifier = modifier
                    .padding(8.dp)
                    .clickable { onSocialNetworkClick(it) },
                tint = colorResource(id = R.color.icon_informative),
            )
        }
    }
}

@Composable
@Preview
fun Preview() {
    DetailsScreen(
        state = DetailsScreenState(
            elements = SettingsElement.values().toList(),
            tangemLinks = TangemSocialAccounts.accountsEn,
            tangemVersion = "Tangem 2.14.12 (343)",
            appCurrency = "Dollar",
            onItemsClick = {}, onSocialNetworkClick = {},
        ),
        onBackPressed = {},
    )
}

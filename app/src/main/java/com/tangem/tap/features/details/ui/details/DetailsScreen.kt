package com.tangem.tap.features.details.ui.details

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
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
import androidx.compose.material.SnackbarHost
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SystemBarsEffect
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.tap.common.compose.TangemTypography
import com.tangem.tap.features.details.ui.common.ScreenTitle
import com.tangem.tap.features.details.ui.common.SettingsScreensScaffold
import com.tangem.wallet.R
import kotlinx.coroutines.launch

@Composable
fun DetailsScreen(state: DetailsScreenState, onBackClick: () -> Unit) {
    SystemBarsEffect {
        setSystemBarsColor(color = TangemColorPalette.Light1)
    }

    SettingsScreensScaffold(
        content = { Content(state = state) },
        onBackClick = onBackClick,
    )
}

@Composable
fun Content(state: DetailsScreenState) {
    Box {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenTitle(titleRes = R.string.details_title, Modifier.padding(bottom = 52.dp))
            state.elements.map { element ->
                if (element == SettingsElement.WalletConnect) {
                    WalletConnectDetailsItem(onItemsClick = state.onItemsClick)
                } else {
                    DetailsItem(
                        item = element,
                        appCurrency = state.appCurrency,
                        onItemsClick = { state.onItemsClick(element) },
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            TangemSocialAccounts(state.tangemLinks, state.onSocialNetworkClick)
            Spacer(modifier = Modifier.size(12.dp))
            Text(
                text = "${stringResource(id = state.appNameRes)} ${state.tangemVersion}",
                style = TangemTypography.caption,
                color = colorResource(id = R.color.text_tertiary),
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 40.dp),
            )
        }
        ShowSnackbarIfNeeded(state.showErrorSnackbar.value)
    }
}

@Composable
fun WalletConnectDetailsItem(onItemsClick: (SettingsElement) -> Unit) {
    Row(
        modifier = Modifier
            .defaultMinSize(minHeight = 84.dp)
            .fillMaxWidth()
            .clickable { onItemsClick(SettingsElement.WalletConnect) },
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_walletconnect),
            contentDescription = stringResource(id = R.string.wallet_connect_title),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            tint = colorResource(id = R.color.all_colors_azure),
        )
        Column(
            modifier = Modifier.defaultMinSize(minHeight = 56.dp),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(id = R.string.wallet_connect_title),
                modifier = Modifier.padding(end = 20.dp, bottom = 4.dp),
                style = TangemTypography.headline3,
                color = colorResource(id = R.color.text_primary_1),
            )
            Text(
                text = stringResource(id = R.string.wallet_connect_subtitle),
                modifier = Modifier.padding(end = 20.dp, bottom = 4.dp),
                style = TangemTypography.body1,
                color = colorResource(id = R.color.text_secondary),
            )
        }
    }
}

@Composable
fun DetailsItem(item: SettingsElement, appCurrency: String, onItemsClick: () -> Unit) {
    Row(
        modifier = Modifier
            .height(56.dp)
            .fillMaxWidth()
            .clickable(onClick = onItemsClick),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(id = item.iconRes),
            contentDescription = stringResource(id = item.titleRes),
            modifier = Modifier.padding(start = 20.dp, end = 20.dp),
            tint = colorResource(id = R.color.icon_secondary),
        )
        Column(modifier = Modifier.padding(end = 20.dp)) {
            Text(
                text = stringResource(id = item.titleRes),
                modifier = Modifier,
                style = TangemTypography.subtitle1,
                color = colorResource(id = R.color.text_primary_1),
            )
            if (item == SettingsElement.AppCurrency) {
                Text(
                    text = appCurrency,
                    style = TangemTypography.body2,
                    color = colorResource(id = R.color.text_secondary),
                )
            }
        }
    }
}

@Composable
fun TangemSocialAccounts(links: List<SocialNetworkLink>, onSocialNetworkClick: (SocialNetworkLink) -> Unit) {
    LazyRow(
        modifier = Modifier.padding(start = 8.dp, end = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        items(links) {
            Icon(
                painter = painterResource(id = it.network.iconRes),
                contentDescription = "",
                modifier = Modifier
                    .padding(8.dp)
                    .clickable { onSocialNetworkClick(it) },
                tint = colorResource(id = R.color.icon_informative),
            )
        }
    }
}

@Composable
fun BoxScope.ShowSnackbarIfNeeded(snackbarErrorState: EventError) {
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    SnackbarHost(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(vertical = TangemTheme.dimens.spacing16)
            .fillMaxWidth(),
        hostState = snackbarHostState,
    )
    val errorTitle = when (snackbarErrorState) {
        is EventError.DemoReferralNotAvailable -> stringResource(id = R.string.alert_demo_feature_disabled)
        EventError.Empty -> ""
    }
    if (snackbarErrorState != EventError.Empty) {
        SideEffect {
            coroutineScope.launch {
                snackbarHostState.showSnackbar(errorTitle)
            }
            when (snackbarErrorState) {
                is EventError.DemoReferralNotAvailable -> snackbarErrorState.onErrorShow.invoke()
                else -> {
                    /*no-op*/
                }
            }
        }
    }
}

@Composable
@Preview
private fun Preview() {
    DetailsScreen(
        state = DetailsScreenState(
            elements = SettingsElement.values().toList(),
            tangemLinks = TangemSocialAccounts.accountsEn,
            tangemVersion = "Tangem 2.14.12 (343)",
            appCurrency = "Dollar",
            onItemsClick = {},
            onSocialNetworkClick = {},
        ),
        onBackClick = {},
    )
}
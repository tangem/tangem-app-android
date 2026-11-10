package com.tangem.features.onramp.redirect.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.request.ImageRequest
import com.tangem.core.ui.components.appbar.models.TopAppBarButtonUM
import com.tangem.core.ui.ds2.loader.TangemLoader
import com.tangem.core.ui.ds2.loader.TangemLoaderSize
import com.tangem.core.ui.ds2.shimmers.ProvideTangemShimmer
import com.tangem.core.ui.ds2.shimmers.TangemShimmer
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.onramp.impl.R
import com.tangem.features.onramp.redirect.entity.OnrampRedirectTopBarUM
import com.tangem.features.onramp.redirect.entity.OnrampRedirectUM
import com.tangem.utils.annotations.RemoveWithToggle

@Composable
@RemoveWithToggle(
    toggleName = "AND_19143_ONRAMP_DESIGN_SYSTEM_ENABLED",
    description = "Rename over the old file",
)
internal fun OnrampRedirectContentDs(state: OnrampRedirectUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors3.bg.primary)
            .imePadding(),
    ) {
        TangemTopNavigation(
            title = state.topBarConfig.title,
            contentAlign = TangemTopNavigation.ContentAlign.Start,
            blurBackground = false,
            fadeEnabled = false,
            onClose = state.topBarConfig.startButtonUM.onClicked,
        )
        Content(
            state = state,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        )
    }
}

@Composable
private fun Content(state: OnrampRedirectUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        LogoBlock(providerImageUrl = state.providerImageUrl)
        Text(
            modifier = Modifier.padding(top = 24.dp),
            text = state.title.resolveReference(),
            style = TangemTheme.typography3.heading.medium,
            color = TangemTheme.colors3.text.primary,
            textAlign = TextAlign.Center,
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = state.subtitle.resolveReference(),
            style = TangemTheme.typography3.caption.medium,
            color = TangemTheme.colors3.text.secondary,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun LogoBlock(providerImageUrl: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(LOGO_SIZE)
                .clip(CircleShape)
                .background(TangemTheme.colors3.bg.secondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                painter = painterResource(id = R.drawable.ic_tangem_24),
                tint = TangemTheme.colors3.icon.secondary,
                contentDescription = null,
            )
        }
        TangemLoader(
            color = TangemTheme.colors3.icon.secondary,
            size = TangemLoaderSize.X20,
        )
        SubcomposeAsyncImage(
            modifier = Modifier
                .size(LOGO_SIZE)
                .clip(CircleShape),
            model = ImageRequest.Builder(context = LocalContext.current)
                .data(providerImageUrl)
                .crossfade(enable = true)
                .allowHardware(false)
                .build(),
            loading = {
                ProvideTangemShimmer {
                    TangemShimmer(modifier = Modifier.size(LOGO_SIZE), radius = LOGO_SIZE / 2)
                }
            },
            contentDescription = null,
        )
    }
}

private val LOGO_SIZE = 72.dp

@Preview(widthDp = 360, heightDp = 640, showBackground = true)
@Preview(widthDp = 360, heightDp = 640, showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_OnrampRedirectContentDs() {
    TangemThemePreviewRedesign {
        OnrampRedirectContentDs(
            state = OnrampRedirectUM(
                topBarConfig = OnrampRedirectTopBarUM(
                    title = stringReference("Buy Tether"),
                    startButtonUM = TopAppBarButtonUM.Back(onBackClicked = {}),
                ),
                providerImageUrl = "",
                title = stringReference("Redirecting to Mercuryo..."),
                subtitle = stringReference(
                    "You will be able to complete your transaction on the third-party provider, Mercuryo",
                ),
            ),
        )
    }
}
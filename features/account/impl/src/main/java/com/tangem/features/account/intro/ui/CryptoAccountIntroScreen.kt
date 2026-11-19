package com.tangem.features.account.intro.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.res.R
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.ds.image.TangemIcon
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.fade.TangemFade
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_20
import com.tangem.features.account.intro.ui.state.CryptoAccountIntroUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val HERO_ASPECT_RATIO = 402f / 568f

/** Stub: the hero is a gradient until the design ships its illustration. */
@Composable
internal fun CryptoAccountIntroScreen(state: CryptoAccountIntroUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                endButton = { TangemButton.Close(onClick = state.onCloseClick) },
                fadeEnabled = false,
            )
        },
    ) { contentPadding ->
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Hero(title = state.title.resolveReference(), subtitle = state.subtitle.resolveReference())
            Points(points = state.points)
            SpacerH(96.dp + contentPadding.calculateBottomPadding())
        }

        Footer(
            isContinueLoading = state.isContinueLoading,
            onContinueClick = state.onContinueClick,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = contentPadding.calculateBottomPadding()),
        )
    }
}

@Composable
private fun Hero(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HERO_ASPECT_RATIO)
            .background(
                Brush.verticalGradient(
                    colors = listOf(TangemTheme.colors3.bg.accent.blue, TangemTheme.colors3.bg.primary),
                ),
            ),
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = TangemTheme.typography3.heading.medium, color = TangemTheme.colors3.text.primary)
            Text(
                text = subtitle,
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun Points(points: ImmutableList<CryptoAccountIntroUM.PointUM>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        points.forEach { point ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TangemIcon(tangemIconUM = point.icon, modifier = Modifier.size(20.dp))
                Text(text = pointText(point = point), style = TangemTheme.typography3.subheading.medium)
            }
        }
    }
}

@Composable
private fun pointText(point: CryptoAccountIntroUM.PointUM): AnnotatedString {
    val lead = point.lead.resolveReference()
    val description = point.description.resolveReference()
    val leadColor = TangemTheme.colors3.text.primary
    val descriptionColor = TangemTheme.colors3.text.secondary

    return remember(lead, description, leadColor, descriptionColor) {
        buildAnnotatedString {
            withStyle(SpanStyle(color = leadColor)) { append(lead) }
            append(' ')
            withStyle(SpanStyle(color = descriptionColor)) { append(description) }
        }
    }
}

@Composable
private fun Footer(isContinueLoading: Boolean, onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(position = TangemFade.Position.Bottom, modifier = Modifier.matchParentSize())

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            isLoading = isContinueLoading,
            text = resourceReference(R.string.common_continue),
            onClick = onContinueClick,
        )
    }
}

@Preview(showBackground = true, heightDp = 874)
@Preview(showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_CryptoAccountIntroScreen() {
    TangemThemePreviewRedesign {
        CryptoAccountIntroScreen(
            state = CryptoAccountIntroUM(
                title = resourceReference(R.string.common_crypto_account),
                subtitle = stringReference("Organize your crypto into separate accounts within one wallet"),
                points = persistentListOf(
                    CryptoAccountIntroUM.PointUM(
                        id = "addresses",
                        icon = TangemIconUM.Icon(imageVector = Icons.ic_shield_checkmark_20),
                        lead = stringReference("New addresses."),
                        description = stringReference("Get a fresh set for savings, trading, or payments."),
                    ),
                ),
                onContinueClick = {},
                onCloseClick = {},
            ),
        )
    }
}
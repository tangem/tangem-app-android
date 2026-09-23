package com.tangem.features.jointaccount.creation.promo.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.datasource.CollectionPreviewParameterProvider
import androidx.compose.ui.unit.dp
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
import com.tangem.core.ui.res.generated.icons.ic_user_plus_20
import com.tangem.core.ui.res.generated.icons.ic_users_20
import com.tangem.features.jointaccount.impl.R
import com.tangem.features.jointaccount.creation.promo.ui.state.JointAccountPromoUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val HERO_ASPECT_RATIO = 402f / 568f

@Composable
internal fun JointAccountPromoScreen(state: JointAccountPromoUM, modifier: Modifier = Modifier) {
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
        Content(state = state, contentPadding = contentPadding)
    }
}

@Composable
private fun BoxScope.Content(state: JointAccountPromoUM, contentPadding: PaddingValues) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Hero(state = state)
        Points(points = state.points)
        SpacerH(96.dp + contentPadding.calculateBottomPadding())
    }

    Footer(
        modifier = Modifier
            .align(Alignment.BottomCenter)
            .padding(bottom = contentPadding.calculateBottomPadding()),
        onContinueClick = state.onContinueClick,
    )
}

@Composable
private fun Hero(state: JointAccountPromoUM, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(HERO_ASPECT_RATIO),
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_joint_promo),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize(),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(horizontal = 24.dp)
                .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = state.title.resolveReference(),
                style = TangemTheme.typography3.heading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = state.subtitle.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
    }
}

@Composable
private fun Points(points: ImmutableList<JointAccountPromoUM.PointUM>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        points.forEach { point ->
            Point(point = point)
        }
    }
}

@Composable
private fun Point(point: JointAccountPromoUM.PointUM, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        TangemIcon(
            tangemIconUM = point.icon,
            modifier = Modifier.size(20.dp),
        )

        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TangemTheme.colors3.text.primary)) {
                    append(point.lead.resolveReference())
                }
                append(' ')
                withStyle(SpanStyle(color = TangemTheme.colors3.text.secondary)) {
                    append(point.description.resolveReference())
                }
            },
            style = TangemTheme.typography3.subheading.medium,
        )
    }
}

@Composable
private fun Footer(onContinueClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth()) {
        TangemFade(
            position = TangemFade.Position.Bottom,
            modifier = Modifier.matchParentSize(),
        )

        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            variant = TangemButton.Variant.Primary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_continue),
            onClick = onContinueClick,
        )
    }
}

@Preview(showBackground = true, heightDp = 874)
@Preview(showBackground = true, heightDp = 874, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Preview_JointAccountPromoScreen(
    @PreviewParameter(JointAccountPromoStateProvider::class) state: JointAccountPromoUM,
) {
    TangemThemePreviewRedesign {
        JointAccountPromoScreen(state = state)
    }
}

private class JointAccountPromoStateProvider : CollectionPreviewParameterProvider<JointAccountPromoUM>(
    collection = listOf(
        JointAccountPromoUM(
            title = resourceReference(R.string.common_joint_account),
            subtitle = resourceReference(R.string.joint_account_onboarding_subtitle),
            points = persistentListOf(
                JointAccountPromoUM.PointUM(
                    id = "members",
                    icon = TangemIconUM.Icon(imageVector = Icons.ic_user_plus_20),
                    lead = stringReference(value = "Up to 5 members."),
                    description = stringReference(value = "Each joins with their own wallet."),
                ),
                JointAccountPromoUM.PointUM(
                    id = "shared",
                    icon = TangemIconUM.Icon(imageVector = Icons.ic_users_20),
                    lead = stringReference(value = "Shared account."),
                    description = stringReference(value = "Use it with your friends, family, or partners."),
                ),
                JointAccountPromoUM.PointUM(
                    id = "approvals",
                    icon = TangemIconUM.Icon(imageVector = Icons.ic_shield_checkmark_20),
                    lead = stringReference(value = "Required approvals."),
                    description = stringReference(value = "Choose how many members must approve each transaction."),
                ),
            ),
            onContinueClick = {},
            onCloseClick = {},
        ),
    ),
)
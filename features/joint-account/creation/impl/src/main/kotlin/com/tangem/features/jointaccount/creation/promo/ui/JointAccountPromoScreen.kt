package com.tangem.features.jointaccount.creation.promo.ui

import android.content.res.Configuration
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
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
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24
import com.tangem.features.jointaccount.creation.impl.R
import com.tangem.features.jointaccount.creation.promo.ui.state.JointAccountPromoUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

private const val HERO_ASPECT_RATIO = 402f / 568f
private const val HERO_TINT_START = 0.5f
private const val HERO_TINT_MIDDLE = 0.75f
private const val HERO_TINT_END = 0.9f
private const val HERO_TINT_MIDDLE_ALPHA = 0.5f

@Composable
internal fun JointAccountPromoScreen(state: JointAccountPromoUM, modifier: Modifier = Modifier) {
    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(endButton = { TangemButton.Close(onClick = state.onCloseClick) })
        },
    ) { contentPadding ->
        Content(state = state, contentPadding = contentPadding)
    }
}

@Composable
private fun BoxScope.Content(state: JointAccountPromoUM, contentPadding: PaddingValues) {
    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
        Hero(state = state)
        Benefits(benefits = state.benefits)
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

        val background = TangemTheme.colors3.bg.primary
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to background.copy(alpha = 0f),
                            HERO_TINT_START to background.copy(alpha = 0f),
                            HERO_TINT_MIDDLE to background.copy(alpha = HERO_TINT_MIDDLE_ALPHA),
                            HERO_TINT_END to background,
                            1f to background,
                        ),
                    ),
                ),
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
private fun Benefits(benefits: ImmutableList<JointAccountPromoUM.BenefitUM>, modifier: Modifier = Modifier) {
    if (benefits.isEmpty()) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        benefits.chunked(size = 2).forEach { row ->
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { benefit ->
                    BenefitCard(
                        benefit = benefit,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                    )
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun BenefitCard(benefit: JointAccountPromoUM.BenefitUM, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(TangemTheme.colors3.bg.secondary)
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        TangemIcon(
            tangemIconUM = benefit.icon,
            modifier = Modifier.size(24.dp),
        )

        Column(
            modifier = Modifier.padding(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = benefit.title.resolveReference(),
                style = TangemTheme.typography3.subheading.medium,
                color = TangemTheme.colors3.text.primary,
            )
            Text(
                text = benefit.subtitle.resolveReference(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.secondary,
            )
        }
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
            benefits = persistentListOf(
                JointAccountPromoUM.BenefitUM(
                    id = "members",
                    icon = TangemIconUM.Icon(imageVector = Icons.ic_lightning_24),
                    title = stringReference(value = "Up to 5 members"),
                    subtitle = stringReference(value = "Everyone shares one balance and history"),
                ),
                JointAccountPromoUM.BenefitUM(
                    id = "control",
                    icon = TangemIconUM.Icon(imageVector = Icons.ic_shield_checkmark_24),
                    title = stringReference(value = "No single point of control"),
                    subtitle = stringReference(value = "Funds move only with enough signatures"),
                ),
            ),
            onContinueClick = {},
            onCloseClick = {},
        ),
    ),
)
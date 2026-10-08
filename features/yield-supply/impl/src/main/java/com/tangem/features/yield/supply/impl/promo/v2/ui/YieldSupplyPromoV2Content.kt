package com.tangem.features.yield.supply.impl.promo.v2.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SpacerH12
import com.tangem.core.ui.components.SpacerH24
import com.tangem.core.ui.components.SpacerWMax
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_refresh_20
import com.tangem.core.ui.res.generated.icons.ic_lightning_20
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_20
import com.tangem.core.ui.test.YieldSupplyTestTags
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.promo.model.YieldSupplyPromoClickIntents
import com.tangem.features.yield.supply.impl.promo.v2.entity.YieldSupplyPromoV2UM

@Composable
internal fun YieldSupplyPromoV2Content(
    state: YieldSupplyPromoV2UM,
    clickIntents: YieldSupplyPromoClickIntents,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(TangemTheme.colors.background.tertiary)
            .imePadding()
            .systemBarsPadding(),
    ) {
        TopBar(onBackClick = clickIntents::onBackClick, onHowItWorksClick = clickIntents::onHowItWorksClick)
        Content(state = state, clickIntents = clickIntents)
        Footer(state = state, clickIntents = clickIntents)
    }
}

@Composable
private fun ColumnScope.Content(state: YieldSupplyPromoV2UM, clickIntents: YieldSupplyPromoClickIntents) {
    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        HeaderArt()
        Column(modifier = Modifier.padding(horizontal = 24.dp)) {
            RatePills(state = state, clickIntents = clickIntents)
            SpacerH12()
            Title(title = state.title)
            SpacerH24()
            PromoPoints(tokenSymbol = state.tokenSymbol)
        }
        SpacerH24()
    }
}

@Composable
private fun HeaderArt() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, bottom = 40.dp)
            .height(148.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.img_yield_promo_graph_104),
            contentDescription = null,
            tint = Color.Unspecified,
            modifier = Modifier
                .width(104.dp)
                .height(93.dp),
        )
    }
}

/**
 * The design's heading step is 28/34, which the type scale of this screen's theme does not carry — its nearest `h2`
 * is 24/32. Sized here until the screen moves to the redesigned scale, where the step exists.
 */
@Composable
private fun Title(title: TextReference) {
    Text(
        text = title.resolveReference(),
        style = TangemTheme.typography.h2.copy(
            fontSize = 28.sp,
            lineHeight = 34.sp,
            fontWeight = FontWeight.SemiBold,
        ),
        color = TangemTheme.colors.text.primary1,
    )
}

/** The two pills open two different sheets: the plain Aave rate and the incentivised one. */
@Composable
private fun RatePills(state: YieldSupplyPromoV2UM, clickIntents: YieldSupplyPromoClickIntents) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Label(
            state = LabelUM(
                text = state.rate,
                style = LabelStyle.REGULAR,
                icon = R.drawable.ic_information_24,
                onClick = clickIntents::onVariableRateInfoClick,
                onIconClick = clickIntents::onVariableRateInfoClick,
            ),
        )
        state.incentiveChip?.let { chip ->
            Label(
                state = LabelUM(
                    text = chip,
                    style = LabelStyle.ACCENT,
                    icon = R.drawable.ic_information_24,
                    onClick = clickIntents::onIncentiveRateInfoClick,
                    onIconClick = clickIntents::onIncentiveRateInfoClick,
                ),
            )
        }
    }
}

@Composable
private fun PromoPoints(tokenSymbol: String) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        PromoPoint(
            icon = Icons.ic_lightning_20,
            title = resourceReference(R.string.yield_module_promo_screen_cash_out_title),
            subtitle = resourceReference(R.string.yield_module_promo_screen_cash_out_subtitle),
        )
        PromoPoint(
            icon = Icons.ic_arrow_refresh_20,
            title = resourceReference(R.string.yield_module_promo_screen_auto_balance_title),
            subtitle = resourceReference(
                id = R.string.yield_module_promo_screen_auto_balance_subtitle_v2,
                formatArgs = wrappedList(tokenSymbol),
            ),
        )
        PromoPoint(
            icon = Icons.ic_shield_checkmark_20,
            title = resourceReference(R.string.yield_module_promo_screen_self_custodial_title),
            subtitle = resourceReference(R.string.yield_module_promo_screen_self_custodial_subtitle),
        )
    }
}

/** Title and description run on as one paragraph, told apart by colour alone, so the title leads the sentence. */
@Composable
private fun PromoPoint(icon: ImageVector, title: TextReference, subtitle: TextReference) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(color = TangemTheme.colors.text.primary1)) {
                    append(title.resolveReference())
                }
                append(" ")
                withStyle(SpanStyle(color = TangemTheme.colors.text.secondary)) {
                    append(subtitle.resolveReference())
                }
            },
            style = TangemTheme.typography.subtitle2,
        )
    }
}

@Composable
private fun Footer(state: YieldSupplyPromoV2UM, clickIntents: YieldSupplyPromoClickIntents) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        TermsDisclaimer(
            tosLink = state.tosLink,
            policyLink = state.policyLink,
            onClick = clickIntents::onUrlClick,
        )
        PrimaryButton(
            text = stringResourceSafe(R.string.common_continue),
            onClick = clickIntents::onStartEarningClick,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(YieldSupplyTestTags.PROMO_CONTINUE_BUTTON),
        )
    }
}

@Suppress("UnnecessaryEventHandlerParameter")
@Composable
private fun TermsDisclaimer(tosLink: String, policyLink: String, onClick: (String) -> Unit) {
    val tosTitle = stringResourceSafe(R.string.common_terms_of_use)
    val policyTitle = stringResourceSafe(R.string.common_privacy_policy)
    val fullString = stringResourceSafe(
        id = R.string.yield_module_promo_screen_terms_disclaimer,
        tosTitle,
        policyTitle,
    )
    val tosIndex = fullString.indexOf(tosTitle)
    val policyIndex = fullString.indexOf(policyTitle)
    val linkStyle = SpanStyle(color = TangemTheme.colors.text.primary1, fontWeight = FontWeight.SemiBold)

    Text(
        text = buildAnnotatedString {
            append(fullString.substring(0, tosIndex))
            withLink(
                LinkAnnotation.Clickable(
                    tag = TOS_TAG,
                    linkInteractionListener = { onClick(tosLink) },
                ),
            ) {
                withStyle(linkStyle) { append(fullString.substring(tosIndex, tosIndex + tosTitle.length)) }
            }
            append(fullString.substring(tosIndex + tosTitle.length, policyIndex))
            withLink(
                LinkAnnotation.Clickable(
                    tag = POLICY_TAG,
                    linkInteractionListener = { onClick(policyLink) },
                ),
            ) {
                withStyle(linkStyle) { append(fullString.substring(policyIndex, policyIndex + policyTitle.length)) }
            }
        },
        textAlign = TextAlign.Center,
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.secondary,
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun TopBar(onBackClick: () -> Unit, onHowItWorksClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_back_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
            modifier = Modifier
                .clip(CircleShape)
                .background(TangemTheme.colors.button.secondary)
                .clickable(onClick = onBackClick)
                .size(44.dp)
                .padding(12.dp),
        )
        SpacerWMax()
        Text(
            text = stringResourceSafe(R.string.yield_module_promo_screen_how_it_works_button_title),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier
                .clip(CircleShape)
                .background(TangemTheme.colors.button.secondary)
                .clickable(onClick = onHowItWorksClick)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

private const val TOS_TAG = "TOS_TAG"
private const val POLICY_TAG = "POLICY_TAG"

@Preview(showBackground = true, widthDp = 360, heightDp = 780)
@Preview(showBackground = true, widthDp = 360, heightDp = 780, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun YieldSupplyPromoV2ContentPreview() {
    TangemThemePreview {
        YieldSupplyPromoV2Content(
            state = YieldSupplyPromoV2UM(
                title = stringReference("Enable 5.5% APY\non your balance"),
                rate = stringReference("Variable Interest Rate"),
                incentiveChip = stringReference("Incentivised 1.4%"),
                tokenSymbol = "USDT",
                tosLink = "",
                policyLink = "",
            ),
            clickIntents = object : YieldSupplyPromoClickIntents {
                override fun onBackClick() = Unit
                override fun onVariableRateInfoClick() = Unit
                override fun onIncentiveRateInfoClick() = Unit
                override fun onHowItWorksClick() = Unit
                override fun onStartEarningClick() = Unit
                override fun onUrlClick(url: String) = Unit
            },
        )
    }
}
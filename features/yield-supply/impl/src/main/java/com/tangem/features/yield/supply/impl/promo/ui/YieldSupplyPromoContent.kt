package com.tangem.features.yield.supply.impl.promo.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.*
import com.tangem.core.ui.components.label.Label
import com.tangem.core.ui.components.label.entity.LabelStyle
import com.tangem.core.ui.components.label.entity.LabelUM
import com.tangem.core.ui.extensions.*
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.yield.supply.impl.R
import com.tangem.features.yield.supply.impl.promo.entity.YieldSupplyPromoUM
import com.tangem.features.yield.supply.impl.promo.model.YieldSupplyPromoClickIntents
import com.tangem.utils.StringsSigns

@Composable
internal fun YieldSupplyPromoContent(
    yieldSupplyPromoUM: YieldSupplyPromoUM,
    clickIntents: YieldSupplyPromoClickIntents,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxSize()
            .imePadding()
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        YieldStatusAppBar(
            onBackClick = clickIntents::onBackClick,
            onHowItWorksClick = clickIntents::onHowItWorksClick,
        )
        Content(yieldSupplyPromoUM = yieldSupplyPromoUM, clickIntents = clickIntents)
        YieldSupplyTosText(
            tosLink = yieldSupplyPromoUM.tosLink,
            policyLink = yieldSupplyPromoUM.policyLink,
            onClick = clickIntents::onUrlClick,
        )
        PrimaryButton(
            text = stringResourceSafe(R.string.common_continue),
            onClick = clickIntents::onStartEarningClick,
            modifier = Modifier
                .padding(
                    start = 16.dp,
                    end = 16.dp,
                    bottom = 8.dp,
                )
                .fillMaxWidth(),
        )
    }
}

@Suppress("MagicNumber", "LongMethod")
@Composable
private fun ColumnScope.Content(yieldSupplyPromoUM: YieldSupplyPromoUM, clickIntents: YieldSupplyPromoClickIntents) {
    Box(modifier = Modifier.weight(1f)) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 20.dp),
            ) {
                Icon(
                    imageVector = ImageVector.vectorResource(R.drawable.ic_analytics_up_24),
                    tint = TangemTheme.colors.icon.accent,
                    contentDescription = null,
                    modifier = Modifier
                        .background(TangemTheme.colors.icon.accent.copy(0.1f), CircleShape)
                        .padding(12.dp)
                        .size(32.dp),
                )
                SpacerH(20.dp)
                if (yieldSupplyPromoUM.isBoostAvailable &&
                    yieldSupplyPromoUM.baseApy != null &&
                    yieldSupplyPromoUM.boostedApy != null
                ) {
                    BoostPromoTitle(
                        baseApy = yieldSupplyPromoUM.baseApy,
                        boostedApy = yieldSupplyPromoUM.boostedApy,
                    )
                } else {
                    Text(
                        text = yieldSupplyPromoUM.title.resolveReference(),
                        style = TangemTheme.typography.h2,
                        textAlign = TextAlign.Center,
                        color = TangemTheme.colors.text.primary1,
                    )
                }
                SpacerH8()
                Label(
                    state = LabelUM(
                        text = yieldSupplyPromoUM.subtitle,
                        style = LabelStyle.REGULAR,
                        icon = R.drawable.ic_information_24,
                        onClick = clickIntents::onApyInfoClick,
                        onIconClick = clickIntents::onApyInfoClick,
                    ),
                )
                SpacerH32()
                PromoItems(yieldSupplyPromoUM.tokenSymbol)
            }
            if (yieldSupplyPromoUM.isBoostAvailable &&
                yieldSupplyPromoUM.baseApy != null &&
                yieldSupplyPromoUM.boostedApy != null
            ) {
                SpacerH(20.dp)
                PromoBoostCard(
                    baseApy = yieldSupplyPromoUM.baseApy,
                    boostedApy = yieldSupplyPromoUM.boostedApy,
                    onLearnMoreClick = { clickIntents.onUrlClick(yieldSupplyPromoUM.boostTermsLink) },
                )
            }
            SpacerH32()
        }
        Fade(
            backgroundColor = TangemTheme.colors.background.tertiary,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun YieldStatusAppBar(onBackClick: () -> Unit, onHowItWorksClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_back_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
            modifier = Modifier.clickable(
                onClick = onBackClick,
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = false),
            ),
        )
        SpacerWMax()
        Text(
            text = stringResourceSafe(R.string.yield_module_promo_screen_how_it_works_button_title),
            style = TangemTheme.typography.body1,
            color = TangemTheme.colors.text.primary1,
            modifier = Modifier.clickable(onClick = onHowItWorksClick),
        )
    }
}

@Composable
private fun PromoItems(tokenSymbol: String) {
    PromoItem(
        icon = R.drawable.ic_flash_new_24,
        title = resourceReference(R.string.yield_module_promo_screen_cash_out_title),
        subtitle = resourceReference(R.string.yield_module_promo_screen_cash_out_subtitle),
    )
    SpacerH24()
    PromoItem(
        icon = R.drawable.ic_repeat_24,
        title = resourceReference(R.string.yield_module_promo_screen_auto_balance_title),
        subtitle = resourceReference(
            R.string.yield_module_promo_screen_auto_balance_subtitle_v2,
            wrappedList(tokenSymbol),
        ),
    )
    SpacerH24()
    PromoItem(
        icon = R.drawable.ic_security_check_24,
        title = resourceReference(R.string.yield_module_promo_screen_self_custodial_title),
        subtitle = resourceReference(R.string.yield_module_promo_screen_self_custodial_subtitle),
    )
}

@Suppress("MagicNumber")
@Composable
private fun BoostPromoTitle(baseApy: String, boostedApy: String) {
    val accent = TangemTheme.colors.text.accent
    val primary = TangemTheme.colors.text.primary1
    // Pass `%1$s` back as the argument so the placeholder survives formatting (`%%` → `%`).
    val raw = stringResourceSafe(R.string.yield_module_promo_screen_title_v2, "%1\$s")
    val (head, rest) = raw.split("%1\$s", limit = 2)
    // The template leaves a stray `%` right after the value (after a space in RU/UK), but the APY
    // strings already carry their own `%` — drop that duplicate.
    val tail = rest.trimStart().removePrefix("%")
    val annotated = buildAnnotatedString {
        append(head)
        withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.LineThrough)) {
            append(baseApy)
        }
        // Arrow glyph sits lower than digits in most fonts; lift it onto the cap-height baseline.
        withStyle(SpanStyle(color = accent, baselineShift = BaselineShift(0.1f))) {
            append(" → ")
        }
        withStyle(SpanStyle(color = accent)) {
            append(boostedApy)
        }
        append(tail)
    }
    Text(
        text = annotated,
        style = TangemTheme.typography.h2,
        textAlign = TextAlign.Center,
        color = primary,
    )
}

@Composable
private fun PromoBoostCard(baseApy: String, boostedApy: String, onLearnMoreClick: () -> Unit) {
    val accent = TangemTheme.colors.text.accent
    val primary = TangemTheme.colors.text.primary1
    val tertiary = TangemTheme.colors.text.tertiary
    val titleAnnotated = buildAnnotatedString {
        withStyle(SpanStyle(color = primary)) {
            append(stringResourceSafe(R.string.common_yield_mode))
            append(" · ")
        }
        withStyle(SpanStyle(color = accent)) {
            append("APY ")
        }
        withStyle(SpanStyle(color = accent, textDecoration = TextDecoration.LineThrough)) {
            append(baseApy)
        }
        withStyle(SpanStyle(color = accent)) {
            append(" x3 → ")
            append(boostedApy)
        }
    }
    val learnMoreLabel = stringResourceSafe(R.string.yield_apy_boost_promo_terms_and_conditions)
    val eligibilityText = stringResourceSafe(R.string.yield_apy_boost_promo_eligibility_text)
    val subtitleAnnotated = buildAnnotatedString {
        append(eligibilityText)
        append("${StringsSigns.COMA_SIGN} ")
        withLink(
            link = LinkAnnotation.Clickable(
                tag = "YIELD_BOOST_LEARN_MORE",
                linkInteractionListener = { onLearnMoreClick() },
            ),
            block = {
                appendColored(text = learnMoreLabel, color = accent)
            },
        )
    }
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(TangemTheme.colors.background.primary)
            .padding(12.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_gift_promo_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
        )
        Column(
            modifier = Modifier.padding(start = 12.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(text = titleAnnotated, style = TangemTheme.typography.subtitle2)
            Text(
                text = subtitleAnnotated,
                style = TangemTheme.typography.caption2,
                color = tertiary,
            )
        }
    }
}

@Composable
private fun PromoItem(@DrawableRes icon: Int, title: TextReference, subtitle: TextReference) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(icon),
            contentDescription = null,
            tint = TangemTheme.colors.icon.accent,
        )
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = title.resolveReference(),
                style = TangemTheme.typography.subtitle1,
                color = TangemTheme.colors.text.primary1,
            )
            Text(
                text = subtitle.resolveReference(),
                style = TangemTheme.typography.body2,
                color = TangemTheme.colors.text.secondary,
            )
        }
    }
}

@Suppress("UnnecessaryEventHandlerParameter")
@Composable
private fun YieldSupplyTosText(
    tosLink: String,
    policyLink: String,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val tosTitle = stringResourceSafe(R.string.common_terms_of_use)
    val policyTitle = stringResourceSafe(R.string.common_privacy_policy)
    val fullString = stringResourceSafe(id = R.string.yield_module_promo_screen_terms_disclaimer, tosTitle, policyTitle)
    val tosIndex = fullString.indexOf(tosTitle)
    val policyIndex = fullString.indexOf(policyTitle)

    Text(
        text = buildAnnotatedString {
            append(fullString.substring(0, tosIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "TOS_TAG",
                    linkInteractionListener = { onClick(tosLink) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(tosIndex, tosIndex + tosTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
            append(fullString.substring(tosIndex + tosTitle.length, policyIndex))
            withLink(
                link = LinkAnnotation.Clickable(
                    tag = "POLICY_TAG",
                    linkInteractionListener = { onClick(policyLink) },
                ),
                block = {
                    appendColored(
                        text = fullString.substring(policyIndex, policyIndex + policyTitle.length),
                        color = TangemTheme.colors.text.accent,
                    )
                },
            )
        },
        textAlign = TextAlign.Center,
        style = TangemTheme.typography.caption2,
        color = TangemTheme.colors.text.tertiary,
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
    )
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 724)
@Preview(showBackground = true, widthDp = 360, heightDp = 724, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun YieldSupplyPromoContent_Preview() {
    TangemThemePreview {
        YieldSupplyPromoContent(
            yieldSupplyPromoUM = YieldSupplyPromoUM(
                tosLink = "https://tangem.com/terms-of-service/",
                policyLink = "https://tangem.com/privacy-policy/",
                boostTermsLink = "https://tangem.com/docs/en/yield-mode-terms.pdf",
                title = resourceReference(R.string.yield_module_promo_screen_title),
                tokenSymbol = "USDT",
                subtitle = resourceReference(R.string.yield_module_promo_screen_variable_rate_info, wrappedList("5.3")),
                isBoostAvailable = false,
            ),
            clickIntents = object : YieldSupplyPromoClickIntents {
                override fun onBackClick() {}
                override fun onApyInfoClick() {}
                override fun onHowItWorksClick() {}
                override fun onStartEarningClick() {}
                override fun onUrlClick(url: String) {}
            },
        )
    }
}

// endregion
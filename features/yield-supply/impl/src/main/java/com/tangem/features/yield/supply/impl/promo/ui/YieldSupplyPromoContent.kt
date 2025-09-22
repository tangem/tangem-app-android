package com.tangem.features.yield.supply.impl.promo.ui

import android.content.res.Configuration
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
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

@Suppress("MagicNumber")
@Composable
internal fun YieldSupplyPromoContent(
    yieldSupplyPromoUM: YieldSupplyPromoUM,
    clickIntents: YieldSupplyPromoClickIntents,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .background(color = TangemTheme.colors.background.tertiary)
            .fillMaxWidth()
            .imePadding()
            .systemBarsPadding()
            .padding(horizontal = 16.dp),
    ) {
        YieldStatusAppBar(
            onBackClick = clickIntents::onBackClick,
            onHowItWorksClick = clickIntents::onHowItWorksClick,
        )
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
            Text(
                text = yieldSupplyPromoUM.title.resolveReference(),
                style = TangemTheme.typography.h2,
                color = TangemTheme.colors.text.primary1,
            )
            SpacerH8()
            Label(
                state = LabelUM(
                    text = resourceReference(R.string.yield_module_promo_screen_variable_rate_info),
                    style = LabelStyle.REGULAR,
                    icon = R.drawable.ic_information_24,
                    onIconClick = clickIntents::onApyInfoClick,
                ),
            )
            SpacerH32()
            PromoItems()
        }
        SpacerHMax()
        YieldSupplyTosText(
            tosLink = yieldSupplyPromoUM.tosLink,
            policyLink = yieldSupplyPromoUM.policyLink,
            onClick = clickIntents::onUrlClick,
        )
        PrimaryButton(
            text = stringResourceSafe(R.string.yield_module_start_earning),
            onClick = clickIntents::onStartEarningClick,
            modifier = Modifier.fillMaxWidth(),
        )
        SpacerH8()
    }
}

@Composable
private fun YieldStatusAppBar(onBackClick: () -> Unit, onHowItWorksClick: () -> Unit) {
    Row(
        modifier = Modifier.padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = ImageVector.vectorResource(R.drawable.ic_back_24),
            contentDescription = null,
            tint = TangemTheme.colors.icon.primary1,
            modifier = Modifier.clickable(onClick = onBackClick),
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
private fun PromoItems() {
    PromoItem(
        icon = R.drawable.ic_flash_new_24,
        title = resourceReference(R.string.yield_module_promo_screen_cash_out_title),
        subtitle = resourceReference(R.string.yield_module_promo_screen_cash_out_subtitle),
    )
    SpacerH24()
    PromoItem(
        icon = R.drawable.ic_repeat_24,
        title = resourceReference(R.string.yield_module_promo_screen_auto_balance_title),
        subtitle = resourceReference(R.string.yield_module_promo_screen_auto_balance_subtitle),
    )
    SpacerH24()
    PromoItem(
        icon = R.drawable.ic_security_check_24,
        title = resourceReference(R.string.yield_module_promo_screen_self_custodial_title),
        subtitle = resourceReference(R.string.yield_module_promo_screen_self_custodial_subtitle),
    )
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

@Composable
private fun YieldSupplyTosText(tosLink: String, policyLink: String, onClick: (String) -> Unit) {
    val tosTitle = stringResourceSafe(R.string.common_terms_of_use)
    val policyTitle = stringResourceSafe(R.string.common_privacy_policy)
    val fullString = stringResourceSafe(id = R.string.yield_module_promo_screen_terms_disclaimer, tosTitle, policyTitle)
    val tosIndex = fullString.indexOf(tosTitle)
    val policyIndex = fullString.indexOf(policyTitle)

    Text(
        text = buildAnnotatedString {
            append(StringsSigns.POINT_SIGN)
            appendSpace()
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
        modifier = Modifier
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
                title = resourceReference(R.string.yield_module_promo_screen_title, wrappedList("5.3")),
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
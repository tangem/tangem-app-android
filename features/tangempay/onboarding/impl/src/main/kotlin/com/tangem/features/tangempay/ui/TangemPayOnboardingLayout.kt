package com.tangem.features.tangempay.ui

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.common.ui.onboarding.OnboardingBenefitCard
import com.tangem.common.ui.onboarding.OnboardingHeadline
import com.tangem.common.ui.onboarding.OnboardingHero
import com.tangem.core.ui.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.Close
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.ds2.footer.TangemFooter
import com.tangem.core.ui.ds2.scaffold.TangemTopBarScaffold
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_document_20
import com.tangem.core.ui.res.generated.icons.ic_heart_24
import com.tangem.core.ui.res.generated.icons.ic_lightning_24
import com.tangem.core.ui.res.generated.icons.ic_percent_backward_24
import com.tangem.core.ui.res.generated.icons.ic_shield_checkmark_24

private const val HERO_TINT_START_Y = 380f
private const val HERO_TINT_END_Y = 532f

@Composable
internal fun TangemPayOnboardingLayout(
    isGetCardLoading: Boolean,
    onGetCardClick: () -> Unit,
    onTermsClick: () -> Unit,
    modifier: Modifier = Modifier,
    getCardIcon: TangemIconUM? = null,
    onCloseClick: (() -> Unit)? = null,
    legalCaption: (@Composable () -> Unit)? = null,
) {
    val scrollState = rememberScrollState()
    var footerHeight by remember { mutableIntStateOf(0) }
    val isTopBarScrimShown by remember(scrollState) { derivedStateOf { scrollState.value > 0 } }

    TangemTopBarScaffold(
        modifier = modifier,
        containerColor = TangemTheme.colors3.bg.primary,
        topBar = {
            TangemTopNavigation(
                fadeEnabled = isTopBarScrimShown,
                endButtonsGroup = { TermsButton(onClick = onTermsClick) },
                endButton = onCloseClick?.let { onClick -> { TangemButton.Close(onClick = onClick) } },
            )
        },
        overlay = {
            TangemFooter(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size -> footerHeight = size.height },
                extraTop = legalCaption,
            ) {
                TangemButton(
                    modifier = Modifier.fillMaxWidth(),
                    variant = TangemButton.Variant.Primary,
                    size = TangemButton.Size.X12,
                    text = resourceReference(R.string.tangempay_onboarding_get_card_button_text),
                    iconEnd = getCardIcon,
                    isLoading = isGetCardLoading,
                    onClick = onGetCardClick,
                )
            }
        },
    ) { contentPadding ->
        OnboardingContent(
            scrollState = scrollState,
            contentPadding = contentPadding,
            trailingSpace = with(LocalDensity.current) { footerHeight.toDp() },
        )
    }
}

@Composable
private fun TermsButton(onClick: () -> Unit) {
    TangemButton(
        variant = TangemButton.Variant.Ghost,
        size = TangemButton.Size.X9,
        iconStart = TangemIconUM.Icon(imageVector = Icons.ic_document_20),
        contentDescription = stringResourceSafe(R.string.tangem_pay_terms_fees_limits),
        onClick = onClick,
    )
}

@Composable
private fun OnboardingContent(scrollState: ScrollState, contentPadding: PaddingValues, trailingSpace: Dp) {
    val layoutDirection = LocalLayoutDirection.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                start = contentPadding.calculateStartPadding(layoutDirection),
                end = contentPadding.calculateEndPadding(layoutDirection),
            ),
    ) {
        OnboardingHero(
            imageResId = R.drawable.ill_tangem_pay_onboarding_bg,
            tintStartY = HERO_TINT_START_Y,
            tintEndY = HERO_TINT_END_Y,
            foregroundImageResId = R.drawable.img_hot_wallet_onboarding,
        ) {
            OnboardingHeadline(
                title = resourceReference(R.string.tangempay_newonboard_title),
                subtitle = resourceReference(R.string.tangempay_newonboard_body),
            )
            OnboardingBenefits()
        }
        Spacer(modifier = Modifier.height(trailingSpace))
    }
}

@Composable
private fun OnboardingBenefits() {
    Column(
        modifier = Modifier.padding(all = 24.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BenefitsRow {
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_lightning_24,
                title = resourceReference(R.string.tangempay_newonboard_topleft_title),
                subtitle = resourceReference(R.string.tangempay_newonboard_topleft_body),
            )
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_heart_24,
                title = resourceReference(R.string.tangempay_newonboard_topright_title),
                subtitle = resourceReference(R.string.tangempay_newonboard_topright_body),
            )
        }
        BenefitsRow {
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_shield_checkmark_24,
                title = resourceReference(R.string.tangempay_newonboard_bottomleft_title),
                subtitle = resourceReference(R.string.tangempay_newonboard_bottomleft_body_android),
            )
            OnboardingBenefitCard(
                modifier = Modifier.weight(1f).fillMaxHeight(),
                icon = Icons.ic_percent_backward_24,
                title = resourceReference(R.string.tangempay_newonboard_bottomright_title),
                subtitle = resourceReference(R.string.tangempay_newonboard_bottomright_body),
            )
        }
    }
}

@Composable
private fun BenefitsRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.height(IntrinsicSize.Min),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}
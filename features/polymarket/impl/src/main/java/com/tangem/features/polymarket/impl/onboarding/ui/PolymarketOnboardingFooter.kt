package com.tangem.features.polymarket.impl.onboarding.ui

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.ui.onboarding.OnboardingFooter
import com.tangem.core.res.R
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_24
import com.tangem.features.polymarket.impl.common.PolymarketLegalLine
import com.tangem.features.polymarket.impl.onboarding.ui.state.PolymarketOnboardingUM

/**
 * Pinned footer of the entry gate: the legal line and the action button over a blurring scrim.
 *
 * @param state screen state — supplies the button's loader, its label and every click handler.
 * @param legalTemplateResId the legal sentence the two term links are spliced into; it differs per screen,
 *  because reconnecting takes no new consent.
 * @param contentPadding safe-area padding from the scaffold.
 * @param modifier applied to the footer root; the caller aligns it to the bottom and measures its height.
 */
@Composable
internal fun PolymarketOnboardingFooter(
    state: PolymarketOnboardingUM.Action,
    @StringRes legalTemplateResId: Int,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    OnboardingFooter(contentPadding = contentPadding, modifier = modifier) {
        PolymarketLegalLine(
            onPolymarketTermsClick = state.onPolymarketTermsClick,
            onTangemTermsClick = state.onTangemTermsClick,
            templateResId = legalTemplateResId,
        )
        TangemButton(
            modifier = Modifier.fillMaxWidth(),
            size = TangemButton.Size.X12,
            variant = TangemButton.Variant.Primary,
            isLoading = state.isInProgress,
            iconEnd = TangemIconUM.Icon(imageVector = Icons.ic_logo_tangem_24),
            text = state.actionButtonText,
            contentDescription = if (state.isInProgress) {
                stringResourceSafe(R.string.common_in_progress)
            } else {
                null
            },
            onClick = state.onActionClick,
        )
    }
}
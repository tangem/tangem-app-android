package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.ds.topbar.TangemTopBar
import com.tangem.core.ui.ds2.button.TangemButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_cross_20
import com.tangem.domain.models.pay.TangemPayCardFrozenState
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddToWalletStepItemUM
import com.tangem.features.tangempay.entity.TangemPayAddToWalletUM
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayAddToWalletScreenV2(
    state: TangemPayAddToWalletUM,
    cardDetailsState: TangemPayCardDetailsUM,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary)
            .systemBarsPadding(),
    ) {
        AddToWalletTopBar(onBackClick = state.onBackClick)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(scrollState)
                .padding(top = 8.dp, bottom = 12.dp),
        ) {
            TangemPayCard(
                state = cardDetailsState,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
            )
            DynamicSpacer(scrollState = scrollState)
            AddToWalletTitle()
            AddToWalletSteps(steps = state.steps)
        }
        AddToWalletBottomBar(state = state)
    }
}

@Composable
private fun AddToWalletTopBar(onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    TangemTopBar(
        modifier = modifier,
        endContent = {
            TangemButton(
                iconStart = TangemIconUM.Icon(imageVector = Icons.ic_cross_20),
                onClick = onBackClick,
                size = TangemButton.Size.X11,
                variant = TangemButton.Variant.Material,
            )
        },
    )
}

@Composable
private fun ColumnScope.DynamicSpacer(scrollState: ScrollState) {
    if (!scrollState.canScrollBackward && !scrollState.canScrollForward) {
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun AddToWalletTitle(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(vertical = 12.dp, horizontal = 16.dp)
            .fillMaxWidth(),
        text = stringResourceSafe(R.string.tangempay_card_details_open_wallet_title),
        style = TangemTheme.typography3.heading.medium,
        color = TangemTheme.colors3.text.primary,
    )
}

@Composable
private fun AddToWalletSteps(steps: ImmutableList<TangemPayAddToWalletStepItemUM>, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        steps.forEachIndexed { idx, step ->
            StepItem(
                modifier = Modifier.padding(
                    top = if (idx == 0) 12.dp else 0.dp,
                    bottom = if (idx < steps.lastIndex) 16.dp else 12.dp,
                    start = 16.dp,
                    end = 16.dp,
                ),
                stepNumber = step.count,
                title = step.text,
            )
        }
    }
}

@Composable
private fun StepItem(stepNumber: Int, title: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(color = TangemTheme.colors3.bg.inverse, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber.toString(),
                style = TangemTheme.typography3.caption.medium,
                color = TangemTheme.colors3.text.inverse.primary,
            )
        }

        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography3.subheading.medium,
            color = TangemTheme.colors3.text.primary,
        )
    }
}

@Composable
private fun AddToWalletBottomBar(state: TangemPayAddToWalletUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 12.dp, bottom = 8.dp),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_got_it),
            onClick = state.onBackClick,
        )

        if (state.showAddToWalletButton) {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 12.dp),
                text = resourceReference(R.string.tangempay_card_details_open_wallet_button),
                size = TangemButton.Size.X12,
                onClick = state.onClickOpenWallet,
            )
        }
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTangemPayAddToWalletScreen() {
    TangemThemePreviewRedesign {
        TangemPayAddToWalletScreenV2(
            state = TangemPayAddToWalletUM(
                steps = persistentListOf(
                    TangemPayAddToWalletStepItemUM(
                        count = 1,
                        text = resourceReference(R.string.tangempay_card_details_open_wallet_step_1),
                    ),
                    TangemPayAddToWalletStepItemUM(
                        count = 2,
                        text = resourceReference(R.string.tangempay_card_details_open_wallet_step_2),
                    ),
                    TangemPayAddToWalletStepItemUM(
                        count = 3,
                        text = resourceReference(R.string.tangempay_card_details_open_wallet_step_3),
                    ),
                    TangemPayAddToWalletStepItemUM(
                        count = 4,
                        text = resourceReference(R.string.tangempay_card_details_open_wallet_step_4),
                    ),
                    TangemPayAddToWalletStepItemUM(
                        count = 5,
                        text = resourceReference(R.string.tangempay_card_details_open_wallet_step_5),
                    ),
                ),
                showAddToWalletButton = true,
                onBackClick = {},
                onClickOpenWallet = {},
            ),
            cardDetailsState = TangemPayCardDetailsUM(
                number = "",
                numberShort = "*1245",
                expiry = "••/••",
                cvv = "•••",
                onCopy = { _, _ -> },
                onClick = {},
                buttonText = TextReference.Res(R.string.tangempay_card_details_hide_text),
                cardFrozenState = TangemPayCardFrozenState.Unfrozen,
                displayNameState = null,
            ),
        )
    }
}
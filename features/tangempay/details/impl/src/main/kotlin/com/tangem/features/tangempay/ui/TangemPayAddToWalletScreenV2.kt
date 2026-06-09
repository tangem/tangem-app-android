package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
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
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddToWalletStepItemUM
import com.tangem.features.tangempay.entity.TangemPayAddToWalletUM
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayAddToWalletScreenV2(state: TangemPayAddToWalletUM, modifier: Modifier = Modifier) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(color = TangemTheme.colors3.bg.primary)
            .systemBarsPadding(),
    ) {
        AddToWalletTopBar(onBackClick = state.onBackClick)
        AddToWalletContent(
            scrollState = scrollState,
            steps = state.steps,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )
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
private fun AddToWalletContent(
    scrollState: ScrollState,
    steps: ImmutableList<TangemPayAddToWalletStepItemUM>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(horizontal = TangemTheme.dimens2.x6)
            .padding(bottom = TangemTheme.dimens2.x3),
    ) {
        AddToWalletCardImage()
        DynamicSpacer(scrollState = scrollState)
        AddToWalletTitle()
        AddToWalletSteps(steps = steps)
    }
}

@Composable
private fun AddToWalletCardImage(modifier: Modifier = Modifier) {
    Image(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                end = TangemTheme.dimens2.x22,
                bottom = TangemTheme.dimens2.x25,
            ),
        painter = painterResource(R.drawable.img_tangem_pay_visa),
        contentDescription = null,
    )
}

@Composable
private fun AddToWalletTitle(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .padding(vertical = TangemTheme.dimens2.x3)
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
                    top = if (idx == 0) TangemTheme.dimens2.x3 else TangemTheme.dimens2.x0,
                    bottom = if (idx < steps.lastIndex) {
                        TangemTheme.dimens2.x4
                    } else {
                        TangemTheme.dimens2.x3
                    },
                ),
                stepNumber = step.count,
                title = step.text,
            )
        }
    }
}

@Composable
private fun AddToWalletBottomBar(state: TangemPayAddToWalletUM, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        TangemButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = TangemTheme.dimens2.x4)
                .padding(top = TangemTheme.dimens2.x3, bottom = TangemTheme.dimens2.x2),
            variant = TangemButton.Variant.Secondary,
            size = TangemButton.Size.X12,
            text = resourceReference(R.string.common_got_it),
            onClick = state.onBackClick,
        )

        if (state.showAddToWalletButton) {
            TangemButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = TangemTheme.dimens2.x4)
                    .padding(bottom = TangemTheme.dimens2.x3),
                text = resourceReference(R.string.tangempay_card_details_open_wallet_button),
                size = TangemButton.Size.X12,
                onClick = state.onClickOpenWallet,
            )
        }
    }
}

@Composable
private fun ColumnScope.DynamicSpacer(scrollState: ScrollState) {
    if (!scrollState.canScrollBackward && !scrollState.canScrollForward) {
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StepItem(stepNumber: Int, title: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(TangemTheme.dimens2.x3),
    ) {
        Box(
            modifier = Modifier
                .size(TangemTheme.dimens2.x4)
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
        )
    }
}
package com.tangem.features.tangempay.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.ui.components.PrimaryButton
import com.tangem.core.ui.components.SecondaryButton
import com.tangem.core.ui.components.appbar.AppBarWithBackButton
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resolveReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemColorPalette
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreview
import com.tangem.features.tangempay.components.cardDetails.PreviewTangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.components.cardDetails.TangemPayCardDetailsBlockComponent
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddToWalletStepItemUM
import com.tangem.features.tangempay.entity.TangemPayAddToWalletUM
import com.tangem.features.tangempay.entity.TangemPayCardDetailsUM
import kotlinx.collections.immutable.persistentListOf

@Composable
internal fun TangemPayAddToWalletScreen(
    state: TangemPayAddToWalletUM,
    cardDetailsBlockComponent: TangemPayCardDetailsBlockComponent,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val bottomBarHeight = with(LocalDensity.current) { WindowInsets.systemBars.getBottom(this).toDp() }
    val cardDetailsState by cardDetailsBlockComponent.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding(),
    ) {
        AppBarWithBackButton(
            modifier = Modifier.statusBarsPadding(),
            iconRes = R.drawable.ic_close_24,
            onBackClick = state.onBackClick,
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            state = listState,
            contentPadding = PaddingValues(bottom = TangemTheme.dimens.spacing16 + bottomBarHeight),
        ) {
            item(key = TangemPayCardDetailsUM::class.java) {
                TangemPayCardDetailsBlockItem(component = cardDetailsBlockComponent, state = cardDetailsState)
            }

            item(key = TangemPayAddToWalletUM::class.java) {
                Text(
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 24.dp)
                        .fillMaxWidth(),
                    text = stringResourceSafe(R.string.tangempay_card_details_open_wallet_title),
                    style = TangemTheme.typography.h2,
                    color = TangemTheme.colors.text.primary1,
                    textAlign = TextAlign.Center,
                )
            }
            itemsIndexed(
                items = state.steps,
                key = { _, step -> "${TangemPayAddToWalletStepItemUM::class.java}${step.count}" },
            ) { idx, step ->
                StepItem(
                    modifier = Modifier.padding(bottom = if (idx < state.steps.lastIndex) 16.dp else 0.dp),
                    stepNumber = step.count,
                    title = step.text,
                )
            }
        }

        SecondaryButton(
            modifier = Modifier
                .imePadding()
                .padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                .fillMaxWidth(),
            text = resourceReference(R.string.common_got_it).resolveReference(),
            onClick = state.onBackClick,

        )
        if (state.showAddToWalletButton) {
            PrimaryButton(
                modifier = Modifier
                    .imePadding()
                    .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                    .fillMaxWidth(),
                text = resourceReference(R.string.tangempay_card_details_open_wallet_button).resolveReference(),
                onClick = state.onClickOpenWallet,
            )
        }
    }
}

@Composable
private fun StepItem(stepNumber: Int, title: TextReference, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(color = Color.Transparent, shape = TangemTheme.shapes.roundedCorners8)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color = TangemColorPalette.Azure, shape = CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = stepNumber.toString(),
                style = TangemTheme.typography.subtitle2,
                color = TangemTheme.colors.text.primary2,
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = title.resolveReference(),
            style = TangemTheme.typography.subtitle1,
            color = TangemTheme.colors.text.primary1,
        )
    }
}

@Preview(showBackground = true)
@Preview(uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewTangemPayAddToWalletScreen() {
    TangemThemePreview {
        TangemPayAddToWalletScreen(
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
            cardDetailsBlockComponent = PreviewTangemPayCardDetailsBlockComponent(
                TangemPayCardDetailsUM(
                    number = "•••• •••• •••• 1245",
                    expiry = "••/••",
                    cvv = "•••",
                    onCopy = {},
                    onClick = {},
                    buttonText = TextReference.Res(R.string.tangempay_card_details_hide_text),
                ),
            ),
        )
    }
}
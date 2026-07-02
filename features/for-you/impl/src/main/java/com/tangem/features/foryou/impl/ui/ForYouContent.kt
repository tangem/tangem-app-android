package com.tangem.features.foryou.impl.ui

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.tangem.core.ui.components.SpacerH
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds.image.DeviceIconUM
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.LocalMainBottomSheetColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletListUM
import com.tangem.features.commonfeatures.api.choosetoken.model.WalletTabUM
import com.tangem.features.foryou.impl.entity.ForYouUM
import com.tangem.features.foryou.impl.ui.components.WalletTabsBlock
import com.tangem.features.foryou.impl.ui.preview.ForYouPortfolioReviewPreviewData
import com.tangem.features.promobanners.api.PromoBannersBlockComponent
import kotlinx.collections.immutable.persistentListOf
import kotlinx.coroutines.flow.distinctUntilChanged

@Composable
internal fun ForYouContent(
    forYouUM: ForYouUM,
    bottomSheetState: State<BottomSheetState>,
    promoBannersBlockComponent: PromoBannersBlockComponent,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(bottomSheetState, promoBannersBlockComponent) {
        snapshotFlow { bottomSheetState.value == BottomSheetState.EXPANDED }
            .distinctUntilChanged()
            .collect(promoBannersBlockComponent::setVisibleOnScreen)
    }
    val background = LocalMainBottomSheetColor.current

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = contentPadding.calculateTopPadding())
            .drawBehind { drawRect(background.value) },
    ) {
        WalletTabsBlock(walletList = forYouUM.walletListUM)

        SpacerH(12.dp)

        promoBannersBlockComponent.ContentWithPadding(
            modifier = Modifier,
            horizontalItemPadding = 16.dp,
        )

        ForYouPortfolioReview(
            portfolioReviewUM = forYouUM.portfolioReviewUM,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        SpacerH(48.dp)
    }
}

// region Preview
@Composable
@Preview(showBackground = true, widthDp = 360)
@Preview(showBackground = true, widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun ForYouContent_Preview(@PreviewParameter(ForYouContentPreviewProvider::class) params: ForYouUM) {
    TangemThemePreviewRedesign {
        ForYouContent(
            forYouUM = params,
            bottomSheetState = remember { mutableStateOf(BottomSheetState.EXPANDED) },
            promoBannersBlockComponent = object : PromoBannersBlockComponent {
                @Composable
                override fun ContentWithPadding(horizontalItemPadding: Dp, modifier: Modifier) {
                }

                override fun setVisibleOnScreen(isVisible: Boolean) {}
            },
            contentPadding = PaddingValues.Zero,
            modifier = Modifier.background(TangemTheme.colors3.bg.primary),
        )
    }
}

private class ForYouContentPreviewProvider : PreviewParameterProvider<ForYouUM> {
    override val values: Sequence<ForYouUM>
        get() = sequenceOf(
            ForYouUM(
                walletListUM = WalletListUM(
                    items = persistentListOf(
                        WalletTabUM(
                            text = stringReference("Wallet 1"),
                            count = stringReference("1"),
                            isSelected = true,
                            onClick = {},
                            deviceIcon = DeviceIconUM.Mobile,
                        ),
                    ),
                ),
                portfolioReviewUM = ForYouPortfolioReviewPreviewData.reviewContent,
            ),
        )
}
// endregion
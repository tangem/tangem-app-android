package com.tangem.features.feed.components.v2

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.ui.components.bottomsheets.state.BottomSheetState
import com.tangem.core.ui.ds2.topnavigation.TangemTopNavigation
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.nav.FeedRoute
import com.tangem.features.feed.nav.FeedScreenComponent
import com.tangem.features.foryou.ForYouComponent
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject

/**
 * [FeedRoute.ForYou] screen of the v2 feed: the existing For You content topped with the DS3
 * [TangemTopNavigation] instead of the legacy sheet-header title. The legacy callbacks are
 * satisfied with router pushes — no lambdas cross the route.
 */
@Stable
internal class FeedForYouComponent @AssistedInject constructor(
    @Assisted context: AppComponentContext,
    @Suppress("UnusedPrivateProperty") @Assisted params: FeedRoute.ForYou,
    forYouComponentFactory: ForYouComponent.Factory,
) : FeedScreenComponent, AppComponentContext by context {

    private val legacyComponent = forYouComponentFactory.create(
        context = context,
        params = ForYouComponent.Params(callbacks = routerCallbacks()),
    )

    @Composable
    override fun Header(bottomSheetState: State<BottomSheetState>, onExpandSheet: () -> Unit, modifier: Modifier) {
        TangemTopNavigation(
            title = resourceReference(R.string.for_you_title),
            contentAlign = TangemTopNavigation.ContentAlign.Center,
            // rendered inside the shtorka's fixed-height sheet header: no system insets and no
            // vertical padding, the host centers the bar vertically
            windowInsets = WindowInsets(0.dp),
            contentPadding = PaddingValues(horizontal = 16.dp),
            // the sheet header is an opaque capsule (the search bar draws no fade either), and the
            // haze blur re-renders every frame of the header animation — both stay off
            fadeEnabled = false,
            blurBackground = false,
            onBack = { router.pop() },
            modifier = modifier,
        )
    }

    @Composable
    override fun Content(
        bottomSheetState: State<BottomSheetState>,
        contentPadding: PaddingValues,
        onExpandSheet: () -> Unit,
        modifier: Modifier,
    ) {
        legacyComponent.Content(
            bottomSheetState = bottomSheetState,
            contentPadding = contentPadding,
            modifier = modifier,
        )
    }

    private fun routerCallbacks(): ForYouComponent.ForYouModelCallbacks {
        return object : ForYouComponent.ForYouModelCallbacks {

            override fun onTokenClick(userWalletId: UserWalletId, currency: CryptoCurrency) {
                router.push(
                    route = FeedRoute.TokenSummary(
                        token = FeedRoute.TokenSummary.Token.Portfolio(cryptoCurrency = currency),
                    ),
                )
            }

            override fun onAllEarnTokensClick() {
                router.push(route = FeedRoute.Earn())
            }
        }
    }

    @AssistedFactory
    interface Factory {
        fun create(context: AppComponentContext, params: FeedRoute.ForYou): FeedForYouComponent
    }
}
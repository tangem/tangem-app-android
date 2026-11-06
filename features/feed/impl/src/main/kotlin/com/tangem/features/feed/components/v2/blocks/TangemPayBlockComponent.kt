package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.decompose.navigation.pushZooming
import com.tangem.core.ui.ds2.ambient.TangemAmbientBackground
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.utils.zoomTransitionSource
import com.tangem.features.feed.components.v2.blocks.analytics.FeedBlocksAnalytics
import com.tangem.features.feed.components.v2.blocks.analytics.ProductCard
import com.tangem.features.feed.components.v2.blocks.model.TangemPayBlockModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedBlockShape
import com.tangem.features.feed.ui.v2.FeedBlockIconBadge
import com.tangem.features.feed.ui.v2.FeedPromoCard
import kotlinx.coroutines.flow.StateFlow

/** Tangem Pay pitch, offered while the user has no spending account — see [TangemPayBlockModel]. */
internal class TangemPayBlockComponent(
    context: AppComponentContext,
    private val blocksAnalytics: FeedBlocksAnalytics,
) : FeedBlockComponent(context = context, key = KEY) {

    private val model: TangemPayBlockModel = getOrCreateModel()

    override val size: FeedBlock.Size = FeedBlock.Size.Large

    override val productCard: ProductCard = ProductCard.TangemPay

    override val isVisible: StateFlow<Boolean> = model.isVisible

    @Composable
    override fun Content(modifier: Modifier) {
        FeedPromoCard(
            title = stringResourceSafe(R.string.feed_promo_tangem_pay_title),
            description = stringResourceSafe(R.string.feed_promo_tangem_pay_description),
            ambientVariant = TangemAmbientBackground.Variant.Violet,
            onClick = ::onClick,
            modifier = modifier.zoomTransitionSource(key = SHARED_TRANSITION_KEY, shape = FeedBlockShape),
            icon = { TangemPayIcon() },
        )
    }

    private fun onClick() {
        val mode = model.onboardingMode.value ?: return
        blocksAnalytics.onProductCardClicked(productCard)
        router.pushZooming(route = AppRoute.TangemPayOnboarding(mode = mode), sourceKey = SHARED_TRANSITION_KEY)
    }

    companion object {
        const val KEY = "tangem_pay"
        private const val SHARED_TRANSITION_KEY = "feed_tangem_pay_block"
    }
}

@Composable
private fun TangemPayIcon() {
    FeedBlockIconBadge(background = TangemTheme.colors3.bg.accent.violet) {
        Icon(
            painter = painterResource(id = R.drawable.ic_visa_logo),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticDark,
            modifier = Modifier.width(width = 22.dp),
        )
    }
}
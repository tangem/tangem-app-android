package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import com.arkivanov.decompose.router.slot.childSlot
import com.arkivanov.essenty.lifecycle.doOnResume
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.context.childByContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.decompose.ComposableBottomSheetComponent
import com.tangem.core.ui.ds2.ambient.TangemAmbientBackground
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.commonfeatures.api.portfolioselector.PortfolioSelectorComponent
import com.tangem.features.feed.components.v2.blocks.analytics.FeedBlocksAnalytics
import com.tangem.features.feed.components.v2.blocks.analytics.ProductCard
import com.tangem.features.feed.components.v2.blocks.model.ReferralBlockModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedBlockIconBadge
import com.tangem.features.feed.ui.v2.FeedPromoCard
import com.tangem.features.feed.ui.v2.FeedPromoCardSkeleton
import kotlinx.coroutines.flow.StateFlow

/**
 * Invitation into the referral programme, and the wallet chooser it opens when the programme is open to
 * more than one wallet — see [ReferralBlockModel] for which wallets those are.
 */
internal class ReferralBlockComponent(
    context: AppComponentContext,
    private val portfolioSelectorComponentFactory: PortfolioSelectorComponent.Factory,
    private val blocksAnalytics: FeedBlocksAnalytics,
) : FeedBlockComponent(context = context, key = KEY) {

    private val model: ReferralBlockModel = getOrCreateModel()

    override val size: FeedBlock.Size = FeedBlock.Size.Large

    override val productCard: ProductCard = ProductCard.ReferAFriend

    override val isVisible: StateFlow<Boolean> = model.isVisible

    private val bottomSheetSlot = childSlot(
        source = model.bottomSheetNavigation,
        serializer = null,
        handleBackButton = false,
    ) { _, componentContext -> walletSelectorChild(componentContext) }

    init {
        lifecycle.doOnResume { model.onResume() }
    }

    @Composable
    override fun Content(modifier: Modifier) {
        val isLoading by model.isLoading.collectAsStateWithLifecycle()

        if (isLoading) {
            FeedPromoCardSkeleton(
                ambientVariant = TangemAmbientBackground.Variant.Red,
                modifier = modifier,
                icon = { ReferralIcon() },
            )
        } else {
            FeedPromoCard(
                title = stringResourceSafe(R.string.feed_promo_referral_title),
                description = stringResourceSafe(R.string.feed_promo_referral_description, REFERRAL_AWARD),
                ambientVariant = TangemAmbientBackground.Variant.Red,
                onClick = ::onClick,
                modifier = modifier,
                icon = { ReferralIcon() },
            )
        }
    }

    @Composable
    override fun Overlay() {
        val bottomSheet by bottomSheetSlot.subscribeAsState()

        bottomSheet.child?.instance?.BottomSheet()
    }

    private fun onClick() {
        blocksAnalytics.onProductCardClicked(productCard)
        model.onClick()
    }

    private fun walletSelectorChild(componentContext: ComponentContext): ComposableBottomSheetComponent =
        portfolioSelectorComponentFactory.create(
            context = childByContext(componentContext),
            params = PortfolioSelectorComponent.Params(
                portfolioFetcher = model.portfolioFetcher,
                controller = model.portfolioSelectorController,
                bsCallback = model.portfolioSelectorCallback,
                settings = PortfolioSelectorComponent.Settings(isWalletSelectionOnly = true, isDs3Modal = true),
            ),
        )

    /** The block's only sheet: the chooser of the wallet the invitation is opened for. */
    data object WalletSelectorConfig

    companion object {
        const val KEY = "referral"
    }
}

/** Fixed in the copy by design — the programme's award is not read from the backend here. */
private const val REFERRAL_AWARD = "$5"

@Composable
private fun ReferralIcon() {
    FeedBlockIconBadge(background = TangemTheme.colors3.bg.accent.red) {
        Icon(
            painter = painterResource(id = R.drawable.ic_gift_24),
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticDark,
            modifier = Modifier.size(size = 24.dp),
        )
    }
}
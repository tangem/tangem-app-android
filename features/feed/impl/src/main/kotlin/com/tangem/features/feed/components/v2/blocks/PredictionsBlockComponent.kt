package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.components.account.PredictionAccountIcon
import com.tangem.core.ui.ds2.ambient.TangemAmbientBackground
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.features.feed.components.v2.blocks.model.PredictionsBlockModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedBlockIconBadge
import com.tangem.features.feed.ui.v2.FeedPromoCard
import kotlinx.coroutines.flow.StateFlow

/** Predictions pitch; whether it is offered at all is decided by [PredictionsBlockModel]. */
internal class PredictionsBlockComponent(
    context: AppComponentContext,
) : FeedBlockComponent(context = context, key = KEY) {

    private val model: PredictionsBlockModel = getOrCreateModel()

    override val size: FeedBlock.Size = FeedBlock.Size.Large

    override val isVisible: StateFlow<Boolean> = model.isVisible

    @Composable
    override fun Content(modifier: Modifier) {
        FeedPromoCard(
            title = stringResourceSafe(R.string.prediction_main_title),
            description = stringResourceSafe(R.string.prediction_account_subtitle),
            ambientVariant = TangemAmbientBackground.Variant.Violet,
            onClick = { router.push(AppRoute.Polymarket()) },
            modifier = modifier,
            icon = { PredictionsIcon() },
        )
    }

    companion object {
        const val KEY = "predictions"
    }
}

@Composable
private fun PredictionsIcon() {
    FeedBlockIconBadge(background = PredictionAccountIcon.color) {
        Icon(
            imageVector = PredictionAccountIcon.imageVector,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticDark,
            modifier = Modifier.size(size = 24.dp),
        )
    }
}
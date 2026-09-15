package com.tangem.features.feed.components.v2.blocks

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tangem.common.routing.AppRoute
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.features.feed.components.v2.blocks.model.PredictionsBlockModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedTitleCard
import kotlinx.coroutines.flow.StateFlow

/** Entry into Predictions. A title-only card until the feature contributes its own pitch. */
internal class PredictionsBlockComponent(
    context: AppComponentContext,
) : FeedBlockComponent(context = context, key = KEY) {

    private val model: PredictionsBlockModel = getOrCreateModel()

    override val size: FeedBlock.Size = FeedBlock.Size.Large

    override val isVisible: StateFlow<Boolean> = model.isVisible

    @Composable
    override fun Content(modifier: Modifier) {
        FeedTitleCard(
            title = stringResourceSafe(R.string.prediction_main_title),
            onClick = { router.push(AppRoute.Polymarket()) },
            modifier = modifier,
        )
    }

    companion object {
        const val KEY = "predictions"
    }
}
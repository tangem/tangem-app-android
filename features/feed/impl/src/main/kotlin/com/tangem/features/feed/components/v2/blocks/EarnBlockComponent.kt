package com.tangem.features.feed.components.v2.blocks

import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tangem.core.decompose.context.AppComponentContext
import com.tangem.core.decompose.model.getOrCreateModel
import com.tangem.core.ui.ds2.ambient.TangemAmbientBackground
import com.tangem.core.ui.extensions.stringResourceSafe
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_chart_line_vertical_24
import com.tangem.features.feed.components.v2.blocks.model.EarnBlockModel
import com.tangem.features.feed.impl.R
import com.tangem.features.feed.ui.v2.FeedBlockIconBadge
import com.tangem.features.feed.ui.v2.FeedPromoCard
import com.tangem.features.feed.ui.v2.state.EarnBlockUM
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Earn pitch. Always in the row; what it says and where it leads come from [EarnBlockModel]. */
internal class EarnBlockComponent(
    context: AppComponentContext,
    callbacks: FeedBlockModalCallbacks,
) : FeedBlockComponent(context = context, key = KEY) {

    private val model: EarnBlockModel = getOrCreateModel(params = callbacks)

    override val size: FeedBlock.Size = FeedBlock.Size.Large

    override val isVisible: StateFlow<Boolean> = MutableStateFlow(true)

    @Composable
    override fun Content(modifier: Modifier) {
        val state by model.state.collectAsStateWithLifecycle()

        // Every pitch opens the same way; only the line under it follows what the user already earns,
        // and it is left out entirely until a rate is known — it cannot be phrased without one.
        val description = when (val pitch = state.pitch) {
            is EarnBlockUM.Pitch.Growth -> stringResourceSafe(
                id = R.string.feed_promo_earn_active_description,
                pitch.apy,
                pitch.tokenSymbol,
            )
            is EarnBlockUM.Pitch.Onboarding -> stringResourceSafe(R.string.feed_promo_earn_description, pitch.apy)
            EarnBlockUM.Pitch.Plain -> null
        }

        FeedPromoCard(
            title = stringResourceSafe(R.string.feed_promo_earn_title),
            description = description,
            ambientVariant = TangemAmbientBackground.Variant.Blue,
            onClick = model::onClick,
            modifier = modifier,
            icon = { EarnIcon() },
        )
    }

    companion object {
        const val KEY = "earn"
    }
}

@Composable
private fun EarnIcon() {
    FeedBlockIconBadge(background = TangemTheme.colors3.bg.accent.blue) {
        Icon(
            imageVector = Icons.ic_chart_line_vertical_24,
            contentDescription = null,
            tint = TangemTheme.colors3.icon.staticDark,
            modifier = Modifier.size(size = 24.dp),
        )
    }
}
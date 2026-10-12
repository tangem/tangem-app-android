package com.tangem.features.collectibles.impl.packopening.ui.preview

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import com.tangem.core.ui.res.TangemThemePreviewRedesign
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.RewardCardContent
import com.tangem.features.collectibles.impl.packopening.ui.components.rewardcard.RewardCardTiming
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardProgress
import com.tangem.features.collectibles.impl.packopening.ui.state.RewardCardUM

/** Early in the grow and the turn. */
private const val PREVIEW_EARLY = 0.3f

/** Mid-turn, where a mirrored back face would show. */
private const val PREVIEW_BACK_FACING = 0.66f

@Composable
@Preview(showBackground = true, widthDp = 360, heightDp = 800)
@Preview(showBackground = true, widthDp = 360, heightDp = 800, uiMode = Configuration.UI_MODE_NIGHT_YES)
private fun RewardCard_Preview(@PreviewParameter(RewardCardPreviewProvider::class) params: RewardCardPreviewParams) {
    TangemThemePreviewRedesign {
        RewardCardContent(
            um = RewardCardPreviewUM.copy(accent = params.accent),
            timing = RewardCardTiming.Default,
            progress = params.progress,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

private data class RewardCardPreviewParams(
    val progress: RewardCardProgress,
    val accent: RewardCardUM.Accent = RewardCardUM.Accent.Green,
)

// No Android types here: Studio builds the provider before the layoutlib session exists
private class RewardCardPreviewProvider : PreviewParameterProvider<RewardCardPreviewParams> {

    override val values: Sequence<RewardCardPreviewParams> = sequenceOf(
        RewardCardPreviewParams(progress = RewardCardProgress.Settled),
        RewardCardPreviewParams(progress = RewardCardProgress.Settled, accent = RewardCardUM.Accent.Neutral),
        RewardCardPreviewParams(progress = RewardCardProgress.Settled, accent = RewardCardUM.Accent.Blue),
        RewardCardPreviewParams(progress = RewardCardProgress.Settled, accent = RewardCardUM.Accent.Violet),
        RewardCardPreviewParams(
            progress = RewardCardProgress(
                grow = PREVIEW_EARLY,
                settle = 0f,
                spin = PREVIEW_EARLY,
                appear = 0.4f,
            ),
        ),
        RewardCardPreviewParams(
            progress = RewardCardProgress(grow = PREVIEW_EARLY, settle = 0f, spin = PREVIEW_EARLY),
        ),
        RewardCardPreviewParams(
            progress = RewardCardProgress(grow = PREVIEW_BACK_FACING, settle = 0f, spin = 0.5f),
        ),
        RewardCardPreviewParams(progress = RewardCardProgress(grow = 1f, settle = 0f, spin = 1f)),
        RewardCardPreviewParams(progress = RewardCardProgress(grow = 1f, settle = 0.5f, spin = 1f)),
    )
}

private val RewardCardPreviewUM = RewardCardUM(
    rarity = "Uncommon pull",
    accent = RewardCardUM.Accent.Green,
    title = "2024 Series One",
    editionNumber = "#216",
    marketValue = "$62.00",
    sellBackValue = "$55.80",
)
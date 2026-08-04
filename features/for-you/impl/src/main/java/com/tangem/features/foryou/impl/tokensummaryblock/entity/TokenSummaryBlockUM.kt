package com.tangem.features.foryou.impl.tokensummaryblock.entity

import com.tangem.features.foryou.impl.components.state.AiInsightUM
import com.tangem.features.foryou.impl.tokensummary.entity.TokenSentimentUM

/**
 * UI model of the embeddable "Token summary" block.
 *
 * Reuses [TokenSentimentUM] and [AiInsightUM] from the full-screen token summary so the same UI can be shared.
 */
internal data class TokenSummaryBlockUM(
    val sentiment: TokenSentimentUM,
    val aiInsight: AiInsightUM,
    val onClick: () -> Unit,
)
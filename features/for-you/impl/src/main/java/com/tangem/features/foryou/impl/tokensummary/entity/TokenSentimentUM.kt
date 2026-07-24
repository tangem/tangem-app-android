package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList

@Immutable
internal sealed class TokenSentimentUM {

    abstract val indicators: ImmutableList<TokenIndicatorUM>

    data class Content(
        val sentiment: TextReference,
        @param:IntRange(from = -5, to = 5)
        val totalScore: Int,
        /** Symmetric max magnitude of the sentiment scale bar; the range is `-scaleMax..scaleMax`. */
        @param:IntRange(from = 1, to = 5)
        val scaleMax: Int,
        val lastUpdate: TextReference,
        override val indicators: ImmutableList<TokenIndicatorUM>,
    ) : TokenSentimentUM()

    data class Empty(
        val text: TextReference,
    ) : TokenSentimentUM() {
        override val indicators: ImmutableList<TokenIndicatorUM> = IndicatorType.entries
            .map { TokenIndicatorUM.NoData(indicatorType = it) }
            .toImmutableList()
    }

    data object Loading : TokenSentimentUM() {
        override val indicators: ImmutableList<TokenIndicatorUM> = IndicatorType.entries
            .map { TokenIndicatorUM.Loading(indicatorType = it) }
            .toImmutableList()
    }
}
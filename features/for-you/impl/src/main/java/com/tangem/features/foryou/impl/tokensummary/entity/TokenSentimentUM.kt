package com.tangem.features.foryou.impl.tokensummary.entity

import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
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

    /** No sentiment to show. The subtype carries the reason, which drives the message shown to the user. */
    sealed class Empty : TokenSentimentUM() {

        /** Explains to the user why the sentiment is missing. */
        abstract val message: TextReference

        /**
         * Indicators never arrived from the backend — nothing about the token could be loaded, not even the
         * indicator names, so the list is empty and only [message] is shown.
         */
        data object NoResponse : Empty() {
            override val message: TextReference = resourceReference(R.string.token_summary_can_not_load_token)
            override val indicators: ImmutableList<TokenIndicatorUM> = persistentListOf()
        }

        /**
         * Indicators arrived, but none of them carries a value — there is no outlook for this token. The rows keep
         * the names that did arrive.
         */
        data class NoOutlook(
            override val indicators: ImmutableList<TokenIndicatorUM>,
        ) : Empty() {
            override val message: TextReference = resourceReference(R.string.token_summary_outlook_is_not_available)
        }
    }

    data object Loading : TokenSentimentUM() {
        override val indicators: ImmutableList<TokenIndicatorUM> = IndicatorType.entries
            .map { TokenIndicatorUM.Loading(indicatorType = it) }
            .toImmutableList()
    }
}
package com.tangem.features.foryou.model

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.markets.CoinIndicators

/**
 * Periods selectable in the For You period picker, tying together the picker segment id, its display
 * title and the indicators [CoinIndicators.Reading.Timeframe] it stands for. Segment ids follow the
 * `TokenSummaryModel` convention ("0"/"1"/"2") so a selection is portable between the screens.
 */
enum class ForYouPeriod(
    val id: String,
    val title: TextReference,
    val timeframe: CoinIndicators.Reading.Timeframe,
) {
    Day(
        id = "0",
        title = resourceReference(R.string.common_day),
        timeframe = CoinIndicators.Reading.Timeframe.DAY,
    ),
    Week(
        id = "1",
        title = resourceReference(R.string.common_week),
        timeframe = CoinIndicators.Reading.Timeframe.WEEK,
    ),
    Month(
        id = "2",
        title = resourceReference(R.string.common_month),
        timeframe = CoinIndicators.Reading.Timeframe.MONTH,
    ),
    ;

    companion object {
        fun fromId(id: String?): ForYouPeriod = entries.firstOrNull { it.id == id } ?: Day
    }
}
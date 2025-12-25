package com.tangem.feature.tester.presentation.news.state

import androidx.annotation.StringRes
import com.tangem.feature.tester.impl.R
import kotlinx.collections.immutable.ImmutableSet

data class NewsUM(
    val onBackClick: () -> Unit,
    val buttons: ImmutableSet<ButtonUM>,
    val onButtonClick: (ButtonUM) -> Unit,
) {

    enum class ButtonUM(@StringRes val textResId: Int) {
        NEWS_DETAILS(R.string.news_details),
        NEWS_DETAILS_BOTTOM_SHEET(R.string.news_details_bottom_sheet),
    }
}
package com.tangem.common.ui.news

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.currency.icon.CurrencyIconState
import com.tangem.core.ui.extensions.TextReference

@Immutable
sealed interface ArticleTagUM {

    val title: TextReference

    data class Category(override val title: TextReference) : ArticleTagUM

    data class Token(
        override val title: TextReference,
        val iconState: CurrencyIconState,
    ) : ArticleTagUM
}
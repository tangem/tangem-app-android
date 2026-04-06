package com.tangem.features.feed.ui.market.detailed.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.pluralReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.wrappedList
import com.tangem.features.feed.impl.R

/**
 * "Listed on" block UI model
 *
[REDACTED_AUTHOR]
 */
@Immutable
internal sealed interface ListedOnUM {

    /** Title */
    val title: TextReference
        get() = resourceReference(id = R.string.markets_token_details_listed_on)

    /** Description */
    val description: TextReference

    @Immutable
    /** Empty state. No exchanges found */
    data object Empty : ListedOnUM {
        override val description = resourceReference(id = R.string.markets_token_details_empty_exchanges)
    }

    /**
     * Content with number of exchanges
     *
     * @property onClick lambda be invoked when button is clicked
     * @property amount  amount of exchanges
     */
    @Immutable
    data class Content(
        val onClick: () -> Unit,
        private val amount: Int,
    ) : ListedOnUM {
        override val description: TextReference = pluralReference(
            id = R.plurals.markets_token_details_amount_exchanges,
            count = amount,
            formatArgs = wrappedList(amount),
        )
    }
}
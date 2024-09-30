package com.tangem.features.markets.details.impl.ui.state

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.stringReference

/**
 * "Listed on" block UI model
 *
* [REDACTED_AUTHOR]
 */
internal sealed interface ListedOnUM {

    /** Description */
    val description: TextReference

    /** Empty state. No exchanges found */
    data object Empty : ListedOnUM {
// [REDACTED_TODO_COMMENT]
        override val description: TextReference = stringReference(value = "No exchanges found")
    }

    /**
     * Content with number of exchanges
     *
     * @property onClick lambda be invoked when button is clicked
     * @property amount  amount of exchanges
     */
    data class Content(
        val onClick: () -> Unit,
        private val amount: Int,
    ) : ListedOnUM {
// [REDACTED_TODO_COMMENT]
        override val description: TextReference = stringReference(value = "$amount exchanges")
    }
}

package com.tangem.features.tangempay.cashback.impl.ui.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

/**
 * State for the "Additional cashback" section on the Cashback screen.
 *
 * @property items one card per active additional/bonus promotion, ordered as returned by the BFF
 */
@Immutable
internal data class TangemPayAdditionalCashbackUM(
    val items: ImmutableList<Item>,
) {

    /**
     * @property name short promotion name shown above the description
     * @property description one-line promotion description
     * @property badge validity badge — permanent or time-limited
     */
    @Immutable
    data class Item(
        val id: String,
        val name: TextReference,
        val description: TextReference,
        val badge: Badge,
    )

    /** Validity badge shown at the top of an additional-cashback card. */
    @Immutable
    sealed interface Badge {

        /** No expiry — rendered as a neutral "Permanent" pill. */
        data object Permanent : Badge

        /** Time-limited — rendered as an info "Until <date>" pill with a clock icon. */
        data class Until(val text: TextReference) : Badge
    }
}
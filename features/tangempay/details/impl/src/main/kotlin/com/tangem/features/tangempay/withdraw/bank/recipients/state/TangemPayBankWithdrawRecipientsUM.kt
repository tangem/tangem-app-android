package com.tangem.features.tangempay.withdraw.bank.recipients.state

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.TextReference
import kotlinx.collections.immutable.ImmutableList

@Immutable
internal data class TangemPayBankWithdrawRecipientsUM(
    val search: TangemSearch.State,
    val content: ContentUM,
    val onBackClick: () -> Unit,
    val onCloseClick: () -> Unit,
) {

    @Immutable
    sealed interface ContentUM {

        data object Loading : ContentUM

        data object NoResults : ContentUM

        data class Recipients(val items: ImmutableList<RecipientUM>) : ContentUM
    }

    @Immutable
    data class RecipientUM(
        val id: String,
        val initial: String,
        val name: String,
        val subtitle: TextReference,
        val onClick: () -> Unit,
    )
}
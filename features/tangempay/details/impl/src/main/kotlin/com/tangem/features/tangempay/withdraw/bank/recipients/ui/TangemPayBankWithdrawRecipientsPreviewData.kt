package com.tangem.features.tangempay.withdraw.bank.recipients.ui

import com.tangem.core.ui.ds2.search.TangemSearch
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.ContentUM
import com.tangem.features.tangempay.withdraw.bank.recipients.state.TangemPayBankWithdrawRecipientsUM.RecipientUM
import kotlinx.collections.immutable.persistentListOf

internal object TangemPayBankWithdrawRecipientsPreviewData {

    private val search = TangemSearch.State(
        placeholderText = resourceReference(R.string.common_search),
        query = "",
        onQueryChange = {},
        isActive = false,
        onActiveChange = {},
        onCloseClick = {},
    )

    private val recipients = persistentListOf(
        RecipientUM(
            id = "1",
            initial = "A",
            name = "Artem Toshmukhamedov",
            subtitle = stringReference("5590013378"),
            onClick = {},
        ),
        RecipientUM(
            id = "2",
            initial = "B",
            name = "Basit Didenko",
            subtitle = stringReference("4830019271"),
            onClick = {},
        ),
        RecipientUM(
            id = "3",
            initial = "R",
            name = "Raxanax",
            subtitle = stringReference("1120938475 · Business"),
            onClick = {},
        ),
        RecipientUM(
            id = "4",
            initial = "S",
            name = "Saburo Arasaka",
            subtitle = stringReference("7719004432"),
            onClick = {},
        ),
        RecipientUM(
            id = "5",
            initial = "S",
            name = "Serhii Buinov",
            subtitle = stringReference("3301198822"),
            onClick = {},
        ),
    )

    val loading = TangemPayBankWithdrawRecipientsUM(
        search = search,
        content = ContentUM.Loading,
        onBackClick = {},
        onCloseClick = {},
    )

    val content = loading.copy(content = ContentUM.Recipients(items = recipients))

    val noResults = loading.copy(
        search = search.copy(query = "Xyz", isActive = true),
        content = ContentUM.NoResults,
    )
}
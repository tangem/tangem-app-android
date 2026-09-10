package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.features.collectibles.impl.main.ui.state.CollectiblesMainUM
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class CollectiblesActionsConverter(
    private val onAddFundsClick: () -> Unit,
    private val onWithdrawalClick: () -> Unit,
) : Converter<Unit, ImmutableList<CollectiblesMainUM.ActionUM>> {

    override fun convert(value: Unit): ImmutableList<CollectiblesMainUM.ActionUM> = persistentListOf(
        CollectiblesMainUM.ActionUM(
            id = ACTION_ADD_FUNDS,
            icon = Icons.ic_arrow_down_24,
            title = stringReference("Add funds"),
            onClick = onAddFundsClick,
        ),
        CollectiblesMainUM.ActionUM(
            id = ACTION_WITHDRAWAL,
            icon = Icons.ic_arrow_up_24,
            title = stringReference("Withdrawal"),
            onClick = onWithdrawalClick,
        ),
    )

    private companion object {

        const val ACTION_ADD_FUNDS = "add_funds"
        const val ACTION_WITHDRAWAL = "withdrawal"
    }
}
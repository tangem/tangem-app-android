package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.features.collectibles.impl.R
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
            icon = Icons.ic_arrow_down_24,
            title = TextReference.Res(R.string.common_add_funds),
            onClick = onAddFundsClick,
        ),
        CollectiblesMainUM.ActionUM(
            icon = Icons.ic_arrow_up_24,
            title = TextReference.Res(R.string.common_withdrawal),
            onClick = onWithdrawalClick,
        ),
    )
}
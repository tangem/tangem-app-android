package com.tangem.features.collectibles.impl.main.model.converter

import com.tangem.core.ui.ds.button.TangemButtonType
import com.tangem.core.ui.ds.button.TangemButtonUM
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_arrow_down_24
import com.tangem.core.ui.res.generated.icons.ic_arrow_up_24
import com.tangem.domain.models.account.CollectiblesAccountStatusValue
import com.tangem.features.collectibles.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

internal class CollectiblesActionsConverter(
    private val onAddFundsClick: () -> Unit,
    private val onWithdrawalClick: () -> Unit,
) : Converter<CollectiblesAccountStatusValue, ImmutableList<TangemButtonUM>> {

    override fun convert(value: CollectiblesAccountStatusValue): ImmutableList<TangemButtonUM> {
        return persistentListOf(
            TangemButtonUM(
                text = TextReference.Res(R.string.common_add_funds),
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_down_24,
                    tintReference = { TangemTheme.colors3.icon.primary },
                ),
                type = TangemButtonType.Secondary,
                isEnabled = value.balance?.usdcToken != null,
                onClick = onAddFundsClick,
            ),
            TangemButtonUM(
                text = TextReference.Res(R.string.common_withdrawal),
                tangemIconUM = TangemIconUM.Icon(
                    imageVector = Icons.ic_arrow_up_24,
                    tintReference = { TangemTheme.colors3.icon.primary },
                ),
                type = TangemButtonType.Secondary,
                // TODO([REDACTED_TASK_KEY]): withdrawal flow is not built yet
                isEnabled = false,
                onClick = onWithdrawalClick,
            ),
        )
    }
}
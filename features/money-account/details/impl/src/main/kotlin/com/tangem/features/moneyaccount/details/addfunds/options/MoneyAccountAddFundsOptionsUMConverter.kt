package com.tangem.features.moneyaccount.details.addfunds.options

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_grid_20
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal class MoneyAccountAddFundsOptionsUMConverter(
    private val depositAddress: String?,
    private val listener: MoneyAccountAddFundsOptionsListener,
) : Converter<Unit, MoneyAccountAddFundsOptionsUM> {

    override fun convert(value: Unit): MoneyAccountAddFundsOptionsUM = MoneyAccountAddFundsOptionsUM(
        items = listOfNotNull(receiveItem()).toImmutableList(),
    )

    private fun receiveItem(): MoneyAccountAddFundsOptionUM? {
        val depositAddress = depositAddress ?: return null
        return MoneyAccountAddFundsOptionUM(
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_grid_20,
                tintReference = { TangemTheme.colors3.icon.brand },
            ),
            title = resourceReference(R.string.tangempay_topup_receive_title),
            description = resourceReference(R.string.money_account_add_funds_receive_description),
            onClick = { listener.onReceiveClick(depositAddress) },
        )
    }
}
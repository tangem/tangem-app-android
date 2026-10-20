package com.tangem.features.moneyaccount.details.addfunds.options

import androidx.compose.ui.graphics.vector.ImageVector
import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_government_20
import com.tangem.core.ui.res.generated.icons.ic_grid_20
import com.tangem.features.moneyaccount.details.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toImmutableList

internal class MoneyAccountAddFundsOptionsUMConverter(
    private val depositAddress: String?,
    private val onReceiveClick: (depositAddress: String) -> Unit,
    private val onBankTransferClick: () -> Unit,
) : Converter<MoneyAccountAddFundsOptionsUMConverter.Input, MoneyAccountAddFundsOptionsUM> {

    data class Input(val isBankTransferLoading: Boolean = false)

    override fun convert(value: Input): MoneyAccountAddFundsOptionsUM = MoneyAccountAddFundsOptionsUM(
        items = listOfNotNull(
            receiveItem(),
            bankTransferItem(isLoading = value.isBankTransferLoading),
        ).toImmutableList(),
    )

    private fun receiveItem(): MoneyAccountAddFundsOptionUM? {
        val depositAddress = depositAddress ?: return null
        return MoneyAccountAddFundsOptionUM(
            icon = brandIcon(Icons.ic_grid_20),
            title = resourceReference(R.string.tangempay_topup_receive_title),
            description = resourceReference(R.string.money_account_add_funds_receive_description),
            onClick = { onReceiveClick(depositAddress) },
        )
    }

    private fun bankTransferItem(isLoading: Boolean) = MoneyAccountAddFundsOptionUM(
        icon = brandIcon(Icons.ic_government_20),
        title = resourceReference(R.string.tangempay_topup_bank_transfer_title),
        description = resourceReference(R.string.tangempay_topup_bank_transfer_body),
        onClick = if (isLoading) null else onBankTransferClick,
        isLoading = isLoading,
    )

    private fun brandIcon(imageVector: ImageVector) = TangemIconUM.Icon(
        imageVector = imageVector,
        tintReference = { TangemTheme.colors3.icon.brand },
    )
}
package com.tangem.features.tangempay.addfunds

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_20
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.features.tangempay.details.impl.R
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.toPersistentList

internal class TangemPayAddFundsUMConverter(
    private val listener: AddFundsListener,
    private val shouldShowBankTransfer: Boolean,
) : Converter<TangemPayAddFundsUMConverter.Input, TangemPayAddFundsUM> {

    data class Input(
        val data: TangemPayTopUpData,
        val isBankTransferLoading: Boolean = false,
    )

    override fun convert(value: Input): TangemPayAddFundsUM {
        val swap = TangemPayAddFundsItemUM(
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_logo_tangem_20,
                tintReference = {
                    TangemTheme.colors3.icon.brand
                },
            ),
            title = resourceReference(R.string.tangempay_topup_swap_title),
            description = resourceReference(R.string.tangempay_topup_swap_body),
            onClick = { listener.onClickSwap(value.data) },
        )
        val receive = TangemPayAddFundsItemUM(
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_card_20,
                tintReference = {
                    TangemTheme.colors3.icon.brand
                },
            ),
            title = resourceReference(R.string.tangempay_topup_receive_title),
            description = resourceReference(R.string.tangempay_topup_receive_body_multichain),
            onClick = { listener.onClickReceive(value.data) },
        )
        val bankTransfer = bankTransferItem(isLoading = value.isBankTransferLoading).takeIf { shouldShowBankTransfer }
        return TangemPayAddFundsUM(
            items = listOfNotNull(bankTransfer, swap, receive).toPersistentList(),
            dismiss = listener::onDismissAddFunds,
            errorMessage = null,
        )
    }

    private fun bankTransferItem(isLoading: Boolean) = TangemPayAddFundsItemUM(
        icon = TangemIconUM.Icon(
            imageVector = Icons.ic_sign_usd_20,
            tintReference = { TangemTheme.colors3.icon.brand },
        ),
        title = resourceReference(R.string.tangempay_topup_bank_transfer_title),
        description = resourceReference(R.string.tangempay_topup_bank_transfer_body),
        onClick = if (isLoading) null else listener::onClickBankTransfer,
        isLoading = isLoading,
    )
}
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
    val listener: AddFundsListener,
    val shouldShowBankTransfer: Boolean,
    val isMultichainEnabled: Boolean,
) : Converter<TangemPayTopUpData, TangemPayAddFundsUM> {

    override fun convert(value: TangemPayTopUpData): TangemPayAddFundsUM {
        val swap = TangemPayAddFundsItemUM(
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_logo_tangem_20,
                tintReference = {
                    TangemTheme.colors3.icon.brand
                },
            ),
            title = resourceReference(R.string.tangempay_topup_swap_title),
            description = resourceReference(R.string.tangempay_topup_swap_body),
            onClick = { listener.onClickSwap(value) },
        )
        val receive = TangemPayAddFundsItemUM(
            icon = TangemIconUM.Icon(
                imageVector = Icons.ic_card_20,
                tintReference = {
                    TangemTheme.colors3.icon.brand
                },
            ),
            title = resourceReference(R.string.tangempay_topup_receive_title),
            description = resourceReference(
                if (isMultichainEnabled) {
                    R.string.tangempay_topup_receive_body_multichain
                } else {
                    R.string.tangempay_topup_receive_body
                },
            ),
            onClick = { listener.onClickReceive(value) },
        )
        val bankTransfer = if (shouldShowBankTransfer) {
            TangemPayAddFundsItemUM(
                icon = TangemIconUM.Icon(
                    imageVector = Icons.ic_sign_usd_20,
                    tintReference = { TangemTheme.colors3.icon.brand },
                ),
                title = resourceReference(R.string.tangempay_topup_bank_transfer_title),
                description = resourceReference(R.string.tangempay_topup_bank_transfer_body),
                onClick = listener::onClickBankTransfer,
            )
        } else {
            null
        }
        // The multichain design puts Bank transfer first; the legacy sheet keeps it last.
        val items = if (isMultichainEnabled) {
            listOfNotNull(bankTransfer, swap, receive)
        } else {
            listOfNotNull(swap, receive, bankTransfer)
        }
        return TangemPayAddFundsUM(
            items = items.toPersistentList(),
            dismiss = listener::onDismissAddFunds,
            errorMessage = null,
        )
    }
}
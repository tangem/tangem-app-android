package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.core.ui.res.generated.icons.ic_sign_usd_20
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddFundsItemUM
import com.tangem.features.tangempay.entity.TangemPayAddFundsUM
import com.tangem.utils.converter.Converter
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.toPersistentList

internal class TangemPayAddFundsUMConverter(
    val listener: AddFundsListener,
    val isRedesignEnabled: Boolean,
    val shouldShowBankTransfer: Boolean,
) : Converter<TangemPayTopUpData, TangemPayAddFundsUM> {

    @Suppress("UnnecessaryLet")
    override fun convert(value: TangemPayTopUpData): TangemPayAddFundsUM {
        return TangemPayAddFundsUM(
            items = buildList {
                TangemPayAddFundsItemUM(
                    icon = if (isRedesignEnabled) {
                        TangemIconUM.Icon(
                            imageVector = Icons.ic_logo_tangem_20,
                            tintReference = {
                                TangemTheme.colors3.icon.brand
                            },
                        )
                    } else {
                        TangemIconUM.Icon(
                            iconRes = R.drawable.ic_exchange_vertical_24,
                            tintReference = {
                                TangemTheme.colors.icon.accent
                            },
                        )
                    },
                    title = resourceReference(R.string.tangempay_topup_swap_title),
                    description = resourceReference(R.string.tangempay_topup_swap_body),
                    onClick = { listener.onClickSwap(value) },
                ).let(::add)
                TangemPayAddFundsItemUM(
                    icon = if (isRedesignEnabled) {
                        TangemIconUM.Icon(
                            imageVector = Icons.ic_card_20,
                            tintReference = {
                                TangemTheme.colors3.icon.brand
                            },
                        )
                    } else {
                        TangemIconUM.Icon(
                            iconRes = R.drawable.ic_arrow_down_24,
                            tintReference = {
                                TangemTheme.colors.icon.accent
                            },
                        )
                    },
                    title = resourceReference(R.string.tangempay_topup_receive_title),
                    description = resourceReference(R.string.tangempay_topup_receive_body),
                    onClick = { listener.onClickReceive(value) },
                ).let(::add)
                addIf(
                    condition = shouldShowBankTransfer,
                    create = {
                        TangemPayAddFundsItemUM(
                            icon = TangemIconUM.Icon(
                                imageVector = Icons.ic_sign_usd_20,
                                tintReference = { TangemTheme.colors3.icon.brand },
                            ),
                            title = stringReference("Bank transfer"),
                            description = stringReference("Receive fiat USD via ACH/FedWire"),
                            onClick = listener::onClickBankTransfer,
                        )
                    },
                )
            }.toPersistentList(),
            dismiss = listener::onDismissAddFunds,
            errorMessage = null,
        )
    }
}
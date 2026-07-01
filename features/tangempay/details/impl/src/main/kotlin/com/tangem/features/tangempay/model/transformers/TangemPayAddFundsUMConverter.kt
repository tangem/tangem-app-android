package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.ds.image.TangemIconUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.res.TangemTheme
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_card_20
import com.tangem.core.ui.res.generated.icons.ic_logo_tangem_20
import com.tangem.domain.pay.model.TangemPayTopUpData
import com.tangem.features.tangempay.components.AddFundsListener
import com.tangem.features.tangempay.details.impl.R
import com.tangem.features.tangempay.entity.TangemPayAddFundsItemUM
import com.tangem.features.tangempay.entity.TangemPayAddFundsUM
import com.tangem.features.tangempay.entity.TangemPayDetailsErrorType
import com.tangem.features.tangempay.utils.TangemPayMessagesFactory
import com.tangem.utils.converter.Converter
import kotlinx.collections.immutable.persistentListOf

internal class TangemPayAddFundsUMConverter(
    val listener: AddFundsListener,
    val isRedesignEnabled: Boolean,
) : Converter<TangemPayTopUpData?, TangemPayAddFundsUM> {

    override fun convert(value: TangemPayTopUpData?): TangemPayAddFundsUM {
        return if (value == null) {
            TangemPayAddFundsUM(
                items = persistentListOf(),
                dismiss = listener::onDismissAddFunds,
                errorMessage = TangemPayMessagesFactory.createErrorMessage(
                    errorType = TangemPayDetailsErrorType.Receive,
                ).messageBottomSheetUM,
            )
        } else {
            TangemPayAddFundsUM(
                items = persistentListOf(
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
                        title = TextReference.Res(R.string.tangempay_topup_swap_title),
                        description = TextReference.Res(R.string.tangempay_topup_swap_body),
                        onClick = { listener.onClickSwap(value) },
                    ),
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
                        title = TextReference.Res(R.string.tangempay_topup_receive_title),
                        description = TextReference.Res(R.string.tangempay_topup_receive_body),
                        onClick = { listener.onClickReceive(value) },
                    ),
                ),
                dismiss = listener::onDismissAddFunds,
                errorMessage = null,
            )
        }
    }
}
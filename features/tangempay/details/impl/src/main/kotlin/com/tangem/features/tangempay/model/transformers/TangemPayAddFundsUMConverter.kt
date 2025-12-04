package com.tangem.features.tangempay.model.transformers

import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.pay.TangemPayTopUpData
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
) : Converter<TangemPayTopUpData?, TangemPayAddFundsUM> {

    override fun convert(value: TangemPayTopUpData?): TangemPayAddFundsUM {
        return if (value == null) {
            TangemPayAddFundsUM(
                items = persistentListOf(),
                dismiss = listener::onDismissAddFunds,
                errorMessage = TangemPayMessagesFactory.createErrorMessage(
                    errorType = TangemPayDetailsErrorType.Receive,
                ).messageBottomSheetUMV2,
            )
        } else {
            TangemPayAddFundsUM(
                items = persistentListOf(
                    TangemPayAddFundsItemUM(
                        iconRes = R.drawable.ic_exchange_vertical_24,
                        title = TextReference.Res(R.string.common_exchange),
                        description = TextReference.Res(R.string.tangempay_card_details_swap_description),
                        onClick = { listener.onClickSwap(value) },
                    ),
                    TangemPayAddFundsItemUM(
                        iconRes = R.drawable.ic_arrow_down_24,
                        title = TextReference.Res(R.string.common_receive),
                        description = TextReference.Res(R.string.tangempay_card_details_receive_description),
                        onClick = { listener.onClickReceive(value) },
                    ),
                ),
                dismiss = listener::onDismissAddFunds,
                errorMessage = null,
            )
        }
    }
}
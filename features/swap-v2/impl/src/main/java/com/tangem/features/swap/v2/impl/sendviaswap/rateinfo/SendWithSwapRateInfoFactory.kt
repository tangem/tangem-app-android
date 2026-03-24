package com.tangem.features.swap.v2.impl.sendviaswap.rateinfo

import com.tangem.core.ui.components.bottomsheets.message.*
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.express.models.ExpressRateType
import com.tangem.features.swap.v2.impl.R

internal object SendWithSwapRateInfoFactory {

    fun getRateTypeMessage(expressRateType: ExpressRateType, onDismiss: () -> Unit): MessageBottomSheetUM {
        val isFixed = expressRateType == ExpressRateType.Fixed
        return messageBottomSheetUM {
            onDismiss(onDismiss)
            infoBlock {
                iconImage(if (isFixed) R.drawable.ic_fixed_32 else R.drawable.ic_floating_32)
                title = resourceReference(
                    if (isFixed) R.string.send_rate_fixed_info_title else R.string.send_rate_floating_info_title,
                )
                body = resourceReference(
                    if (isFixed) {
                        R.string.send_rate_fixed_info_description
                    } else {
                        R.string.send_rate_floating_info_description
                    },
                )
            }
            secondaryButton {
                text = resourceReference(R.string.common_got_it)
                onClick { closeBs() }
            }
        }
    }
}
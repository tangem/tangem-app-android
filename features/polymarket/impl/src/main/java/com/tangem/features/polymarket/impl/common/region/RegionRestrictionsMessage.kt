package com.tangem.features.polymarket.impl.common.region

import com.tangem.core.res.R
import com.tangem.core.ui.ds2.modalstatus.TangemModalStatus
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.ModalMessage
import com.tangem.core.ui.message.modalMessage
import com.tangem.core.ui.res.generated.icons.Icons
import com.tangem.core.ui.res.generated.icons.ic_error_28

internal fun regionRestrictionsMessage(): ModalMessage = modalMessage {
    status {
        variant = TangemModalStatus.Variant.Warning
        title = resourceReference(R.string.prediction_region_restrictions_title)
        description = resourceReference(R.string.prediction_region_restrictions_subtitle)
        glyph(Icons.ic_error_28)
    }
    primaryButton {
        text = resourceReference(R.string.common_close)
    }
}
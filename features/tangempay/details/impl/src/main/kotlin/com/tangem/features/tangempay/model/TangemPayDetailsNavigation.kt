package com.tangem.features.tangempay.model

import com.tangem.core.ui.components.bottomsheets.message.MessageBottomSheetUMV2
import com.tangem.domain.models.TokenReceiveConfig
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayDetailsNavigation {

    data class Receive(
        val config: TokenReceiveConfig,
    ) : TangemPayDetailsNavigation()

    data class Error(
        val messageUM: MessageBottomSheetUMV2,
    ) : TangemPayDetailsNavigation()
}
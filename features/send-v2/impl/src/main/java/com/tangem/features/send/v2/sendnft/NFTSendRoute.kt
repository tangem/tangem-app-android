package com.tangem.features.send.v2.sendnft

import com.tangem.features.send.v2.common.CommonSendRoute
import kotlinx.serialization.Serializable

@Serializable
internal sealed class NFTSendRoute : CommonSendRoute {

    @Serializable
    data object Empty : NFTSendRoute(), CommonSendRoute.Empty {
        override val isEditMode = false
    }

    @Serializable
    data object Confirm : NFTSendRoute(), CommonSendRoute.Confirm {
        override val isEditMode: Boolean = false
    }

    @Serializable
    data class Destination(
        override val isEditMode: Boolean,
    ) : NFTSendRoute(), CommonSendRoute.Destination

    @Serializable
    data object Fee : NFTSendRoute(), CommonSendRoute.Fee {
        override val isEditMode: Boolean = true
    }
}
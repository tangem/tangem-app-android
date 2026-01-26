package com.tangem.features.send.v2.feeselector.route

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.features.send.v2.impl.R
import kotlinx.serialization.Serializable

@Immutable
@Serializable
internal sealed interface FeeSelectorRoute : Route {

    val title: TextReference

    @Serializable
    data object NetworkFee : FeeSelectorRoute {
        override val title: TextReference = resourceReference(R.string.common_network_fee_title)
    }

    @Serializable
    data object ChooseSpeed : FeeSelectorRoute {
        override val title: TextReference = resourceReference(R.string.fee_selector_choose_speed_title)
    }

    @Serializable
    data object ChooseToken : FeeSelectorRoute {
        override val title: TextReference = resourceReference(R.string.fee_selector_choose_token_title)
    }
}
package com.tangem.features.tangempay.multichain.othernetworks

import com.tangem.core.ui.extensions.TextReference

internal data class PaymentOtherNetworksUM(
    val title: TextReference,
    val subtitle: TextReference,
    val onClose: () -> Unit,
)
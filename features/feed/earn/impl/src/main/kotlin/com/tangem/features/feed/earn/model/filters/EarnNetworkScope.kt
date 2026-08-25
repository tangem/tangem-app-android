package com.tangem.features.feed.earn.model.filters

import com.tangem.core.ui.R
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference

/** Which set of networks the "Filter by network" sheet lists: every earn network, or the user's own ones. */
internal enum class EarnNetworkScope(val title: TextReference) {
    AllNetworks(resourceReference(R.string.earn_filter_all_networks)),
    MyNetworks(resourceReference(R.string.earn_filter_my_networks)),
}
package com.tangem.features.feed.earn.model.filters.state

import com.tangem.core.ui.R
import com.tangem.core.ui.ds2.filter.TangemFilterItemUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.stringReference
import com.tangem.domain.earn.model.EarnFilter
import com.tangem.domain.earn.model.EarnFilterNetwork
import com.tangem.domain.earn.model.EarnFilterType
import com.tangem.domain.models.earn.EarnNetworks
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

/**
 * Builds the two filter chips of the Best opportunities section.
 *
 * A filter left at its default renders as [TangemFilterItemUM.Inactive] — a label and a chevron, with
 * no clear cross, since there is nothing to clear. A picked value renders as
 * [TangemFilterItemUM.Active], whose cross drops that filter back to its default.
 */
internal class EarnFilterChipsFactory(
    private val onNetworkClick: () -> Unit,
    private val onTypeClick: () -> Unit,
    private val onNetworkClear: () -> Unit,
    private val onTypeClear: () -> Unit,
) {

    /** Both chips shimmer until the applied filter and the network list have arrived. */
    fun create(filter: EarnFilter?, networks: EarnNetworks?): ImmutableList<TangemFilterItemUM> {
        if (filter == null || networks == null) return LOADING

        return persistentListOf(
            networkChip(filter.earnFilterNetwork),
            typeChip(filter.earnFilterType),
        )
    }

    private fun networkChip(filter: EarnFilterNetwork): TangemFilterItemUM = when (filter) {
        is EarnFilterNetwork.AllNetworks -> TangemFilterItemUM.Inactive(
            id = NETWORK_ID,
            label = resourceReference(R.string.earn_filter_all_networks),
            onClick = onNetworkClick,
        )
        is EarnFilterNetwork.MyNetworks -> activeNetworkChip(resourceReference(R.string.earn_filter_my_networks))
        is EarnFilterNetwork.Specific -> activeNetworkChip(stringReference(filter.fullName))
    }

    private fun activeNetworkChip(value: TextReference) = TangemFilterItemUM.Active(
        id = NETWORK_ID,
        value = value,
        onClick = onNetworkClick,
        onClearClick = onNetworkClear,
    )

    private fun typeChip(filter: EarnFilterType): TangemFilterItemUM = when (filter) {
        EarnFilterType.ALL -> TangemFilterItemUM.Inactive(
            id = TYPE_ID,
            label = resourceReference(R.string.earn_filter_all_types),
            onClick = onTypeClick,
        )
        EarnFilterType.STAKING,
        EarnFilterType.YIELD,
        -> TangemFilterItemUM.Active(
            id = TYPE_ID,
            value = EarnFilterTypeConverter().convert(filter).text,
            onClick = onTypeClick,
            onClearClick = onTypeClear,
        )
    }

    companion object {

        private const val NETWORK_ID = "network"
        private const val TYPE_ID = "type"

        val LOADING: ImmutableList<TangemFilterItemUM> = persistentListOf(
            TangemFilterItemUM.Loading(id = NETWORK_ID),
            TangemFilterItemUM.Loading(id = TYPE_ID),
        )
    }
}
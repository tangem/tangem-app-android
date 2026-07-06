package com.tangem.features.addressbook.selectnetworks.state.transformers

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.toNetworkId
import com.tangem.features.addressbook.selectnetworks.state.transformers.converter.SelectNetworkItemConverter
import com.tangem.features.addressbook.selectnetworks.ui.state.SelectNetworksUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.toImmutableList

internal class UpdateNetworksContentTransformer(
    private val matchedBlockchains: List<Blockchain>,
    private val query: String,
    private val selectedNetworkIds: Set<String>,
    private val onToggle: (networkId: String) -> Unit,
) : Transformer<SelectNetworksUM> {

    override fun transform(prevState: SelectNetworksUM): SelectNetworksUM {
        val visible = if (query.isBlank()) {
            matchedBlockchains
        } else {
            matchedBlockchains.filter { blockchain ->
                blockchain.fullName.contains(query, ignoreCase = true) ||
                    blockchain.currency.contains(query, ignoreCase = true) ||
                    blockchain.name.contains(query, ignoreCase = true)
            }
        }
        val networks = visible
            .map { blockchain ->
                SelectNetworkItemConverter().convert(
                    SelectNetworkItemConverter.Input(
                        blockchain = blockchain,
                        isSelected = blockchain.toNetworkId() in selectedNetworkIds,
                        onToggle = onToggle,
                    ),
                )
            }
            .toImmutableList()

        // Search field is owned by UpdateSelectNetworksSearchBarTransformer and intentionally left untouched here.
        return prevState.copy(
            networks = networks,
            doneButton = prevState.doneButton.copy(isEnabled = selectedNetworkIds.isNotEmpty()),
        )
    }
}
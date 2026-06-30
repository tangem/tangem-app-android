package com.tangem.features.addressbook.editcontact.model

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchainsdk.utils.fromNetworkId
import com.tangem.blockchainsdk.utils.getSupportedTransactionExtras
import com.tangem.domain.addressbook.model.AddressEntry
import com.tangem.domain.addressbook.model.AddressEntryId
import com.tangem.domain.models.network.Network
import com.tangem.features.addressbook.editcontact.ui.state.ValidatedAddress
import java.util.UUID

internal class ContactAddressEntriesConverter {

    fun convert(addresses: List<ValidatedAddress>): List<AddressEntry> {
        return addresses.flatMap { address ->
            address.networkIds.map { rawId -> address.toAddressEntry(rawId) }
        }
    }

    private fun ValidatedAddress.toAddressEntry(rawId: String): AddressEntry {
        val blockchain = Blockchain.fromNetworkId(rawId)
        val hasExtrasSupport = blockchain?.getSupportedTransactionExtras()?.isTxExtrasSupported() == true
        return AddressEntry(
            id = AddressEntryId(UUID.randomUUID().toString()),
            address = address,
            networkId = Network.RawID(rawId),
            networkName = blockchain?.fullName ?: rawId,
            memo = memo?.takeIf { hasExtrasSupport },
            signature = "",
        )
    }
}
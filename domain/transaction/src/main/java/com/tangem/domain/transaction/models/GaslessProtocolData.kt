package com.tangem.domain.transaction.models

import com.tangem.domain.models.network.Network
import com.tangem.domain.transaction.models.tron.TronGaslessQuote

/**
 * Which protocol fronts this fee, and what the send step needs to settle it. Fee resolvers
 * produce it, the send step dispatches on it, so a new protocol is added by extending this hierarchy —
 * routing on the fee's own shape instead would let it fall into whichever branch happened to be last.
 */
sealed interface GaslessProtocolData {

    /** EIP-712/7702 relaying: everything the send step needs is already on [TransactionFeeExtended]. */
    data object Evm : GaslessProtocolData

    /** Tron compensation: the send step submits the quoted compensation transfer alongside the user's own. */
    data class Tron(val quote: TronGaslessQuote) : GaslessProtocolData

    /**
     * Solana sponsorship: the sponsor pays the network fee as [feePayer], with no compensation from the user.
     * [network] is the network whose account signs, which a provider-built (compiled) transaction does not name.
     */
    data class Solana(val feePayer: String, val network: Network) : GaslessProtocolData
}
package com.tangem.domain.transaction.models

import com.tangem.domain.transaction.models.tron.TronGaslessQuote

/**
 * Which protocol compensates the relayer for this fee, and what it needs to settle it. Fee resolvers
 * produce it, the send step dispatches on it, so a new protocol is added by extending this hierarchy —
 * routing on the fee's own shape instead would let it fall into whichever branch happened to be last.
 */
sealed interface GaslessProtocolData {

    /** EIP-712/7702 relaying: everything the send step needs is already on [TransactionFeeExtended]. */
    data object Evm : GaslessProtocolData

    /** Tron compensation: the send step submits the quoted compensation transfer alongside the user's own. */
    data class Tron(val quote: TronGaslessQuote) : GaslessProtocolData
}
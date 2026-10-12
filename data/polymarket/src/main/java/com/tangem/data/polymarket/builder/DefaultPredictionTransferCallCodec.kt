package com.tangem.data.polymarket.builder

import com.tangem.domain.polymarket.model.PolymarketApprovalCall
import com.tangem.domain.polymarket.model.PredictionTransferCall
import com.tangem.domain.polymarket.model.PredictionTransferCallCodec
import java.math.BigInteger
import javax.inject.Inject

internal class DefaultPredictionTransferCallCodec @Inject constructor() : PredictionTransferCallCodec {

    override fun build(recipient: String, amountBaseUnits: BigInteger): PolymarketApprovalCall =
        PolymarketTransferCallBuilder.build(recipient = recipient, amountBaseUnits = amountBaseUnits)

    override fun decode(call: PolymarketApprovalCall): PredictionTransferCall? =
        PolymarketTransferCallBuilder.decode(call)
}
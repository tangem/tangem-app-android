package com.tangem.domain.card

import com.tangem.domain.card.model.ScanCardResult
import com.tangem.domain.core.chain.Chain
import com.tangem.domain.core.chain.ChainResult

typealias ScanCardChain = Chain<ScanCardResult>
typealias ScanCardChainResult = ChainResult<ScanCardResult>

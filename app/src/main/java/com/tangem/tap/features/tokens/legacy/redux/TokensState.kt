package com.tangem.tap.features.tokens.legacy.redux

import com.tangem.blockchain.common.Blockchain
import com.tangem.blockchain.common.Token
import com.tangem.tap.domain.model.WalletDataModel
import org.rekotlin.StateType

data class TokensState(
    val isManageAccess: Boolean = false,
    val addedWallets: List<WalletDataModel> = emptyList(),
    val addedTokens: List<TokenWithBlockchain> = emptyList(),
    val addedBlockchains: List<Blockchain> = emptyList(),
) : StateType

// TODO: [REDACTED_TASK_KEY] Remove this class
data class TokenWithBlockchain(val token: Token, val blockchain: Blockchain)
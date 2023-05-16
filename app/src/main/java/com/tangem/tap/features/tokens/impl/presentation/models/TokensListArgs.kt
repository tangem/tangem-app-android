package com.tangem.tap.features.tokens.impl.presentation.models

import com.tangem.blockchain.common.Blockchain
import com.tangem.tap.features.tokens.legacy.redux.TokenWithBlockchain
import com.tangem.tap.store

/**
 * Required data for tokens list screen
 * FIXME("Necessary to avoid using redux state")
 *
[REDACTED_AUTHOR]
 */
class TokensListArgs {
    /** Tokens list screen mode */
    val isManageAccess: Boolean get() = store.state.tokensState.isManageAccess

    /** Tokens list that accessible from the main screen */
    val mainScreenTokenList: List<TokenWithBlockchain> get() = store.state.tokensState.addedTokens

    /** Blockchains list that accessible from the main screen */
    val mainScreenBlockchainList: List<Blockchain> get() = store.state.tokensState.addedBlockchains
}
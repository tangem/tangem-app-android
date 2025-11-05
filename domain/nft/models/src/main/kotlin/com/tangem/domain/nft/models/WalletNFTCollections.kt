package com.tangem.domain.nft.models

import com.tangem.domain.models.account.Account

data class WalletNFTCollections(
    val collections: Map<Account, List<NFTCollections>>,
) {
    val flattenCollections by lazy { collections.values.flatten() }
}
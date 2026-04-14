package com.tangem.domain.nft.models

import com.tangem.domain.models.account.Account

data class WalletNFTCollections(
    val collections: Map<Account.CryptoPortfolio, List<NFTCollections>>,
) {
    val flattenCollections by lazy { collections.values.flatten() }
}
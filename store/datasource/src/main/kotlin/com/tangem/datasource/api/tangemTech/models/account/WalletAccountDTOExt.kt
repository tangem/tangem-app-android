package com.tangem.datasource.api.tangemTech.models.account

/**
 * Whether the account record describes the wallet's collectibles account.
 *
 * The record only registers the account with the backend: it holds no tokens, and its
 * [WalletAccountDTO.derivationIndex] is the account node of the collectibles owner key path, not a derivation index
 * of the wallet's portfolios.
 */
val WalletAccountDTO.isCollectibles: Boolean
    get() = type == WalletAccountDTO.Type.COLLECTIBLES
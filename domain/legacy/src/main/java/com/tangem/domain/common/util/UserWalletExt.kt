package com.tangem.domain.common.util

import com.tangem.domain.wallets.models.UserWallet

/**
 * Get total cards count in wallets set for a card that was saved in [UserWallet]
 *
 * @return null if wallet is not multi-currency or total cards count
 */
fun UserWallet.getCardsCount(): Int? = scanResponse.getCardsCount()

/**
 * Get backup cards count for a card that was saved in [UserWallet]
 *
 * @return null if wallet is not multi-currency or total cards count
 */
fun UserWallet.getBackupCardsCount(): Int? = scanResponse.getBackupCardsCount()
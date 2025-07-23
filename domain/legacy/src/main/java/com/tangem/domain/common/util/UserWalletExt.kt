package com.tangem.domain.common.util

import com.tangem.domain.models.wallet.UserWallet

/**
 * Get total cards count in wallets set for a card that was saved in [UserWallet]
 *
 * @return null if wallet is not multi-currency or total cards count
 */
fun UserWallet.Cold.getCardsCount(): Int? = scanResponse.getCardsCount()

/**
 * Get backup cards count for a card that was saved in [UserWallet]
 *
 * @return null if wallet is not multi-currency or total cards count
 */
fun UserWallet.Cold.getBackupCardsCount(): Int? = scanResponse.getBackupCardsCount()
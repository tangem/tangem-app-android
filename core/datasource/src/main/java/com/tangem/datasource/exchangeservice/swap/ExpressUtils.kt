package com.tangem.datasource.exchangeservice.swap

import com.tangem.common.CardIdRangeDec
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.domain.wallets.models.UserWallet
import kotlinx.coroutines.runBlocking

object ExpressUtils {
    private const val BATCH_ID_CHANGENOW = "BB000013"
    private const val BATCH_ID_PARTNER = "AF990015"

    fun getRefCode(userWallet: UserWallet, appPreferencesStore: AppPreferencesStore): String {
        return when {
            isRing(userWallet, appPreferencesStore) -> "ring"
            isChangeNow(userWallet) -> "ChangeNow"
            isPartner(userWallet) -> "partner"
            else -> ""
        }
    }

    private fun isRing(selectedUserWallet: UserWallet, appPreferencesStore: AppPreferencesStore): Boolean {
        val addedWalletsWithRings = runBlocking {
            appPreferencesStore.getSyncOrDefault(
                key = PreferencesKeys.ADDED_WALLETS_WITH_RING_KEY,
                default = emptySet(),
            )
        }

        return addedWalletsWithRings.contains(selectedUserWallet.walletId.stringValue)
    }

    private fun isChangeNow(selectedUserWallet: UserWallet): Boolean {
        val changeNowRange = CardIdRangeDec(
            start = "AF99001800554008",
            end = "AF99001800559994",
        )
        val card = selectedUserWallet.scanResponse.card
        return card.batchId == BATCH_ID_CHANGENOW || changeNowRange?.contains(card.cardId) == true
    }

    private fun isPartner(selectedUserWallet: UserWallet): Boolean {
        return selectedUserWallet.scanResponse.card.batchId == BATCH_ID_PARTNER
    }
}
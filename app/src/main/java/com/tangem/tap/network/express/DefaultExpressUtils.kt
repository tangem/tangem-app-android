package com.tangem.tap.network.express

import com.tangem.common.CardIdRangeDec
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.store.datasource.preferences.StorePreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.grow.datasource.express.ExpressUtils
import javax.inject.Inject

internal class DefaultExpressUtils @Inject constructor(
    private val appPreferencesStore: AppPreferencesStore,
    private val storePreferencesKeys: StorePreferencesKeys,
) : ExpressUtils {

    override suspend fun getRefCode(userWallet: UserWallet?): String? {
        return when (userWallet) {
            is UserWallet.Cold -> when {
                isRing(userWallet) -> "ring"
                isChangeNow(userWallet) -> "ChangeNow"
                isPartner(userWallet) -> "partner"
                else -> null
            }
            else -> null
        }
    }

    private suspend fun isRing(selectedUserWallet: UserWallet.Cold): Boolean {
        val addedWalletsWithRings = appPreferencesStore.getSyncOrDefault(
            key = storePreferencesKeys.addedWalletsWithRingKey,
            default = emptySet(),
        )

        return addedWalletsWithRings.contains(selectedUserWallet.walletId.stringValue)
    }

    private fun isChangeNow(selectedUserWallet: UserWallet.Cold): Boolean {
        val changeNowRange = CardIdRangeDec(
            start = "AF99001800554008",
            end = "AF99001800559994",
        )
        val card = selectedUserWallet.scanResponse.card
        return card.batchId == BATCH_ID_CHANGENOW || changeNowRange?.contains(card.cardId) == true
    }

    private fun isPartner(selectedUserWallet: UserWallet.Cold): Boolean {
        return selectedUserWallet.scanResponse.card.batchId == BATCH_ID_PARTNER
    }

    private companion object {
        const val BATCH_ID_CHANGENOW = "BB000013"
        const val BATCH_ID_PARTNER = "AF990015"
    }
}
package com.tangem.tap.network.auth

import com.tangem.common.CardIdRangeDec
import com.tangem.datasource.local.preferences.AppPreferencesStore
import com.tangem.datasource.local.preferences.PreferencesKeys
import com.tangem.datasource.local.preferences.utils.getSyncOrDefault
import com.tangem.datasource.local.userwallet.UserWalletsStore
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.lib.auth.ExpressAuthProvider
import kotlinx.coroutines.runBlocking
import java.util.UUID
import java.util.concurrent.atomic.AtomicReference

internal class DefaultExpressAuthProvider(
    private val userWalletsStore: UserWalletsStore,
    private val appPreferencesStore: AppPreferencesStore,
) : ExpressAuthProvider {

    private var uuid = AtomicReference(UUID.randomUUID())

    override fun getUserId(): String {
        return userWalletsStore.selectedUserWalletOrNull?.walletId?.stringValue ?: error("No user id provided")
    }

    override fun getSessionId(): String {
        return uuid.get().toString()
    }

    override fun getRefCode(): String {
        val selectedUserWallet = userWalletsStore.selectedUserWalletOrNull ?: error("Can not get selected user wallet")

        return when {
            isRing(selectedUserWallet) -> "ring"
            isChangeNow(selectedUserWallet) -> "ChangeNow"
            isPartner(selectedUserWallet) -> "partner"
            else -> ""
        }
    }

    private fun isRing(selectedUserWallet: UserWallet): Boolean {
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

    private companion object {
        const val BATCH_ID_CHANGENOW = "BB000013"
        const val BATCH_ID_PARTNER = "AF990015"
    }
}
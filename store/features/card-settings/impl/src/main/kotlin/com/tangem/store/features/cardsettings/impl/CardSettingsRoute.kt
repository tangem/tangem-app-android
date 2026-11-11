package com.tangem.store.features.cardsettings.impl

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CardSettingsRoute : Route {

    @Serializable
    data object Main : CardSettingsRoute

    @Serializable
    data class SecurityMode(val userWalletId: UserWalletId) : CardSettingsRoute

    @Serializable
    data class ResetCard(
        val userWalletId: UserWalletId,
        val cardId: String,
        val isActiveBackupStatus: Boolean,
        val backupCardsCount: Int,
        val hasTangemPay: Boolean,
    ) : CardSettingsRoute

    @Serializable
    data object AccessCodeRecovery : CardSettingsRoute
}
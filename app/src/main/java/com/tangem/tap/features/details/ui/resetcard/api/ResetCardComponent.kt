package com.tangem.tap.features.details.ui.resetcard.api

import com.tangem.core.decompose.factory.ComponentFactory
import com.tangem.core.ui.decompose.ComposableContentComponent
import com.tangem.domain.wallets.models.UserWalletId

interface ResetCardComponent : ComposableContentComponent {

    data class Params(
        val userWalletId: UserWalletId,
        val cardId: String,
        val isActiveBackupStatus: Boolean,
        val backupCardsCount: Int,
    )

    interface Factory : ComponentFactory<Params, ResetCardComponent>
}
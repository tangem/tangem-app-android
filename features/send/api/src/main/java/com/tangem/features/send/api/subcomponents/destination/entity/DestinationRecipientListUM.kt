package com.tangem.features.send.api.subcomponents.destination.entity

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.common.ui.account.AccountTitleUM
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

@Immutable
data class DestinationRecipientListUM(
    val id: String,
    val title: TextReference = TextReference.EMPTY,
    val subtitle: TextReference = TextReference.EMPTY,
    val accountTitleUM: AccountTitleUM.Account? = null,
    val timestamp: TextReference? = null,
    val subtitleEndOffset: Int = 0,
    @DrawableRes val subtitleIconRes: Int? = null,
    val isVisible: Boolean = true,
    val isLoading: Boolean = false,
    val userWalletId: UserWalletId? = null,
    val network: Network? = null,
    val accountId: AccountId? = null,
    val address: String? = null,
)
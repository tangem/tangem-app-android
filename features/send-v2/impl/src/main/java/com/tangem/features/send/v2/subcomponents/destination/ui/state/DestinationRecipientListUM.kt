package com.tangem.features.send.v2.subcomponents.destination.ui.state

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

@Immutable
internal data class DestinationRecipientListUM(
    val id: String,
    val title: TextReference = TextReference.Companion.EMPTY,
    val subtitle: TextReference = TextReference.Companion.EMPTY,
    val timestamp: TextReference? = null,
    val subtitleEndOffset: Int = 0,
    @DrawableRes val subtitleIconRes: Int? = null,
    val isVisible: Boolean = true,
    val isLoading: Boolean = false,
    val userWalletId: UserWalletId? = null,
    val network: Network? = null,
    val address: String? = null,
)
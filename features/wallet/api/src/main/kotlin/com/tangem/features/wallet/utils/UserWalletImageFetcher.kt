package com.tangem.features.wallet.utils

import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.operations.attestation.ArtworkSize
import kotlinx.coroutines.flow.Flow

interface UserWalletImageFetcher {

    fun walletImage(walletId: UserWalletId, size: ArtworkSize): Flow<UserWalletItemUM.ImageState>
    fun walletImage(cardDTO: CardDTO, size: ArtworkSize): Flow<UserWalletItemUM.ImageState>
    fun walletImage(wallet: UserWallet, size: ArtworkSize): Flow<UserWalletItemUM.ImageState>

    fun walletsImage(
        wallets: Collection<UserWallet>,
        size: ArtworkSize,
    ): Flow<Map<UserWalletId, UserWalletItemUM.ImageState>>
}
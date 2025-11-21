package com.tangem.feature.wallet.utils

import com.tangem.common.ui.userwallet.converter.ArtworkUMConverter
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.ui.components.artwork.ArtworkUM
import com.tangem.domain.models.scan.CardDTO
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.wallet.utils.UserWalletImageFetcher
import com.tangem.operations.attestation.ArtworkSize
import kotlinx.coroutines.flow.*
import javax.inject.Inject

class DefaultUserWalletImageFetcher @Inject constructor(
    private val getCardImageUseCase: GetCardImageUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
    private val artworkUMConverter: ArtworkUMConverter,
) : UserWalletImageFetcher {

    private val smallCache = MutableStateFlow(mapOf<String, ArtworkUM>())
    private val largeCache = MutableStateFlow(mapOf<String, ArtworkUM>())

    override fun allWallets(size: ArtworkSize): Flow<Map<UserWalletId, UserWalletItemUM.ImageState>> =
        getWalletsUseCase.invoke()
            .map { list -> list.map { it.walletId } }
            .distinctUntilChanged()
            .map { list -> list.map { walletId -> walletImage(walletId, size).map { image -> walletId to image } } }
            .flatMapLatest { flows -> combine(flows) { it.toMap() } }
            .distinctUntilChanged()

    override fun walletImage(wallet: UserWallet, size: ArtworkSize): Flow<UserWalletItemUM.ImageState> = when (wallet) {
        is UserWallet.Cold -> walletImage(wallet.scanResponse.card, size)
        is UserWallet.Hot -> flowOf(UserWalletItemUM.ImageState.MobileWallet)
    }

    override fun walletsImage(
        wallets: Collection<UserWallet>,
        size: ArtworkSize,
    ): Flow<Map<UserWalletId, UserWalletItemUM.ImageState>> = allWallets(size)
        .map { allWallets -> allWallets.filter { (walletId, _) -> wallets.any { it.walletId == walletId } } }
        .distinctUntilChanged()

    override fun walletImage(walletId: UserWalletId, size: ArtworkSize): Flow<UserWalletItemUM.ImageState> =
        getUserWalletUseCase.invokeFlow(walletId)
            .transform {
                it.fold(
                    ifLeft = { emit(UserWalletItemUM.ImageState.Loading) },
                    ifRight = { wallet -> emitAll(walletImage(wallet, size)) },
                )
            }
            .distinctUntilChanged()

    override fun walletImage(cardDTO: CardDTO, size: ArtworkSize): Flow<UserWalletItemUM.ImageState> =
        internalGetCardImage(
            cardInfo = cardDTO,
            size = size,
        ).distinctUntilChanged()

    private fun internalGetCardImage(cardInfo: CardDTO, size: ArtworkSize): Flow<UserWalletItemUM.ImageState> = flow {
        emit(cacheOrLoading(cardInfo.cardId, size))

        val artwork = getCardImageUseCase.invoke(
            cardId = cardInfo.cardId,
            cardPublicKey = cardInfo.cardPublicKey,
            size = size,
            manufacturerName = cardInfo.manufacturer.name,
            firmwareVersion = cardInfo.firmwareVersion.toSdkFirmwareVersion(),
        )
            .let { artworkUMConverter.convert(it) }
            .also { save(cardInfo.cardId, size, it) }
        emit(UserWalletItemUM.ImageState.Image(artwork))
    }

    private fun cacheOrLoading(cardId: String, size: ArtworkSize): UserWalletItemUM.ImageState {
        val artwork = when (size) {
            ArtworkSize.LARGE -> largeCache.value[cardId]
            ArtworkSize.SMALL -> smallCache.value[cardId]
        }
        return artwork
            ?.let { UserWalletItemUM.ImageState.Image(artwork) }
            ?: UserWalletItemUM.ImageState.Loading
    }

    private fun save(cardId: String, size: ArtworkSize, artwork: ArtworkUM) {
        when (size) {
            ArtworkSize.LARGE -> largeCache.update { it.plus(cardId to artwork) }
            ArtworkSize.SMALL -> smallCache.update { it.plus(cardId to artwork) }
        }
    }
}
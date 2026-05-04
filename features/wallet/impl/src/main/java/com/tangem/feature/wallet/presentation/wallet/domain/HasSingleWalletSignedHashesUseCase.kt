package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.card.repository.CardRepository
import com.tangem.domain.demo.models.DemoConfig
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.walletmanager.WalletManagersFacade
import com.tangem.utils.logging.TangemLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@ModelScoped
class HasSingleWalletSignedHashesUseCase @Inject constructor(
    private val cardRepository: CardRepository,
    private val walletManagersFacade: WalletManagersFacade,
) {

    operator fun invoke(userWallet: UserWallet.Cold, network: Network): Flow<Boolean> {
        return cardRepository.wasCardScanned(cardId = userWallet.cardId)
            .map { wasCardScanned ->
                if (wasCardScanned || !userWallet.isCorrectCardType()) return@map false

                if (!userWallet.scanResponse.cardTypesResolver.hasWalletSignedHashes()) {
                    cardRepository.setCardWasScanned(cardId = userWallet.cardId)
                    return@map false
                }

                return@map try {
                    walletManagersFacade.validateSignatureCount(
                        userWalletId = userWallet.walletId,
                        network = network,
                        signedHashes = userWallet.scanResponse.card.wallets.firstOrNull()?.totalSignedHashes ?: 0,
                    )
                        .fold(
                            ifLeft = { true },
                            ifRight = {
                                cardRepository.setCardWasScanned(cardId = userWallet.cardId)
                                false
                            },
                        )
                } catch (e: IllegalArgumentException) {
                    TangemLogger.w("Unable to validate signature count: user wallet not found", e)
                    false
                }
            }
    }

    private fun UserWallet.Cold.isCorrectCardType(): Boolean {
        return with(scanResponse.cardTypesResolver) {
            !DemoConfig.isDemoCardId(cardId) && isReleaseFirmwareType() && !isMultiwalletAllowed() && !isTangemTwins()
        }
    }
}
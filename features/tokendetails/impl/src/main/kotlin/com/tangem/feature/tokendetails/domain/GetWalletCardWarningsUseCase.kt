package com.tangem.feature.tokendetails.domain

import com.tangem.domain.card.IsWalletBackupProblematicUseCase
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.HasSingleWalletSignedHashesUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Produces card/wallet-level warnings for the Token Details screen.
 *
 * These banners mirror the ones the main screen shows via `GetWalletNotificationsFactory`, but they only apply to
 * a single-currency cold wallet (a multi-currency wallet keeps them on the main screen only).
 */
internal class GetWalletCardWarningsUseCase @Inject constructor(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase,
    private val isWalletBackupProblematicUseCase: IsWalletBackupProblematicUseCase,
    private val dispatchers: CoroutineDispatcherProvider,
) {

    operator fun invoke(userWallet: UserWallet, network: Network): Flow<Set<WalletCardWarning>> {
        if (userWallet !is UserWallet.Cold || userWallet.isMultiCurrency) {
            return flowOf(emptySet())
        }

        return hasSingleWalletSignedHashesUseCase(userWallet, network)
            .map { hasIncorrectSignedHashes ->
                buildWarnings(
                    userWallet = userWallet,
                    hasIncorrectSignedHashes = hasIncorrectSignedHashes,
                )
            }
            .flowOn(dispatchers.io)
    }

    private fun buildWarnings(userWallet: UserWallet.Cold, hasIncorrectSignedHashes: Boolean): Set<WalletCardWarning> {
        val cardTypesResolver = userWallet.cardTypesResolver

        return buildSet {
            if (isWalletBackupProblematicUseCase(userWallet)) {
                add(WalletCardWarning.BackupError)
            }
            if (!cardTypesResolver.isReleaseFirmwareType()) {
                add(WalletCardWarning.DevCard)
            }
            if (cardTypesResolver.isReleaseFirmwareType() && cardTypesResolver.isAttestationFailed()) {
                add(WalletCardWarning.FailedCardValidation)
            }
            cardTypesResolver.getRemainingSignatures()?.let { remainingSignatures ->
                if (remainingSignatures <= MAX_REMAINING_SIGNATURES_COUNT) {
                    add(WalletCardWarning.LowSignatures(count = remainingSignatures))
                }
            }
            if (isDemoCardUseCase(cardId = cardTypesResolver.getCardId())) {
                add(WalletCardWarning.DemoCard)
            }
            if (cardTypesResolver.isTestCard()) {
                add(WalletCardWarning.TestnetCard)
            }
            if (hasIncorrectSignedHashes) {
                add(WalletCardWarning.NumberOfSignedHashesIncorrect)
            }
        }
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
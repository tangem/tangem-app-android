package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.common.CardTypesResolver
import com.tangem.feature.wallet.presentation.wallet.state.WalletNotification
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * @author Andrew Khokhlov on 16/07/2023
 */
@Suppress("LongParameterList")
internal class WalletNotificationsListFactory(
    private val wasCardScannedCallback: suspend (String) -> Boolean,
    private val isUserAlreadyRateAppCallback: suspend () -> Boolean,
    private val onWalletAlreadySignedHashesClick: () -> Unit,
    private val onMultiWalletAlreadySignedHashesClick: () -> Unit,
    private val onBackupCardClick: () -> Unit,
    private val hasUnreachableNetworksCallback: () -> Boolean,
    private val onLikeTangemAppClick: () -> Unit,
) {

    fun create(cardTypesResolver: CardTypesResolver): Flow<ImmutableList<WalletNotification>> {
        // TODO: https://tangem.atlassian.net/browse/AND-4107 order
        return flow {
            emit(
                buildList {
                    if (cardTypesResolver.isTestCard()) {
                        add(element = WalletNotification.TestCard)
                        return@buildList
                    }

                    addRemainingSignaturesLeftNotifications(cardTypesResolver)

                    if (!cardTypesResolver.isReleaseFirmwareType()) {
                        add(element = WalletNotification.DevCard)
                    } else {
                        addReleaseSpecialNotifications(cardTypesResolver)
                    }

                    // TODO: https://tangem.atlassian.net/browse/AND-4107 - DemoModeDatasource

                    if (hasUnreachableNetworksCallback()) {
                        add(element = WalletNotification.UnreachableNetworks)
                    }

                    if (!cardTypesResolver.isBackupForbidden() && !cardTypesResolver.hasBackup()) {
                        add(element = WalletNotification.BackupCard(onClick = onBackupCardClick))
                    }

                    /* TODO: https://tangem.atlassian.net/browse/AND-4107
                     * List<WalletStoreModel>
                     *     .filter { store ->
                     *         store.walletsData.any { it.status is WalletDataModel.MissedDerivation }
                     *     }
                     */

                    if (isUserAlreadyRateAppCallback()) {
                        add(element = WalletNotification.LikeTangemApp(onClick = onLikeTangemAppClick))
                    }
                }.toImmutableList(),
            )
        }
    }

    private fun MutableList<WalletNotification>.addRemainingSignaturesLeftNotifications(
        cardTypesResolver: CardTypesResolver,
    ) {
        val remainingSignatures = cardTypesResolver.getRemainingSignatures()
        if (remainingSignatures != null && remainingSignatures <= MAX_REMAINING_SIGNATURES_COUNT) {
            add(element = WalletNotification.RemainingSignaturesLeft(remainingSignatures))
        }
    }

    private suspend fun MutableList<WalletNotification>.addReleaseSpecialNotifications(
        cardTypesResolver: CardTypesResolver,
    ) {
        // TODO: https://tangem.atlassian.net/browse/AND-4107  !isDemo
        if (!wasCardScannedCallback(cardTypesResolver.getCardId()) && cardTypesResolver.isMultiwalletAllowed()) {
            if (cardTypesResolver.isBackupForbidden() && cardTypesResolver.hasWalletSignedHashes()) {
                add(
                    element = WalletNotification.SignedTransactionsInThePast(
                        onClick = onMultiWalletAlreadySignedHashesClick,
                    ),
                )
            } else if (cardTypesResolver.hasWalletSignedHashes()) {
                add(
                    element = WalletNotification.AlreadyToppedUpAndSignedHashes(
                        onClick = onWalletAlreadySignedHashesClick,
                    ),
                )
            }
        }

        if (cardTypesResolver.isAttestationFailed()) {
            add(element = WalletNotification.CardVerificationFailed)
        }
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}

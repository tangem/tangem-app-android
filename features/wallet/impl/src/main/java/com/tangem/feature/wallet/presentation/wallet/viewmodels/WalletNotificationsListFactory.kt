package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.card.GetCardWasScannedUseCase
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.settings.IsUserAlreadyRateAppUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Wallet notifications list factory
 *
 * @property isDemoCardUseCase           use case that check if card is demo
 * @property isUserAlreadyRateAppUseCase use case that check if card is user already rate app
 * @property getCardWasScannedUseCase    use case that check if card was scanned
 * @property clickIntents                screen click intents
 *
 * @author Andrew Khokhlov on 16/07/2023
 */
internal class WalletNotificationsListFactory(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isUserAlreadyRateAppUseCase: IsUserAlreadyRateAppUseCase,
    private val getCardWasScannedUseCase: GetCardWasScannedUseCase,
    private val clickIntents: WalletClickIntents,
) {

    fun create(
        cardTypesResolver: CardTypesResolver,
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
    ): Flow<ImmutableList<WalletNotification>> {
        return flow {
            emit(
                buildList {
                    addCriticalNotifications(cardTypesResolver)

                    addMissingAddressesNotification(cryptoCurrencyList)

                    addRateTheAppNotification()

                    addWarningNotifications(cardTypesResolver, cryptoCurrencyList)
                }
                    .toImmutableList(),
            )
        }
    }

    private fun MutableList<WalletNotification>.addCriticalNotifications(cardTypesResolver: CardTypesResolver) {
        addIf(
            element = WalletNotification.Critical.DevCard,
            condition = !cardTypesResolver.isReleaseFirmwareType(),
        )

        addIf(
            element = WalletNotification.Critical.DemoCard,
            condition = isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )

        addIf(
            element = WalletNotification.Critical.TestNetCard,
            condition = cardTypesResolver.isTestCard(),
        )

        addIf(
            element = WalletNotification.Critical.FailedCardValidation,
            condition = cardTypesResolver.isReleaseFirmwareType() && cardTypesResolver.isAttestationFailed(),
        )

        cardTypesResolver.getRemainingSignatures()?.let { remainingSignatures ->
            addIf(
                element = WalletNotification.Critical.LowSignatures(count = remainingSignatures),
                condition = remainingSignatures <= MAX_REMAINING_SIGNATURES_COUNT,
            )
        }
    }

    private fun MutableList<WalletNotification>.addMissingAddressesNotification(
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
    ) {
        val missedAddressCurrencies = cryptoCurrencyList.getMissedAddressCurrencies()

        addIf(
            element = WalletNotification.MissingAddresses(
                missingAddressesCount = missedAddressCurrencies.count(),
                onGenerateClick = {
                    clickIntents.onGenerateMissedAddressesClick(missedAddressCurrencies = missedAddressCurrencies)
                },
            ),
            condition = missedAddressCurrencies.isNotEmpty(),
        )
    }

    private fun List<CryptoCurrencyStatus>.getMissedAddressCurrencies(): List<CryptoCurrency> {
        return this
            .filter { it.value is CryptoCurrencyStatus.MissedDerivation }
            .map(CryptoCurrencyStatus::currency)
    }

    // TODO: https://tangem.atlassian.net/browse/AND-4626
    private suspend fun MutableList<WalletNotification>.addRateTheAppNotification() {
        addIf(
            element = WalletNotification.RateApp(
                onPositiveClick = {},
                onNegativeClick = {},
                onCloseClick = {},
            ),
            condition = isUserAlreadyRateAppUseCase(),
        )
    }

    private suspend fun MutableList<WalletNotification>.addWarningNotifications(
        cardTypesResolver: CardTypesResolver,
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
    ) {
        addIf(
            element = WalletNotification.Warning.MissingBackup(
                onStartBackupClick = clickIntents::onBackupCardClick,
            ),
            condition = !cardTypesResolver.isBackupForbidden() && !cardTypesResolver.hasBackup(),
        )

        val isDemo = isDemoCardUseCase(cardId = cardTypesResolver.getCardId())
        if (cardTypesResolver.isMultiwalletAllowed()) {
            addIf(
                element = WalletNotification.Warning.SomeNetworksUnreachable,
                condition = cryptoCurrencyList.hasUnreachableNetworks(),
            )

            if (cardTypesResolver.isBackupForbidden()) {
                addIf(
                    element = WalletNotification.Warning.NumberOfSignedHashesIncorrect,
                    condition = checkSignedHashes(cardTypesResolver = cardTypesResolver, isDemo = isDemo),
                )
            } else {
                addIf(
                    element = WalletNotification.Warning.MultiWalletSignedHashesIncorrect(
                        onClick = clickIntents::onMultiWalletSignedHashesNotificationClick,
                    ),
                    condition = checkSignedHashes(cardTypesResolver = cardTypesResolver, isDemo = isDemo),
                )
            }
        } else {
            addIf(
                element = WalletNotification.Warning.NetworksUnreachable,
                condition = cryptoCurrencyList.hasUnreachableNetworks(),
            )

            // TODO: https://tangem.atlassian.net/browse/AND-4627
            addIf(
                element = WalletNotification.Warning.TopUpNote(
                    errorMessage = "To activate card top up it with at least 1 XLM",
                ),
                condition = cryptoCurrencyList.hasNoAccountStatus(),
            )

            addIf(
                element = WalletNotification.Warning.NumberOfSignedHashesIncorrect,
                condition = checkSignedHashes(cardTypesResolver, isDemo),
            )
        }
    }

    private fun MutableList<WalletNotification>.addIf(element: WalletNotification, condition: Boolean) {
        if (condition) add(element = element)
    }

    private fun List<CryptoCurrencyStatus>.hasUnreachableNetworks(): Boolean {
        return any { it.value is CryptoCurrencyStatus.Unreachable }
    }

    private fun List<CryptoCurrencyStatus>.hasNoAccountStatus(): Boolean {
        return any { it.value is CryptoCurrencyStatus.NoAccount }
    }

    private suspend fun checkSignedHashes(cardTypesResolver: CardTypesResolver, isDemo: Boolean): Boolean {
        return cardTypesResolver.isReleaseFirmwareType() && cardTypesResolver.hasWalletSignedHashes() &&
            !isDemo && !getCardWasScannedUseCase(cardId = cardTypesResolver.getCardId())
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}

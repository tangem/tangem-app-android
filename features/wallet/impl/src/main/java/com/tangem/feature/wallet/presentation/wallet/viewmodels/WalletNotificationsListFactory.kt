package com.tangem.feature.wallet.presentation.wallet.viewmodels

import com.tangem.domain.card.WasCardScannedUseCase
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlin.collections.count

/**
 * Wallet notifications list factory
 *
 * @property isDemoCardUseCase           use case that checks if card is demo
 * @property isReadyToShowRateAppUseCase use case that checks if card is user already rate app
 * @property wasCardScannedUseCase       use case that checks if card was scanned
 * @property isNeedToBackupUseCase       use case that checks if wallet need backup cards
 * @property clickIntents                screen click intents
 *
[REDACTED_AUTHOR]
 */
internal class WalletNotificationsListFactory(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val wasCardScannedUseCase: WasCardScannedUseCase,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val clickIntents: WalletClickIntents,
) {

    fun create(
        selectedWalletId: UserWalletId,
        cardTypesResolver: CardTypesResolver,
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
    ): Flow<ImmutableList<WalletNotification>> {
        return combine(
            flow = wasCardScannedUseCase(cardTypesResolver.getCardId()),
            flow2 = isReadyToShowRateAppUseCase(),
            flow3 = isNeedToBackupUseCase(selectedWalletId),
        ) { wasCardScanned, isReadyToShowRating, isNeedToBackup ->
            buildList {
                addCriticalNotifications(cardTypesResolver)

                addMissingAddressesNotification(cryptoCurrencyList)

                addRateTheAppNotification(isReadyToShowRating)

                addWarningNotifications(cardTypesResolver, cryptoCurrencyList, wasCardScanned, isNeedToBackup)
            }.toImmutableList()
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

    private fun MutableList<WalletNotification>.addRateTheAppNotification(isReadyToShowRating: Boolean) {
        addIf(
            element = WalletNotification.RateApp(
                onLikeClick = clickIntents::onLikeAppClick,
                onDislikeClick = clickIntents::onDislikeAppClick,
                onCloseClick = clickIntents::onCloseRateAppNotificationClick,
            ),
            condition = isReadyToShowRating,
        )
    }

    private fun MutableList<WalletNotification>.addWarningNotifications(
        cardTypesResolver: CardTypesResolver,
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
        wasCardScanned: Boolean,
        isNeedToBackup: Boolean,
    ) {
        addIf(
            element = WalletNotification.Warning.MissingBackup(
                onStartBackupClick = clickIntents::onBackupCardClick,
            ),
            condition = isNeedToBackup,
        )

        val isDemo = isDemoCardUseCase(cardId = cardTypesResolver.getCardId())
        if (cardTypesResolver.isMultiwalletAllowed()) {
            addIf(
                element = WalletNotification.Warning.SomeNetworksUnreachable,
                condition = cryptoCurrencyList.hasUnreachableNetworks(),
            )
        } else {
            addIf(
                element = WalletNotification.Warning.NetworksUnreachable,
                condition = cryptoCurrencyList.hasUnreachableNetworks(),
            )

            val errorMessage = cryptoCurrencyList.geNoAccountStatusMessage()
            if (errorMessage != null) {
                add(element = WalletNotification.Warning.TopUpNote(errorMessage = errorMessage))
            }

            addIf(
                element = WalletNotification.Warning.NumberOfSignedHashesIncorrect,
                condition = checkSignedHashes(cardTypesResolver, isDemo, wasCardScanned),
            )
        }
    }

    private fun MutableList<WalletNotification>.addIf(element: WalletNotification, condition: Boolean) {
        if (condition) add(element = element)
    }

    private fun List<CryptoCurrencyStatus>.hasUnreachableNetworks(): Boolean {
        return any { it.value is CryptoCurrencyStatus.Unreachable }
    }

    private fun List<CryptoCurrencyStatus>.geNoAccountStatusMessage(): String? {
        return this
            .map(CryptoCurrencyStatus::value)
            .filterIsInstance<CryptoCurrencyStatus.NoAccount>()
            .firstOrNull()
            ?.errorMessage
    }

    private fun checkSignedHashes(
        cardTypesResolver: CardTypesResolver,
        isDemo: Boolean,
        wasCardScanned: Boolean,
    ): Boolean {
        return cardTypesResolver.isReleaseFirmwareType() && cardTypesResolver.hasWalletSignedHashes() && !isDemo &&
            !wasCardScanned
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
package com.tangem.feature.wallet.presentation.wallet.viewmodels

import arrow.core.Either
import com.tangem.domain.card.WasCardScannedUseCase
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.tokens.GetMissedAddressesCryptoCurrenciesUseCase
import com.tangem.domain.tokens.error.GetCurrenciesError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate

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
    private val getMissedAddressCryptoCurrenciesUseCase: GetMissedAddressesCryptoCurrenciesUseCase,
    private val clickIntents: WalletClickIntents,
) {

    private var readyForRateAppNotification = false

    fun create(
        selectedWalletId: UserWalletId,
        cardTypesResolver: CardTypesResolver,
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
    ): Flow<ImmutableList<WalletNotification>> {
        return combine(
            flow = wasCardScannedUseCase(cardTypesResolver.getCardId()).conflate(),
            flow2 = isReadyToShowRateAppUseCase().conflate(),
            flow3 = isNeedToBackupUseCase(selectedWalletId).conflate(),
            flow4 = getMissedAddressCryptoCurrenciesUseCase(selectedWalletId).conflate(),
        ) { wasCardScanned, isReadyToShowRating, isNeedToBackup, maybeMissedAddressCurrencies ->
            readyForRateAppNotification = true
            buildList {
                addCriticalNotifications(cardTypesResolver)

                addInformationalNotifications(cardTypesResolver, maybeMissedAddressCurrencies)

                addWarningNotifications(cardTypesResolver, cryptoCurrencyList, wasCardScanned, isNeedToBackup)

                addRateTheAppNotification(isReadyToShowRating)
            }.toImmutableList()
        }
    }

    private fun MutableList<WalletNotification>.addCriticalNotifications(cardTypesResolver: CardTypesResolver) {
        addIf(
            element = WalletNotification.Critical.DevCard,
            condition = !cardTypesResolver.isReleaseFirmwareType(),
        )

        addIf(
            element = WalletNotification.Critical.FailedCardValidation,
            condition = cardTypesResolver.isReleaseFirmwareType() && cardTypesResolver.isAttestationFailed(),
        )

        cardTypesResolver.getRemainingSignatures()?.let { remainingSignatures ->
            addIf(
                element = WalletNotification.Warning.LowSignatures(count = remainingSignatures),
                condition = remainingSignatures <= MAX_REMAINING_SIGNATURES_COUNT,
            )
        }
    }

    private fun MutableList<WalletNotification>.addRateTheAppNotification(isReadyToShowRating: Boolean) {
        addIf(
            element = WalletNotification.RateApp(
                onLikeClick = clickIntents::onLikeAppClick,
                onDislikeClick = clickIntents::onDislikeAppClick,
                onCloseClick = clickIntents::onCloseRateAppNotificationClick,
            ),
            condition = isReadyToShowRating && readyForRateAppNotification,
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

        addIf(
            element = WalletNotification.Warning.TestNetCard,
            condition = cardTypesResolver.isTestCard(),
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

            addNoAccountWarning(cryptoCurrencyList)

            addIf(
                element = WalletNotification.Warning.NumberOfSignedHashesIncorrect(
                    onCloseClick = clickIntents::onSignedHashesNotificationCloseClick,
                ),
                condition = checkSignedHashes(
                    cardTypesResolver = cardTypesResolver,
                    isDemo = isDemo,
                    wasCardScanned = wasCardScanned,
                ),
            )
        }
    }

    private fun MutableList<WalletNotification>.addInformationalNotifications(
        cardTypesResolver: CardTypesResolver,
        maybeMissedAddressCurrencies: Either<GetCurrenciesError, List<CryptoCurrency>>,
    ) {
        addIf(
            element = WalletNotification.Informational.DemoCard,
            condition = isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )

        addMissingAddressesNotification(maybeMissedAddressCurrencies)
    }

    private fun MutableList<WalletNotification>.addIf(element: WalletNotification, condition: Boolean) {
        if (condition) {
            add(element = element)
            if (element is WalletNotification.Critical || element is WalletNotification.Warning) {
                readyForRateAppNotification = false
            }
        }
    }

    private fun MutableList<WalletNotification>.addMissingAddressesNotification(
        maybeCurrencies: Either<GetCurrenciesError, List<CryptoCurrency>>,
    ) {
        val missingAddressCurrencies = (maybeCurrencies as? Either.Right)?.value ?: return

        addIf(
            element = WalletNotification.Informational.MissingAddresses(
                missingAddressesCount = missingAddressCurrencies.count(),
                onGenerateClick = {
                    clickIntents.onGenerateMissedAddressesClick(missedAddressCurrencies = missingAddressCurrencies)
                },
            ),
            condition = missingAddressCurrencies.isNotEmpty(),
        )
    }

    private fun List<CryptoCurrencyStatus>.hasUnreachableNetworks(): Boolean {
        return any { it.value is CryptoCurrencyStatus.Unreachable }
    }

    private fun MutableList<WalletNotification>.addNoAccountWarning(cryptoCurrencyList: List<CryptoCurrencyStatus>) {
        val noAccountNetwork = cryptoCurrencyList.firstOrNull { it.value is CryptoCurrencyStatus.NoAccount }
        if (noAccountNetwork != null) {
            val amountToCreateAccount = (noAccountNetwork.value as? CryptoCurrencyStatus.NoAccount)
                ?.amountToCreateAccount.toString()
            add(
                element = WalletNotification.Informational.NoAccount(
                    network = noAccountNetwork.currency.name,
                    amount = amountToCreateAccount,
                    symbol = noAccountNetwork.currency.symbol,
                ),
            )
        }
    }

    /**
     * Warning is being shown for single wallet cards only
     */
    private fun checkSignedHashes(
        cardTypesResolver: CardTypesResolver,
        isDemo: Boolean,
        wasCardScanned: Boolean,
    ): Boolean {
        return cardTypesResolver.isReleaseFirmwareType() && !cardTypesResolver.isTangemTwins() &&
            cardTypesResolver.hasWalletSignedHashes() && !isDemo && !wasCardScanned
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
package com.tangem.feature.wallet.presentation.wallet.viewmodels

import arrow.core.Either
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.settings.ShouldShowSwapPromoWalletUseCase
import com.tangem.domain.tokens.GetMissedAddressesCryptoCurrenciesUseCase
import com.tangem.domain.tokens.error.GetCurrenciesError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.presentation.wallet.domain.HasSingleWalletSignedHashesUseCase
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOf

/**
 * Wallet notifications list factory
 *
 * @property isDemoCardUseCase           use case that checks if card is demo
 * @property isReadyToShowRateAppUseCase use case that checks if card is user already rate app
 * @property isNeedToBackupUseCase       use case that checks if wallet need backup cards
 * @property getMissedAddressCryptoCurrenciesUseCase use case that gets missed address crypto currencies
 * @property hasSingleWalletSignedHashesUseCase use case that checks if single wallet signed hashes
 * @property shouldShowSwapPromoWalletUseCase use case that checks if should show swap promo
 * @property clickIntents                screen click intents
 *
[REDACTED_AUTHOR]
 */
@Suppress("LongParameterList")
internal class WalletNotificationsListFactory(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val getMissedAddressCryptoCurrenciesUseCase: GetMissedAddressesCryptoCurrenciesUseCase,
    private val hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase,
    private val shouldShowSwapPromoWalletUseCase: ShouldShowSwapPromoWalletUseCase,
    private val clickIntents: WalletClickIntents,
) {

    private var readyForRateAppNotification = false

    fun create(
        selectedWallet: UserWallet,
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
    ): Flow<ImmutableList<WalletNotification>> {
        val cardTypesResolver = selectedWallet.scanResponse.cardTypesResolver
        return combine(
            flow = hasSingleWalletSignedHashesFlow(selectedWallet, cryptoCurrencyList),
            flow2 = isReadyToShowRateAppUseCase().conflate(),
            flow3 = isNeedToBackupUseCase(selectedWallet.walletId).conflate(),
            flow4 = getMissedAddressCryptoCurrenciesUseCase(selectedWallet.walletId).conflate(),
            flow5 = shouldShowSwapPromoWalletUseCase().conflate(),
        ) { hasSignedHashes, isReadyToShowRating, isNeedToBackup, maybeMissedAddressCurrencies, isShowSwapPromo ->
            readyForRateAppNotification = true
            buildList {
                addSwapPromoNotification(isShowSwapPromo, cardTypesResolver)

                addCriticalNotifications(cardTypesResolver)

                addInformationalNotifications(cardTypesResolver, maybeMissedAddressCurrencies)

                addWarningNotifications(cardTypesResolver, cryptoCurrencyList, hasSignedHashes, isNeedToBackup)

                addRateTheAppNotification(isReadyToShowRating)
            }.toImmutableList()
        }
    }

    private fun hasSingleWalletSignedHashesFlow(
        selectedWallet: UserWallet,
        cryptoCurrencyList: List<CryptoCurrencyStatus>,
    ): Flow<Boolean> {
        return if (selectedWallet.scanResponse.cardTypesResolver.isMultiwalletAllowed()) {
            flowOf(value = false)
        } else {
            val network = requireNotNull(cryptoCurrencyList.firstOrNull()?.currency?.network)
            hasSingleWalletSignedHashesUseCase(userWallet = selectedWallet, network = network).conflate()
        }
    }

    private fun MutableList<WalletNotification>.addSwapPromoNotification(
        showSwapPromo: Boolean,
        cardTypesResolver: CardTypesResolver,
    ) {
        addIf(
            element = WalletNotification.SwapPromo(
                clickIntents::onCloseSwapPromoNotificationClick,
            ),
            condition = showSwapPromo && cardTypesResolver.isMultiwalletAllowed(),
        )
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
        hasSignedHashes: Boolean,
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
                condition = hasSignedHashes,
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

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
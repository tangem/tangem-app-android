package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.ds.message.TangemMessageEffect
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.hotwallet.GetAccessCodeSkippedUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isMultiCurrency
import com.tangem.domain.wallets.models.SeedPhraseNotificationsStatus
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.domain.wallets.usecase.SeedPhraseNotificationUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotificationUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.extensions.addIf
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * Factory for creating a list of notifications that can be shown on the wallet screen.
 * These notifications are critical and should be shown separately from each other.
 */
@Suppress("LongParameterList")
@ModelScoped
internal class GetWalletNotificationsFactory @Inject constructor(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val backupValidator: BackupValidator,
    private val seedPhraseNotificationUseCase: SeedPhraseNotificationUseCase,
    private val accountDependencies: AccountDependencies,
    private val getAccessCodeSkippedUseCase: GetAccessCodeSkippedUseCase,
    private val hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase,
) {
    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotificationUM>> {
        val cardTypesResolver = (userWallet as? UserWallet.Cold)?.scanResponse?.cardTypesResolver

        val params = SingleAccountStatusListProducer.Params(userWallet.walletId)
        val accountStatusListFlow = accountDependencies.singleAccountStatusListSupplier(params)

        return combine(
            flow = accountStatusListFlow,
            flow2 = isNeedToBackupUseCase(userWallet.walletId).distinctUntilChanged(),
            flow3 = seedPhraseNotificationUseCase(userWalletId = userWallet.walletId).distinctUntilChanged(),
            flow4 = getAccessCodeSkippedUseCase(userWallet.walletId).distinctUntilChanged(),
        ) { accountList, isNeedToBackup, seedPhraseIssueStatus, shouldAccessCodeSkipped ->
            val totalFiatBalance = accountList.totalFiatBalance
            val flattenCurrencies = accountList.flattenCurrencies()

            buildList {
                addUsedOutdatedDataNotification(totalFiatBalance)

                addCriticalNotifications(userWallet, seedPhraseIssueStatus, clickIntents)

                addFinishWalletActivationNotification(
                    userWallet = userWallet,
                    totalFiatBalance = totalFiatBalance,
                    clickIntents = clickIntents,
                    shouldAccessCodeSkipped = shouldAccessCodeSkipped,
                )

                addInformationalNotifications(
                    userWallet = userWallet,
                    cardTypesResolver = cardTypesResolver,
                    flattenCurrencies = flattenCurrencies,
                    clickIntents = clickIntents,
                )

                addWarningNotifications(
                    userWallet = userWallet,
                    cardTypesResolver = cardTypesResolver,
                    flattenCurrencies = flattenCurrencies,
                    isNeedToBackup = isNeedToBackup,
                    clickIntents = clickIntents,
                )
            }.sortedBy { it.type.ordinal }.toImmutableList()
        }
    }

    private fun MutableList<WalletNotificationUM>.addUsedOutdatedDataNotification(totalFiatBalance: TotalFiatBalance) {
        addIf(
            element = WalletNotificationUM.UsedOutdatedData,
            condition = (totalFiatBalance as? TotalFiatBalance.Loaded)?.source == StatusSource.ONLY_CACHE,
        )
    }

    private fun MutableList<WalletNotificationUM>.addCriticalNotifications(
        userWallet: UserWallet,
        seedPhraseIssueStatus: SeedPhraseNotificationsStatus,
        clickIntents: WalletClickIntents,
    ) {
        if (userWallet !is UserWallet.Cold) {
            return
        }

        addSeedNotificationIfNeeded(userWallet, seedPhraseIssueStatus, clickIntents)

        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver
        addIf(
            element = WalletNotificationUM.BackupError { clickIntents.onSupportClick() },
            condition = !backupValidator.isValidBackupStatus(userWallet.scanResponse.card) || userWallet.hasBackupError,
        )

        addIf(
            element = WalletNotificationUM.DevCard,
            condition = !cardTypesResolver.isReleaseFirmwareType(),
        )

        addIf(
            element = WalletNotificationUM.FailedCardValidation,
            condition = cardTypesResolver.isReleaseFirmwareType() && cardTypesResolver.isAttestationFailed(),
        )

        cardTypesResolver.getRemainingSignatures()?.let { remainingSignatures ->
            addIf(
                element = WalletNotificationUM.LowSignatures(count = remainingSignatures),
                condition = remainingSignatures <= MAX_REMAINING_SIGNATURES_COUNT,
            )
        }
    }

    private fun MutableList<WalletNotificationUM>.addInformationalNotifications(
        userWallet: UserWallet,
        cardTypesResolver: CardTypesResolver?,
        flattenCurrencies: List<CryptoCurrencyStatus>,
        clickIntents: WalletClickIntents,
    ) {
        addIf(
            element = WalletNotificationUM.DemoCard,
            condition = cardTypesResolver != null && isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )

        addMissingAddressesNotification(userWallet, flattenCurrencies, clickIntents)
    }

    private fun MutableList<WalletNotificationUM>.addMissingAddressesNotification(
        userWallet: UserWallet,
        flattenCurrencies: List<CryptoCurrencyStatus>,
        clickIntents: WalletClickIntents,
    ) {
        val currencies = flattenCurrencies.getMissingAddressCurrencies().ifEmpty { return }

        addIf(
            element = WalletNotificationUM.MissingAddresses(
                tangemIcon = walletInterationIcon(userWallet),
                missingAddressesCount = currencies.count(),
                onGenerateClick = {
                    clickIntents.onGenerateMissedAddressesClick(missedAddressCurrencies = currencies)
                },
            ),
            condition = currencies.isNotEmpty(),
        )
    }

    private fun List<CryptoCurrencyStatus>.getMissingAddressCurrencies(): List<CryptoCurrency> {
        return this
            .filter { it.value is CryptoCurrencyStatus.MissedDerivation }
            .map(CryptoCurrencyStatus::currency)
    }

    private suspend fun MutableList<WalletNotificationUM>.addWarningNotifications(
        userWallet: UserWallet,
        cardTypesResolver: CardTypesResolver?,
        flattenCurrencies: List<CryptoCurrencyStatus>,
        isNeedToBackup: Boolean,
        clickIntents: WalletClickIntents,
    ) {
        addIf(
            element = WalletNotificationUM.MissingBackup(
                onClick = clickIntents::onAddBackupCardClick,
            ),
            condition = isNeedToBackup,
        )

        addIf(
            element = WalletNotificationUM.TestnetCard,
            condition = cardTypesResolver?.isTestCard() == true,
        )

        addIf(
            element = WalletNotificationUM.SomeNetworksUnreachable,
            condition = flattenCurrencies.hasUnreachableNetworks(),
        )

        addCloreMigrationNotification(userWallet, flattenCurrencies, clickIntents)

        addNoAccountWarning(cryptoCurrencyStatus = flattenCurrencies.firstOrNull())

        addIf(
            element = WalletNotificationUM.NumberOfSignedHashesIncorrect(
                onCloseClick = clickIntents::onCloseAlreadySignedHashesWarningClick,
            ),
            condition = hasSignedHashes(userWallet, flattenCurrencies.firstOrNull()),
        )
    }

    private fun MutableList<WalletNotificationUM>.addNoAccountWarning(cryptoCurrencyStatus: CryptoCurrencyStatus?) {
        val noAccountStatus = cryptoCurrencyStatus?.value as? CryptoCurrencyStatus.NoAccount
        if (noAccountStatus != null) {
            add(
                element = WalletNotificationUM.NoAccount(
                    network = cryptoCurrencyStatus.currency.name,
                    amount = noAccountStatus.amountToCreateAccount.toString(),
                    symbol = cryptoCurrencyStatus.currency.symbol,
                ),
            )
        }
    }

    private fun MutableList<WalletNotificationUM>.addCloreMigrationNotification(
        userWallet: UserWallet,
        flattenCurrencies: List<CryptoCurrencyStatus>,
        clickIntents: WalletClickIntents,
    ) {
        val cloreCurrency = flattenCurrencies.findCloreCurrency() ?: return

        addIf(
            condition = userWallet.isMultiCurrency,
            element = WalletNotificationUM.CloreMigration(
                onStartMigrationClick = { clickIntents.onCloreMigrationClick(cloreCurrency) },
            ),
        )
    }

    private fun List<CryptoCurrencyStatus>.findCloreCurrency(): CryptoCurrencyStatus? {
        return find { currencyStatus ->
            BlockchainUtils.isClore(currencyStatus.currency.network.rawId)
        }
    }

    private fun List<CryptoCurrencyStatus>.hasUnreachableNetworks(): Boolean {
        return any { it.value is CryptoCurrencyStatus.Unreachable }
    }

    private fun MutableList<WalletNotificationUM>.addFinishWalletActivationNotification(
        userWallet: UserWallet,
        totalFiatBalance: TotalFiatBalance,
        clickIntents: WalletClickIntents,
        shouldAccessCodeSkipped: Boolean,
    ) {
        if (userWallet !is UserWallet.Hot) return

        val isBackupExists = userWallet.backedUp
        val isAccessCodeRequired = userWallet.hotWalletId.authType == HotWalletId.AuthType.NoPassword &&
            !shouldAccessCodeSkipped
        val shouldShowFinishActivation = !isBackupExists || isAccessCodeRequired

        val messageEffect = when (totalFiatBalance) {
            TotalFiatBalance.Failed,
            TotalFiatBalance.Loading,
            -> TangemMessageEffect.None
            is TotalFiatBalance.Loaded -> if (totalFiatBalance.amount.orZero().isPositive()) {
                TangemMessageEffect.Warning
            } else {
                TangemMessageEffect.None
            }
        }

        addIf(
            element = WalletNotificationUM.FinishWalletActivation(
                messageEffect = messageEffect,
                onClick = { clickIntents.onFinishWalletActivationClick(isBackupExists) },
                isBackupExists = isBackupExists,
            ),
            condition = shouldShowFinishActivation,
        )
    }

    private fun MutableList<WalletNotificationUM>.addSeedNotificationIfNeeded(
        userWallet: UserWallet.Cold,
        seedPhraseIssueStatus: SeedPhraseNotificationsStatus,
        clickIntents: WalletClickIntents,
    ) {
        val isNotificationAvailable = with(userWallet) {
            val isDemo = isDemoCardUseCase(cardId = userWallet.cardId)
            val isWalletWithSeedPhrase = scanResponse.cardTypesResolver.isWallet2() && userWallet.isImported

            !isDemo && isWalletWithSeedPhrase
        }

        when (seedPhraseIssueStatus) {
            SeedPhraseNotificationsStatus.SHOW_FIRST -> addIf(
                element = WalletNotificationUM.SeedPhraseNotification(
                    onDeclineClick = clickIntents::onSeedPhraseNotificationDecline,
                    onConfirmClick = clickIntents::onSeedPhraseNotificationConfirm,
                ),
                condition = isNotificationAvailable,
            )
            SeedPhraseNotificationsStatus.SHOW_SECOND -> addIf(
                element = WalletNotificationUM.SeedPhraseSecondNotification(
                    onDeclineClick = clickIntents::onSeedPhraseSecondNotificationReject,
                    onConfirmClick = clickIntents::onSeedPhraseSecondNotificationAccept,
                ),
                condition = isNotificationAvailable,
            )
            SeedPhraseNotificationsStatus.NOT_NEEDED -> Unit
        }
    }

    private suspend fun hasSignedHashes(
        selectedWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus?,
    ): Boolean {
        if (selectedWallet !is UserWallet.Cold || !selectedWallet.isMultiCurrency) return false
        val network = cryptoCurrencyStatus?.currency?.network ?: return false

        return hasSingleWalletSignedHashesUseCase(userWallet = selectedWallet, network = network)
            .conflate()
            .distinctUntilChanged()
            .firstOrNull() == true
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
@file:Suppress("MaximumLineLength")

package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.common.ui.notifications.NotificationId
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.components.notifications.NotificationConfig.ButtonsState
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.account.status.producer.SingleAccountStatusListProducer
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.hotwallet.CheckHotWalletUpgradeBannerUseCase
import com.tangem.domain.hotwallet.GetAccessCodeSkippedUseCase
import com.tangem.domain.hotwallet.GetUpgradeBannerClosureTimestampUseCase
import com.tangem.domain.hotwallet.ShouldShowUpgradeHotWalletBannerUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.promo.ShouldShowPromoWalletUseCase
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.wallets.models.SeedPhraseNotificationsStatus
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.domain.wallets.usecase.SeedPhraseNotificationUseCase
import com.tangem.feature.wallet.child.wallet.model.WalletActivationBannerType
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.account.AccountDependencies
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.lib.crypto.BlockchainUtils
import com.tangem.utils.extensions.addIf
import com.tangem.utils.extensions.isPositive
import com.tangem.utils.extensions.orZero
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

import javax.inject.Inject

@Deprecated("Remove with main toggle [DesignFeatureToggles.isRedesignEnabled]")
@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class GetMultiWalletWarningsFactory @Inject constructor(
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val backupValidator: BackupValidator,
    private val seedPhraseNotificationUseCase: SeedPhraseNotificationUseCase,
    private val shouldShowPromoWalletUseCase: ShouldShowPromoWalletUseCase,
    private val notificationsRepository: NotificationsRepository,
    private val accountDependencies: AccountDependencies,
    private val getAccessCodeSkippedUseCase: GetAccessCodeSkippedUseCase,
    private val shouldShowUpgradeHotWalletBannerUseCase: ShouldShowUpgradeHotWalletBannerUseCase,
    private val getUpgradeBannerClosureTimestampUseCase: GetUpgradeBannerClosureTimestampUseCase,
    private val checkHotWalletUpgradeBannerUseCase: CheckHotWalletUpgradeBannerUseCase,
) {

    @Suppress("UNCHECKED_CAST", "MagicNumber", "LongMethod", "CastNullableToNonNullableType")
    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotification>> {
        val cardTypesResolver = (userWallet as? UserWallet.Cold)?.scanResponse?.cardTypesResolver

        val accountStatusListFlow by lazy {
            val params = SingleAccountStatusListProducer.Params(userWallet.walletId)
            accountDependencies.singleAccountStatusListSupplier(params)
                .map { it.totalFiatBalance to it.flattenCurrencies() }
                .map { Lce.Content(it) }
        }

        return combine(
            accountStatusListFlow,
            isReadyToShowRateAppUseCase().distinctUntilChanged(),
            isNeedToBackupUseCase(userWallet.walletId).distinctUntilChanged(),
            seedPhraseNotificationUseCase(userWalletId = userWallet.walletId).distinctUntilChanged(),
            shouldShowPromoWalletUseCase(userWalletId = userWallet.walletId, promoId = PromoId.OnePlusOne)
                .distinctUntilChanged(),
            notificationsRepository.getShouldShowNotification(NotificationId.EnablePushesReminderNotification.key)
                .distinctUntilChanged(),
            getAccessCodeSkippedUseCase(userWallet.walletId).distinctUntilChanged(),
            shouldShowPromoWalletUseCase(userWalletId = userWallet.walletId, promoId = PromoId.YieldPromo)
                .distinctUntilChanged(),
            shouldShowUpgradeHotWalletBannerUseCase.invoke(userWallet.walletId)
                .distinctUntilChanged(),
            getUpgradeBannerClosureTimestampUseCase(userWallet.walletId)
                .distinctUntilChanged(),
        ) { array -> array }
            .map { array ->
                val lceTokens = array[0] as Lce<TokenListError, Pair<TotalFiatBalance, List<CryptoCurrencyStatus>>>
                val totalFiatBalance = lceTokens.map { it.first }
                val flattenCurrencies = lceTokens.map { it.second }
                val isReadyToShowRating = array[1] as Boolean
                val isNeedToBackup = array[2] as Boolean
                val seedPhraseIssueStatus = array[3] as SeedPhraseNotificationsStatus
                val shouldShowOnePlusOnePromo = array[4] as Boolean
                val shouldShowEnablePushesReminderNotification = array[5] as Boolean
                val shouldAccessCodeSkipped = array[6] as Boolean
                val shouldShowYieldPromo = array[7] as Boolean
                val shouldShowUpgradeBanner = array[8] as Boolean
                val closureTimestamp = array[9] as? Long

                buildList {
                    addUsedOutdatedDataNotification(totalFiatBalance)

                    addCriticalNotifications(userWallet, seedPhraseIssueStatus, clickIntents)

                    addUpgradeHotWalletPromoNotification(
                        userWallet = userWallet,
                        flattenCurrencies = flattenCurrencies,
                        clickIntents = clickIntents,
                        shouldShowUpgradeBanner = shouldShowUpgradeBanner,
                        closureTimestamp = closureTimestamp,
                    )

                    addFinishWalletActivationNotification(
                        userWallet = userWallet,
                        flattenCurrencies = flattenCurrencies,
                        clickIntents = clickIntents,
                        shouldAccessCodeSkipped = shouldAccessCodeSkipped,
                    )

                    addOnePlusOnePromoNotification(clickIntents, shouldShowOnePlusOnePromo)

                    addYieldPromoNotification(clickIntents, shouldShowYieldPromo)

                    addInformationalNotifications(
                        userWallet = userWallet,
                        cardTypesResolver = cardTypesResolver,
                        flattenCurrencies = flattenCurrencies,
                        clickIntents = clickIntents,
                    )

                    addWarningNotifications(
                        cardTypesResolver = cardTypesResolver,
                        flattenCurrencies = flattenCurrencies,
                        isNeedToBackup = isNeedToBackup,
                        clickIntents = clickIntents,
                    )

                    addPushReminderNotification(
                        clickIntents = clickIntents,
                        shouldShowPushReminderBanner = shouldShowEnablePushesReminderNotification &&
                            !notificationsRepository.isUserAllowToSubscribeOnPushNotifications(),
                    )

                    // Remove in first iteration of yield supply feature
                    // addYieldSupplyNotifications(flattenCurrencies)

                    val hasCriticalOrWarning = any { notification ->
                        notification is WalletNotification.Critical || notification is WalletNotification.Warning
                    }

                    if (!hasCriticalOrWarning) {
                        addRateTheAppNotification(isReadyToShowRating, clickIntents)
                    }
                }.toImmutableList()
            }
    }

    private fun MutableList<WalletNotification>.addUsedOutdatedDataNotification(
        totalFiatBalance: Lce<TokenListError, TotalFiatBalance>,
    ) {
        addIf(
            element = WalletNotification.UsedOutdatedData,
            condition = totalFiatBalance.fold(
                ifLoading = {
                    (it as? TotalFiatBalance.Loaded)?.source == StatusSource.ONLY_CACHE
                },
                ifContent = {
                    (it as? TotalFiatBalance.Loaded)?.source == StatusSource.ONLY_CACHE
                },
                ifError = { false },
            ),
        )
    }

    private fun MutableList<WalletNotification>.addCriticalNotifications(
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
            element = WalletNotification.Critical.BackupError { clickIntents.onBackupErrorClick() },
            condition = !backupValidator.isValidBackupStatus(userWallet.scanResponse.card) || userWallet.hasBackupError,
        )

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

    private fun MutableList<WalletNotification>.addSeedNotificationIfNeeded(
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
                element = WalletNotification.Critical.SeedPhraseNotification(
                    onDeclineClick = clickIntents::onSeedPhraseNotificationDecline,
                    onConfirmClick = clickIntents::onSeedPhraseNotificationConfirm,
                ),
                condition = isNotificationAvailable,
            )
            SeedPhraseNotificationsStatus.SHOW_SECOND -> addIf(
                element = WalletNotification.Critical.SeedPhraseSecondNotification(
                    onDeclineClick = clickIntents::onSeedPhraseSecondNotificationReject,
                    onConfirmClick = clickIntents::onSeedPhraseSecondNotificationAccept,
                ),
                condition = isNotificationAvailable,
            )
            SeedPhraseNotificationsStatus.NOT_NEEDED -> {
                // do nothing
            }
        }
    }

    private fun MutableList<WalletNotification>.addInformationalNotifications(
        userWallet: UserWallet,
        cardTypesResolver: CardTypesResolver?,
        flattenCurrencies: Lce<TokenListError, List<CryptoCurrencyStatus>>,
        clickIntents: WalletClickIntents,
    ) {
        addIf(
            element = WalletNotification.Informational.DemoCard,
            condition = cardTypesResolver != null && isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )

        addMissingAddressesNotification(userWallet, flattenCurrencies, clickIntents)
    }

    private fun MutableList<WalletNotification>.addMissingAddressesNotification(
        userWallet: UserWallet,
        flattenCurrencies: Lce<TokenListError, List<CryptoCurrencyStatus>>,
        clickIntents: WalletClickIntents,
    ) {
        val currencies = flattenCurrencies.getMissingAddressCurrencies()
            .ifEmpty { return }

        addIf(
            element = WalletNotification.Informational.MissingAddresses(
                tangemIcon = walletInterationIcon(userWallet),
                missingAddressesCount = currencies.count(),
                onGenerateClick = {
                    clickIntents.onGenerateMissedAddressesClick(missedAddressCurrencies = currencies)
                },
            ),
            condition = currencies.isNotEmpty(),
        )
    }

    private fun Lce<TokenListError, List<CryptoCurrencyStatus>>.getMissingAddressCurrencies(): List<CryptoCurrency> {
        val flattenCurrencies = getOrNull(isPartialContentAccepted = true) ?: return emptyList()

        return flattenCurrencies
            .filter { it.value is CryptoCurrencyStatus.MissedDerivation }
            .map(CryptoCurrencyStatus::currency)
    }

    private fun MutableList<WalletNotification>.addOnePlusOnePromoNotification(
        clickIntents: WalletClickIntents,
        shouldShowPromo: Boolean,
    ) {
        addIf(
            element = WalletNotification.OnePlusOnePromo(
                onCloseClick = { clickIntents.onClosePromoClick(promoId = PromoId.OnePlusOne) },
                onClick = { clickIntents.onPromoClick(promoId = PromoId.OnePlusOne) },
            ),
            condition = shouldShowPromo,
        )
    }

    private fun MutableList<WalletNotification>.addYieldPromoNotification(
        clickIntents: WalletClickIntents,
        shouldShowPromo: Boolean,
    ) {
        addIf(
            element = WalletNotification.YieldPromo(
                onCloseClick = { clickIntents.onClosePromoClick(promoId = PromoId.YieldPromo) },
                onTermsAndConditionsClick = { clickIntents.onYieldPromoTermsAndConditionsClick() },
            ),
            condition = shouldShowPromo,
        )
    }

    // private fun MutableList<WalletNotification>.addYieldSupplyNotifications(
    //     flattenCurrencies: Lce<TokenListError, List<CryptoCurrencyStatus>>,
    // ) {
    //     addIf(
    //         element = WalletNotification.Warning.YeildSupplyApprove,
    //         condition = flattenCurrencies.hasTokensWithActivatedSupplyWithoutApprove(),
    //     )
    // }

    private fun MutableList<WalletNotification>.addWarningNotifications(
        cardTypesResolver: CardTypesResolver?,
        flattenCurrencies: Lce<TokenListError, List<CryptoCurrencyStatus>>,
        isNeedToBackup: Boolean,
        clickIntents: WalletClickIntents,
    ) {
        addIf(
            element = WalletNotification.Warning.MissingBackup(
                onStartBackupClick = clickIntents::onAddBackupCardClick,
            ),
            condition = isNeedToBackup,
        )

        addIf(
            element = WalletNotification.Warning.TestNetCard,
            condition = cardTypesResolver?.isTestCard() == true,
        )

        addIf(
            element = WalletNotification.Warning.SomeNetworksUnreachable,
            condition = flattenCurrencies.hasUnreachableNetworks(),
        )

        addCloreMigrationNotification(flattenCurrencies, clickIntents)
    }

    private fun MutableList<WalletNotification>.addCloreMigrationNotification(
        flattenCurrencies: Lce<TokenListError, List<CryptoCurrencyStatus>>,
        clickIntents: WalletClickIntents,
    ) {
        val cloreCurrency = flattenCurrencies.findCloreCurrency() ?: return

        add(
            WalletNotification.CloreMigration(
                onStartMigrationClick = { clickIntents.onCloreMigrationClick(cloreCurrency) },
            ),
        )
    }

    private fun Lce<TokenListError, List<CryptoCurrencyStatus>>.findCloreCurrency(): CryptoCurrencyStatus? {
        val currencies = getOrNull(isPartialContentAccepted = true) ?: return null

        return currencies.find { currencyStatus ->
            BlockchainUtils.isClore(currencyStatus.currency.network.rawId)
        }
    }

    private fun MutableList<WalletNotification>.addPushReminderNotification(
        clickIntents: WalletClickIntents,
        shouldShowPushReminderBanner: Boolean,
    ) {
        addIf(
            element = WalletNotification.PushNotifications(
                onCloseClick = clickIntents::onDenyPermissions,
                onEnabledClick = clickIntents::onAllowPermissions,
            ),
            condition = shouldShowPushReminderBanner,
        )
    }

    private fun Lce<TokenListError, List<CryptoCurrencyStatus>>.hasUnreachableNetworks(): Boolean {
        val flattenCurrencies = getOrNull(isPartialContentAccepted = false) ?: return false

        return flattenCurrencies.any { it.value is CryptoCurrencyStatus.Unreachable }
    }

    // Remove in first iteration of yield supply feature
    // private fun Lce<TokenListError, List<CryptoCurrencyStatus>>.hasTokensWithActivatedSupplyWithoutApprove(): Boolean {
    //     val flattenCurrencies = getOrNull(isPartialContentAccepted = false) ?: return false
    //     val yieldSupplyEnabled = yieldSupplyFeatureToggles.isYieldSupplyFeatureEnabled
    //     return yieldSupplyEnabled && flattenCurrencies.any {
    //         it.value.yieldSupplyStatus?.isAllowedToSpend == false
    //     }
    // }

    private fun MutableList<WalletNotification>.addRateTheAppNotification(
        isReadyToShowRating: Boolean,
        clickIntents: WalletClickIntents,
    ) {
        addIf(
            element = WalletNotification.RateApp(
                onLikeClick = clickIntents::onLikeAppClick,
                onDislikeClick = clickIntents::onDislikeAppClick,
                onCloseClick = clickIntents::onCloseRateAppWarningClick,
            ),
            condition = isReadyToShowRating,
        )
    }

    private fun List<CryptoCurrencyStatus>?.getFinishWalletActivationType(): WalletActivationBannerType {
        return if (this?.any { it.value.amount.orZero().isPositive() } == true) {
            WalletActivationBannerType.Warning
        } else {
            WalletActivationBannerType.Attention
        }
    }

    private fun MutableList<WalletNotification>.addFinishWalletActivationNotification(
        userWallet: UserWallet,
        flattenCurrencies: Lce<TokenListError, List<CryptoCurrencyStatus>>,
        clickIntents: WalletClickIntents,
        shouldAccessCodeSkipped: Boolean,
    ) {
        if (userWallet !is UserWallet.Hot) return

        val isBackupExists = userWallet.backedUp
        val isAccessCodeRequired = userWallet.hotWalletId.authType == HotWalletId.AuthType.NoPassword &&
            !shouldAccessCodeSkipped
        val shouldShowFinishActivation = !isBackupExists || isAccessCodeRequired

        val type = flattenCurrencies.fold(
            ifLoading = { return },
            ifContent = { it.getFinishWalletActivationType() },
            ifError = { WalletActivationBannerType.Attention },
        )

        addIf(
            element = WalletNotification.FinishWalletActivation(
                type = type,
                buttonsState = when (type) {
                    WalletActivationBannerType.Warning -> ButtonsState.PrimaryButtonConfig(
                        text = resourceReference(R.string.hw_activation_need_finish),
                        onClick = { clickIntents.onFinishWalletActivationClick(isBackupExists) },
                    )
                    else -> ButtonsState.SecondaryButtonConfig(
                        text = resourceReference(R.string.hw_activation_need_finish),
                        onClick = { clickIntents.onFinishWalletActivationClick(isBackupExists) },
                    )
                },
                isBackupExists = isBackupExists,
            ),
            condition = shouldShowFinishActivation,
        )
    }

    private suspend fun MutableList<WalletNotification>.addUpgradeHotWalletPromoNotification(
        userWallet: UserWallet,
        flattenCurrencies: Lce<TokenListError, List<CryptoCurrencyStatus>>,
        clickIntents: WalletClickIntents,
        shouldShowUpgradeBanner: Boolean,
        closureTimestamp: Long?,
    ) {
        if (userWallet !is UserWallet.Hot) return

        val currencies = flattenCurrencies.getOrNull(isPartialContentAccepted = true).orEmpty()
        val hasBalance = currencies.any { it.value.amount.orZero().isPositive() }

        val shouldShow = checkHotWalletUpgradeBannerUseCase(
            walletId = userWallet.walletId,
            hasBalance = hasBalance,
            shouldShowUpgradeBanner = shouldShowUpgradeBanner,
            closureTimestamp = closureTimestamp,
        ).getOrNull() ?: return

        addIf(
            element = WalletNotification.UpgradeHotWalletPromo(
                onLaterClick = { clickIntents.onCloseUpgradeBannerClick(userWallet.walletId) },
                onUpgradeClick = { clickIntents.onUpgradeHotWalletClick(userWallet.walletId) },
            ),
            condition = shouldShow,
        )
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
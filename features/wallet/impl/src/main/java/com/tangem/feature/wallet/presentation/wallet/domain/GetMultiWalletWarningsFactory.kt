@file:Suppress("MaximumLineLength")

package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.getOrElse
import com.tangem.common.ui.notifications.NotificationId
import com.tangem.common.ui.userwallet.ext.walletInterationIcon
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.ui.components.notifications.NotificationConfig.ButtonsState
import com.tangem.core.ui.components.notifications.NotificationConfig.IconTint
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.TotalFiatBalance
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.tokenlist.TokenList
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.notifications.repository.NotificationsRepository
import com.tangem.domain.onramp.GetOnrampCountryUseCase
import com.tangem.domain.onramp.OnrampSepaAvailableUseCase
import com.tangem.domain.promo.ShouldShowPromoWalletUseCase
import com.tangem.domain.promo.models.PromoId
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.tokens.GetCryptoCurrenciesUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.wallets.models.SeedPhraseNotificationsStatus
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.domain.wallets.usecase.SeedPhraseNotificationUseCase
import com.tangem.feature.wallet.child.wallet.model.WalletActivationBannerType
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.impl.R
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.lib.crypto.BlockchainUtils.isBitcoin
import com.tangem.utils.extensions.addIf
import com.tangem.utils.extensions.isPositive
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList", "LargeClass")
@ModelScoped
internal class GetMultiWalletWarningsFactory @Inject constructor(
    private val tokenListStore: MultiWalletTokenListStore,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val backupValidator: BackupValidator,
    private val seedPhraseNotificationUseCase: SeedPhraseNotificationUseCase,
    private val shouldShowPromoWalletUseCase: ShouldShowPromoWalletUseCase,
    private val getCryptoCurrenciesUseCase: GetCryptoCurrenciesUseCase,
    private val onrampSepaAvailableUseCase: OnrampSepaAvailableUseCase,
    private val getOnrampCountryUseCase: GetOnrampCountryUseCase,
    private val notificationsRepository: NotificationsRepository,
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles,
) {

    @Suppress("UNCHECKED_CAST", "MagicNumber")
    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotification>> {
        val cardTypesResolver = (userWallet as? UserWallet.Cold)?.scanResponse?.cardTypesResolver

        return combine(
            tokenListStore.getOrThrow(userWallet.walletId),
            isReadyToShowRateAppUseCase(),
            isNeedToBackupUseCase(userWallet.walletId),
            seedPhraseNotificationUseCase(userWalletId = userWallet.walletId),
            shouldShowPromoWalletUseCase(userWalletId = userWallet.walletId, promoId = PromoId.Referral),
            shouldShowPromoWalletUseCase(userWalletId = userWallet.walletId, promoId = PromoId.Sepa),
            notificationsRepository.getShouldShowNotification(NotificationId.EnablePushesReminderNotification.key),
        ) { array ->
            val maybeTokenList = array[0] as Lce<TokenListError, TokenList>
            val isReadyToShowRating = array[1] as Boolean
            val isNeedToBackup = array[2] as Boolean
            val seedPhraseIssueStatus = array[3] as SeedPhraseNotificationsStatus
            val shouldShowReferralPromo = array[4] as Boolean
            val shouldShowSepaBanner = array[5] as Boolean
            val shouldShowEnablePushesReminderNotification = array[6] as Boolean

            buildList {
                addUsedOutdatedDataNotification(maybeTokenList)

                addCriticalNotifications(userWallet, seedPhraseIssueStatus, clickIntents)

                addFinishWalletActivationNotification(userWallet, maybeTokenList, clickIntents)

                addReferralPromoNotification(cardTypesResolver, clickIntents, shouldShowReferralPromo)

                addSepaPromoNotification(userWallet, clickIntents, shouldShowSepaBanner)

                addInformationalNotifications(userWallet, cardTypesResolver, maybeTokenList, clickIntents)

                addWarningNotifications(cardTypesResolver, maybeTokenList, isNeedToBackup, clickIntents)

                addPushReminderNotification(
                    clickIntents = clickIntents,
                    shouldShowPushReminderBanner = shouldShowEnablePushesReminderNotification &&
                        !notificationsRepository.isUserAllowToSubscribeOnPushNotifications(),
                )

                addYieldSupplyNotifications(maybeTokenList)

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
        maybeTokenList: Lce<TokenListError, TokenList>,
    ) {
        addIf(
            element = WalletNotification.UsedOutdatedData,
            condition = maybeTokenList.fold(
                ifLoading = {
                    (it?.totalFiatBalance as? TotalFiatBalance.Loaded)?.source == StatusSource.ONLY_CACHE
                },
                ifContent = {
                    (it.totalFiatBalance as? TotalFiatBalance.Loaded)?.source == StatusSource.ONLY_CACHE
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
            element = WalletNotification.Critical.BackupError { clickIntents.onSupportClick() },
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
        maybeTokenList: Lce<TokenListError, TokenList>,
        clickIntents: WalletClickIntents,
    ) {
        addIf(
            element = WalletNotification.Informational.DemoCard,
            condition = cardTypesResolver != null && isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )

        addMissingAddressesNotification(userWallet, maybeTokenList, clickIntents)
    }

    private fun MutableList<WalletNotification>.addMissingAddressesNotification(
        userWallet: UserWallet,
        maybeTokenList: Lce<TokenListError, TokenList>,
        clickIntents: WalletClickIntents,
    ) {
        val currencies = maybeTokenList.getMissingAddressCurrencies()
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

    private fun Lce<TokenListError, TokenList>.getMissingAddressCurrencies(): List<CryptoCurrency> {
        val tokenList = getOrNull(isPartialContentAccepted = true) ?: return emptyList()

        return tokenList
            .flattenCurrencies()
            .filter { it.value is CryptoCurrencyStatus.MissedDerivation }
            .map(CryptoCurrencyStatus::currency)
    }

    private fun MutableList<WalletNotification>.addReferralPromoNotification(
        cardTypesResolver: CardTypesResolver?,
        clickIntents: WalletClickIntents,
        shouldShowPromo: Boolean,
    ) {
        addIf(
            element = WalletNotification.ReferralPromo(
                onCloseClick = { clickIntents.onClosePromoClick(promoId = PromoId.Referral) },
                onClick = { clickIntents.onPromoClick(promoId = PromoId.Referral) },
            ),
            condition = shouldShowPromo && (cardTypesResolver == null || cardTypesResolver.isTangemWallet()),
        )
    }

    private suspend fun MutableList<WalletNotification>.addSepaPromoNotification(
        userWallet: UserWallet,
        clickIntents: WalletClickIntents,
        shouldShowSepaPromo: Boolean,
    ) {
        val currencies = getCryptoCurrenciesUseCase(userWalletId = userWallet.walletId).getOrElse {
            Timber.e("Error on getting crypto currency list")
            return
        }

        val bitcoinCurrency = currencies.find { isBitcoin(it.network.rawId) } ?: return

        val country = getOnrampCountryUseCase.invokeSync(userWallet).getOrElse {
            Timber.e("Error on getting onramp country")
            return
        }

        val isSepaAvailable = onrampSepaAvailableUseCase(
            userWallet = userWallet,
            country = country,
            cryptoCurrency = bitcoinCurrency,
        )

        addIf(
            element = WalletNotification.Sepa(
                onCloseClick = { clickIntents.onClosePromoClick(promoId = PromoId.Sepa) },
                onClick = { clickIntents.onPromoClick(promoId = PromoId.Sepa, bitcoinCurrency) },
            ),
            condition = shouldShowSepaPromo && isSepaAvailable,
        )
    }

    private fun MutableList<WalletNotification>.addYieldSupplyNotifications(
        tokenList: Lce<TokenListError, TokenList>,
    ) {
        addIf(
            element = WalletNotification.Warning.YeildSupplyApprove,
            condition = tokenList.hasTokensWithActivatedSupplyWithoutApprove(),
        )
    }

    private fun MutableList<WalletNotification>.addWarningNotifications(
        cardTypesResolver: CardTypesResolver?,
        tokenList: Lce<TokenListError, TokenList>,
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
            condition = tokenList.hasUnreachableNetworks(),
        )
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

    private fun Lce<TokenListError, TokenList>.hasUnreachableNetworks(): Boolean {
        val tokenList = getOrNull(isPartialContentAccepted = false) ?: return false

        return tokenList.flattenCurrencies().any { it.value is CryptoCurrencyStatus.Unreachable }
    }

    private fun Lce<TokenListError, TokenList>.hasTokensWithActivatedSupplyWithoutApprove(): Boolean {
        val tokenList = getOrNull(isPartialContentAccepted = false) ?: return false
        val yieldSupplyEnabled = yieldSupplyFeatureToggles.isYieldSupplyFeatureEnabled
        return yieldSupplyEnabled && tokenList.flattenCurrencies().any {
            it.value.yieldSupplyStatus?.isAllowedToSpend == false
        }
    }

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

    private fun TokenList?.getFinishWalletActivationType(): WalletActivationBannerType {
        return if ((this?.totalFiatBalance as? TotalFiatBalance.Loaded)?.amount?.isPositive() == true) {
            WalletActivationBannerType.Warning
        } else {
            WalletActivationBannerType.Attention
        }
    }

    private fun MutableList<WalletNotification>.addFinishWalletActivationNotification(
        userWallet: UserWallet,
        maybeTokenList: Lce<TokenListError, TokenList>,
        clickIntents: WalletClickIntents,
    ) {
        if (userWallet !is UserWallet.Hot) return

        val shouldShowFinishActivation = !userWallet.backedUp

        val type = maybeTokenList.fold(
            ifLoading = { it.getFinishWalletActivationType() },
            ifContent = { it.getFinishWalletActivationType() },
            ifError = { WalletActivationBannerType.Attention },
        )

        val tint = when (type) {
            WalletActivationBannerType.Attention -> IconTint.Attention
            WalletActivationBannerType.Warning -> IconTint.Warning
        }

        addIf(
            element = WalletNotification.FinishWalletActivation(
                iconTint = tint,
                buttonsState = when (type) {
                    WalletActivationBannerType.Warning -> ButtonsState.PrimaryButtonConfig(
                        text = resourceReference(R.string.hw_activation_need_finish),
                        onClick = { clickIntents.onFinishWalletActivationClick(type) },
                    )
                    else -> ButtonsState.SecondaryButtonConfig(
                        text = resourceReference(R.string.hw_activation_need_finish),
                        onClick = { clickIntents.onFinishWalletActivationClick(type) },
                    )
                },
            ),
            condition = shouldShowFinishActivation,
        )
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
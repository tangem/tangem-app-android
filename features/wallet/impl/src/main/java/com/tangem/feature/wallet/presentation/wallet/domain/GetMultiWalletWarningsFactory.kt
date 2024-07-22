package com.tangem.feature.wallet.presentation.wallet.domain

import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.promo.PromoBanner
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.settings.ShouldShowSwapPromoWalletUseCase
import com.tangem.domain.tokens.GetTokenListUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.tokens.model.NetworkGroup
import com.tangem.domain.tokens.model.TokenList
import com.tangem.domain.tokens.repository.PromoRepository
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.collections.count

@Suppress("LongParameterList")
@ViewModelScoped
internal class GetMultiWalletWarningsFactory @Inject constructor(
    private val getTokenListUseCase: GetTokenListUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val shouldShowSwapPromoWalletUseCase: ShouldShowSwapPromoWalletUseCase,
    private val promoRepository: PromoRepository,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val backupValidator: BackupValidator,
) {

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotification>> {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver

        val promoFlow = flow { emit(promoRepository.getOkxPromoBanner()) }
        return combine(
            flow = getTokenListUseCase.launch(userWallet.walletId),
            flow2 = isReadyToShowRateAppUseCase(),
            flow3 = isNeedToBackupUseCase(userWallet.walletId),
            flow4 = shouldShowSwapPromoWalletUseCase(),
            flow5 = promoFlow,
        ) { maybeTokenList, isReadyToShowRating, isNeedToBackup, shouldShowPromo, promoBanner ->
            buildList {
                addSwapPromoNotification(shouldShowPromo, promoBanner, clickIntents)

                addCriticalNotifications(userWallet)

                addInformationalNotifications(cardTypesResolver, maybeTokenList, clickIntents)

                addWarningNotifications(cardTypesResolver, maybeTokenList, isNeedToBackup, clickIntents)

                val hasCriticalOrWarning = any { notification ->
                    notification is WalletNotification.Critical || notification is WalletNotification.Warning
                }

                if (!hasCriticalOrWarning) {
                    addRateTheAppNotification(isReadyToShowRating, clickIntents)
                }
            }.toImmutableList()
        }
    }

    private fun MutableList<WalletNotification>.addSwapPromoNotification(
        shouldShowPromo: Boolean,
        promoBanner: PromoBanner?,
        clickIntents: WalletClickIntents,
    ) {
        promoBanner ?: return
        val promoNotification = WalletNotification.SwapPromo(
            startDateTime = promoBanner.bannerState.timeline.start,
            endDateTime = promoBanner.bannerState.timeline.end,
            onCloseClick = clickIntents::onCloseSwapPromoClick,
        )
        addIf(
            element = promoNotification,
            condition = shouldShowPromo && promoBanner.isActive,
        )
    }

    private fun MutableList<WalletNotification>.addCriticalNotifications(userWallet: UserWallet) {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver
        addIf(
            element = WalletNotification.Critical.BackupError,
            condition = !backupValidator.isValid(userWallet.scanResponse.card) || userWallet.hasBackupError,
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

    private fun MutableList<WalletNotification>.addInformationalNotifications(
        cardTypesResolver: CardTypesResolver,
        maybeTokenList: Lce<TokenListError, TokenList>,
        clickIntents: WalletClickIntents,
    ) {
        addIf(
            element = WalletNotification.Informational.DemoCard,
            condition = isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )

        addMissingAddressesNotification(maybeTokenList, clickIntents)
    }

    private fun MutableList<WalletNotification>.addMissingAddressesNotification(
        maybeTokenList: Lce<TokenListError, TokenList>,
        clickIntents: WalletClickIntents,
    ) {
        val currencies = maybeTokenList.getMissingAddressCurrencies()

        addIf(
            element = WalletNotification.Informational.MissingAddresses(
                missingAddressesCount = currencies.count(),
                onGenerateClick = {
                    clickIntents.onGenerateMissedAddressesClick(missedAddressCurrencies = currencies)
                },
            ),
            condition = currencies.isNotEmpty(),
        )
    }

    private fun Lce<TokenListError, TokenList>.getMissingAddressCurrencies(): List<CryptoCurrency> {
        val tokenList = getOrNull(isPartialContentAccepted = false) ?: return emptyList()

        val currencies = when (tokenList) {
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
            is TokenList.Ungrouped -> tokenList.currencies
            is TokenList.Empty -> emptyList()
        }

        return currencies
            .filter { it.value is CryptoCurrencyStatus.MissedDerivation }
            .map(CryptoCurrencyStatus::currency)
    }

    private fun MutableList<WalletNotification>.addWarningNotifications(
        cardTypesResolver: CardTypesResolver,
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
            condition = cardTypesResolver.isTestCard(),
        )

        addIf(
            element = WalletNotification.Warning.SomeNetworksUnreachable,
            condition = tokenList.hasUnreachableNetworks(),
        )
    }

    private fun Lce<TokenListError, TokenList>.hasUnreachableNetworks(): Boolean {
        val tokenList = getOrNull(isPartialContentAccepted = false) ?: return false

        val currencies = when (tokenList) {
            is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
            is TokenList.Ungrouped -> tokenList.currencies
            is TokenList.Empty -> emptyList()
        }

        return currencies.any { it.value is CryptoCurrencyStatus.Unreachable }
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

    private fun MutableList<WalletNotification>.addIf(element: WalletNotification, condition: Boolean) {
        if (condition) add(element = element)
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
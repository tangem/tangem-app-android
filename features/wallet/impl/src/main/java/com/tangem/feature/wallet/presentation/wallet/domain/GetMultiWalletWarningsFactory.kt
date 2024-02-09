package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
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
import com.tangem.domain.wallets.usecase.GetSelectedWalletSyncUseCase
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.presentation.wallet.state.components.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntentsV2
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ViewModelScoped
internal class GetMultiWalletWarningsFactory @Inject constructor(
    private val getSelectedWalletSyncUseCase: GetSelectedWalletSyncUseCase,
    private val getTokenListUseCase: GetTokenListUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val shouldShowSwapPromoWalletUseCase: ShouldShowSwapPromoWalletUseCase,
    private val promoRepository: PromoRepository,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
) {

    private var readyForRateAppNotification = false

    fun create(clickIntents: WalletClickIntentsV2): Flow<ImmutableList<WalletNotification>> {
        val userWallet = getSelectedWalletSyncUseCase().fold(
            ifLeft = {
                Timber.e("Failed to get selected wallet $it")
                return flowOf(value = persistentListOf())
            },
            ifRight = { it },
        )

        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver

        val promoFlow = flow { emit(promoRepository.getChangellyPromoBanner()) }
        return combine(
            flow = getTokenListUseCase(userWallet.walletId).conflate(),
            flow2 = isReadyToShowRateAppUseCase().conflate(),
            flow3 = isNeedToBackupUseCase(userWallet.walletId).conflate(),
            flow4 = shouldShowSwapPromoWalletUseCase().conflate(),
            flow5 = promoFlow,
        ) { maybeTokenList, isReadyToShowRating, isNeedToBackup, shouldShowPromo, promoBanner ->

            readyForRateAppNotification = true
            buildList {
                addSwapPromoNotification(shouldShowPromo, promoBanner, clickIntents)

                addCriticalNotifications(cardTypesResolver)

                addInformationalNotifications(cardTypesResolver, maybeTokenList, clickIntents)

                addWarningNotifications(cardTypesResolver, maybeTokenList, isNeedToBackup, clickIntents)

                addRateTheAppNotification(isReadyToShowRating, clickIntents)
            }.toImmutableList()
        }
    }

    private fun MutableList<WalletNotification>.addSwapPromoNotification(
        shouldShowPromo: Boolean,
        promoBanner: PromoBanner?,
        clickIntents: WalletClickIntentsV2,
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

    private fun MutableList<WalletNotification>.addInformationalNotifications(
        cardTypesResolver: CardTypesResolver,
        maybeTokenList: Either<TokenListError, TokenList>,
        clickIntents: WalletClickIntentsV2,
    ) {
        addIf(
            element = WalletNotification.Informational.DemoCard,
            condition = isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )

        addMissingAddressesNotification(maybeTokenList, clickIntents)
    }

    private fun MutableList<WalletNotification>.addMissingAddressesNotification(
        maybeTokenList: Either<TokenListError, TokenList>,
        clickIntents: WalletClickIntentsV2,
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

    private fun Either<TokenListError, TokenList>.getMissingAddressCurrencies(): List<CryptoCurrency> {
        return fold(
            ifLeft = { emptyList() },
            ifRight = { tokenList ->
                val currencies = when (tokenList) {
                    is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
                    is TokenList.Ungrouped -> tokenList.currencies
                    is TokenList.Empty -> emptyList()
                }

                currencies
                    .filter { it.value is CryptoCurrencyStatus.MissedDerivation }
                    .map(CryptoCurrencyStatus::currency)
            },
        )
    }

    private fun MutableList<WalletNotification>.addWarningNotifications(
        cardTypesResolver: CardTypesResolver,
        tokenList: Either<TokenListError, TokenList>,
        isNeedToBackup: Boolean,
        clickIntents: WalletClickIntentsV2,
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

    private fun Either<TokenListError, TokenList>.hasUnreachableNetworks(): Boolean {
        return fold(
            ifLeft = { false },
            ifRight = { tokenList ->
                val currencies = when (tokenList) {
                    is TokenList.GroupedByNetwork -> tokenList.groups.flatMap(NetworkGroup::currencies)
                    is TokenList.Ungrouped -> tokenList.currencies
                    is TokenList.Empty -> emptyList()
                }

                currencies.any { it.value is CryptoCurrencyStatus.Unreachable }
            },
        )
    }

    private fun MutableList<WalletNotification>.addRateTheAppNotification(
        isReadyToShowRating: Boolean,
        clickIntents: WalletClickIntentsV2,
    ) {
        addIf(
            element = WalletNotification.RateApp(
                onLikeClick = clickIntents::onLikeAppClick,
                onDislikeClick = clickIntents::onDislikeAppClick,
                onCloseClick = clickIntents::onCloseRateAppWarningClick,
            ),
            condition = isReadyToShowRating && readyForRateAppNotification,
        )
    }

    private fun MutableList<WalletNotification>.addIf(element: WalletNotification, condition: Boolean) {
        if (condition) {
            add(element = element)
            if (element is WalletNotification.Critical || element is WalletNotification.Warning) {
                readyForRateAppNotification = false
            }
        }
    }

    private companion object {
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}

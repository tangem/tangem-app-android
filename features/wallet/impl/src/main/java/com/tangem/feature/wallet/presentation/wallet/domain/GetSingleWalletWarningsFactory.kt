package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import com.tangem.domain.common.CardTypesResolver
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.tokens.GetPrimaryCurrencyStatusUpdatesUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.CryptoCurrencyStatus
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.feature.wallet.presentation.wallet.viewmodels.intents.WalletClickIntents
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ViewModelScoped
internal class GetSingleWalletWarningsFactory @Inject constructor(
    private val getPrimaryCurrencyStatusUpdatesUseCase: GetPrimaryCurrencyStatusUpdatesUseCase,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase,
) {

    private var readyForRateAppNotification = false

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotification>> {
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver

        return combine(
            flow = getPrimaryCurrencyStatusUpdatesUseCase(userWallet.walletId),
            flow2 = isReadyToShowRateAppUseCase().conflate(),
            flow3 = isNeedToBackupUseCase(userWallet.walletId).conflate(),
        ) { primaryCurrencyStatus, isReadyToShowRating, isNeedToBackup ->
            readyForRateAppNotification = true
            buildList {
                addCriticalNotifications(cardTypesResolver)

                addInformationalNotifications(cardTypesResolver)

                addWarningNotifications(
                    userWallet,
                    cardTypesResolver,
                    primaryCurrencyStatus,
                    isNeedToBackup,
                    clickIntents,
                )

                addRateTheAppNotification(isReadyToShowRating, clickIntents)
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

    private fun MutableList<WalletNotification>.addInformationalNotifications(cardTypesResolver: CardTypesResolver) {
        addIf(
            element = WalletNotification.Informational.DemoCard,
            condition = isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )
    }

    private suspend fun MutableList<WalletNotification>.addWarningNotifications(
        userWallet: UserWallet,
        cardTypesResolver: CardTypesResolver,
        maybePrimaryCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>,
        isNeedToBackup: Boolean,
        clickIntents: WalletClickIntents,
    ) {
        val cryptoCurrencyStatus = maybePrimaryCurrencyStatus.fold(ifLeft = { null }, ifRight = { it })

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
            element = WalletNotification.Warning.NetworksUnreachable,
            condition = cryptoCurrencyStatus?.value is CryptoCurrencyStatus.Unreachable,
        )

        addNoAccountWarning(cryptoCurrencyStatus)

        addIf(
            element = WalletNotification.Warning.NumberOfSignedHashesIncorrect(
                onCloseClick = clickIntents::onCloseAlreadySignedHashesWarningClick,
            ),
            condition = hasSignedHashes(userWallet, cryptoCurrencyStatus),
        )
    }

    private fun MutableList<WalletNotification>.addNoAccountWarning(cryptoCurrencyStatus: CryptoCurrencyStatus?) {
        val noAccountStatus = cryptoCurrencyStatus?.value as? CryptoCurrencyStatus.NoAccount
        if (noAccountStatus != null) {
            add(
                element = WalletNotification.Informational.NoAccount(
                    network = cryptoCurrencyStatus.currency.name,
                    amount = noAccountStatus.amountToCreateAccount.toString(),
                    symbol = cryptoCurrencyStatus.currency.symbol,
                ),
            )
        }
    }

    private suspend fun hasSignedHashes(
        selectedWallet: UserWallet,
        cryptoCurrencyStatus: CryptoCurrencyStatus?,
    ): Boolean {
        return cryptoCurrencyStatus?.currency?.network?.let {
            hasSingleWalletSignedHashesUseCase(userWallet = selectedWallet, network = it)
                .conflate()
                .distinctUntilChanged()
                .firstOrNull()
        } ?: false
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
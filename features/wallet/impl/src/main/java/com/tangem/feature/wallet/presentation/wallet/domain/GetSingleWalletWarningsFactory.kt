package com.tangem.feature.wallet.presentation.wallet.domain

import arrow.core.Either
import arrow.core.right
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.domain.account.featuretoggle.AccountsFeatureToggles
import com.tangem.domain.account.status.producer.SingleAccountStatusProducer
import com.tangem.domain.account.status.supplier.SingleAccountStatusSupplier
import com.tangem.domain.card.CardTypesResolver
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.demo.IsDemoCardUseCase
import com.tangem.domain.models.StatusSource
import com.tangem.domain.models.account.AccountId
import com.tangem.domain.models.account.AccountStatus
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.settings.IsReadyToShowRateAppUseCase
import com.tangem.domain.tokens.GetSingleCryptoCurrencyStatusUseCase
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.domain.wallets.usecase.IsNeedToBackupUseCase
import com.tangem.feature.wallet.child.wallet.model.intents.WalletClickIntents
import com.tangem.feature.wallet.presentation.wallet.state.model.WalletNotification
import com.tangem.utils.extensions.addIf
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@ModelScoped
@Suppress("LongParameterList")
internal class GetSingleWalletWarningsFactory @Inject constructor(
    private val accountsFeatureToggles: AccountsFeatureToggles,
    private val getSingleCryptoCurrencyStatusUseCase: GetSingleCryptoCurrencyStatusUseCase,
    private val singleAccountStatusSupplier: SingleAccountStatusSupplier,
    private val isDemoCardUseCase: IsDemoCardUseCase,
    private val isReadyToShowRateAppUseCase: IsReadyToShowRateAppUseCase,
    private val isNeedToBackupUseCase: IsNeedToBackupUseCase,
    private val hasSingleWalletSignedHashesUseCase: HasSingleWalletSignedHashesUseCase,
    private val getWalletsUseCase: GetWalletsUseCase,
) {

    private var readyForRateAppNotification = false

    fun create(userWallet: UserWallet, clickIntents: WalletClickIntents): Flow<ImmutableList<WalletNotification>> {
        if (userWallet !is UserWallet.Cold) {
            return flowOf(emptyList<WalletNotification>().toImmutableList())
        }
        val cardTypesResolver = userWallet.scanResponse.cardTypesResolver

        return combine(
            flow = getPrimaryCurrencyStatusFlow(userWallet),
            flow2 = isReadyToShowRateAppUseCase().conflate(),
            flow3 = isNeedToBackupUseCase(userWallet.walletId).conflate(),
            flow4 = getWalletsUseCase().conflate(),
        ) { maybePrimaryCurrencyStatus, isReadyToShowRating, isNeedToBackup, userWallets ->
            readyForRateAppNotification = true
            buildList {
                addUsedOutdatedDataNotification(maybePrimaryCurrencyStatus)

                addCriticalNotifications(
                    cardTypesResolver = cardTypesResolver,
                )

                addInformationalNotifications(
                    userWallets = userWallets,
                    cardTypesResolver = cardTypesResolver,
                    clickIntents = clickIntents,
                )

                addWarningNotifications(
                    userWallet = userWallet,
                    cardTypesResolver = cardTypesResolver,
                    maybePrimaryCurrencyStatus = maybePrimaryCurrencyStatus,
                    isNeedToBackup = isNeedToBackup,
                    clickIntents = clickIntents,
                )

                addRateTheAppNotification(
                    isReadyToShowRating = isReadyToShowRating,
                    clickIntents = clickIntents,
                )
            }.toImmutableList()
        }
    }

    private fun MutableList<WalletNotification>.addUsedOutdatedDataNotification(
        maybePrimaryCurrencyStatus: Either<CurrencyStatusError, CryptoCurrencyStatus>,
    ) {
        addIf(
            element = WalletNotification.UsedOutdatedData,
            condition = maybePrimaryCurrencyStatus.fold(
                ifLeft = { false },
                ifRight = { it.value.sources.total == StatusSource.ONLY_CACHE },
            ),
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
        userWallets: List<UserWallet>,
        cardTypesResolver: CardTypesResolver,
        clickIntents: WalletClickIntents,
    ) {
        val userHasWalletOrWallet2 = userWallets.filterIsInstance<UserWallet.Cold>().any {
            val typesResolver = it.scanResponse.cardTypesResolver
            typesResolver.isTangemWallet() || typesResolver.isWallet2()
        }

        addIf(
            element = WalletNotification.NoteMigration(
                onClick = { clickIntents.onNoteMigrationButtonClick(NOTE_MIGRATION_URL) },
            ),
            condition = cardTypesResolver.isTangemNote() && !userHasWalletOrWallet2,
        )

        addIf(
            element = WalletNotification.Informational.DemoCard,
            condition = isDemoCardUseCase(cardId = cardTypesResolver.getCardId()),
        )
    }

    private suspend fun MutableList<WalletNotification>.addWarningNotifications(
        userWallet: UserWallet.Cold,
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
        selectedWallet: UserWallet.Cold,
        cryptoCurrencyStatus: CryptoCurrencyStatus?,
    ): Boolean {
        return cryptoCurrencyStatus?.currency?.network?.let {
            hasSingleWalletSignedHashesUseCase(userWallet = selectedWallet, network = it)
                .conflate()
                .distinctUntilChanged()
                .firstOrNull()
        } == true
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
        addIf(condition) {
            if (element is WalletNotification.Critical ||
                element is WalletNotification.Warning ||
                element is WalletNotification.NoteMigration
            ) {
                readyForRateAppNotification = false
            }

            element
        }
    }

    private fun getPrimaryCurrencyStatusFlow(
        userWallet: UserWallet,
    ): Flow<Either<CurrencyStatusError, CryptoCurrencyStatus>> {
        return if (accountsFeatureToggles.isFeatureEnabled) {
            getAccountStatusFlow(userWallet).mapNotNull { accountStatus ->
                accountStatus.flattenCurrencies().firstOrNull()
            }
                .distinctUntilChanged()
                .conflate()
                .map { it.right() }
        } else {
            getSingleCryptoCurrencyStatusUseCase.invokeSingleWallet(userWallet.walletId)
        }
    }

    private fun getAccountStatusFlow(userWallet: UserWallet): Flow<AccountStatus> {
        val accountId = AccountId.forMainCryptoPortfolio(userWalletId = userWallet.walletId)

        return singleAccountStatusSupplier(SingleAccountStatusProducer.Params(accountId))
            .distinctUntilChanged()
            .conflate()
    }

    private companion object {
        const val NOTE_MIGRATION_URL = "https://tangem.com/en/?promocode=Note10"
        const val MAX_REMAINING_SIGNATURES_COUNT = 10
    }
}
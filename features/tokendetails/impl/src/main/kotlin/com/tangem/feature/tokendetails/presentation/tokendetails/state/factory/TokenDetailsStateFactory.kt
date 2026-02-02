package com.tangem.feature.tokendetails.presentation.tokendetails.state.factory

import arrow.core.Either
import com.tangem.common.ui.bottomsheet.chooseaddress.ChooseAddressBottomSheetConfig
import com.tangem.common.ui.tokendetails.TokenDetailsDialogConfig
import com.tangem.common.ui.tokens.getUnavailabilityReasonText
import com.tangem.feature.tokendetails.presentation.tokendetails.ui.components.clore.CloreMigrationBottomSheetConfig
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.core.ui.components.dropdownmenu.TangemDropdownMenuItem
import com.tangem.core.ui.extensions.TextReference
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.extensions.themedColor
import com.tangem.core.ui.res.TangemTheme
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.currency.CryptoCurrencyStatus
import com.tangem.domain.models.network.NetworkAddress
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingAvailability
import com.tangem.domain.staking.model.StakingEntryInfo
import com.tangem.domain.tokens.error.CurrencyStatusError
import com.tangem.domain.tokens.model.ScenarioUnavailabilityReason
import com.tangem.domain.tokens.model.TokenActionsState
import com.tangem.domain.tokens.model.warnings.CryptoCurrencyWarning
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.domain.wallets.usecase.NetworkHasDerivationUseCase
import com.tangem.domain.yield.supply.models.YieldSupplyRewardBalance
import com.tangem.feature.tokendetails.presentation.tokendetails.model.ExpressTransactionsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.model.TokenDetailsClickIntents
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenBalanceSegmentedButtonConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsAppBarMenuConfig
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsBalanceBlockState
import com.tangem.feature.tokendetails.presentation.tokendetails.state.TokenDetailsState
import com.tangem.features.tokendetails.impl.R
import com.tangem.features.yield.supply.api.YieldSupplyFeatureToggles
import com.tangem.utils.Provider
import kotlinx.collections.immutable.toImmutableList

@Suppress("TooManyFunctions", "LargeClass", "LongParameterList")
internal class TokenDetailsStateFactory(
    private val currentStateProvider: Provider<TokenDetailsState>,
    private val appCurrencyProvider: Provider<AppCurrency>,
    private val cryptoCurrencyStatusProvider: Provider<CryptoCurrencyStatus?>,
    private val tokenDetailsClickIntents: TokenDetailsClickIntents,
    private val expressTransactionsClickIntents: ExpressTransactionsClickIntents,
    private val networkHasDerivationUseCase: NetworkHasDerivationUseCase,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val userWalletId: UserWalletId,
    private val yieldSupplyFeatureToggles: YieldSupplyFeatureToggles,
) {

    private val skeletonStateConverter by lazy {
        TokenDetailsSkeletonStateConverter(
            clickIntents = tokenDetailsClickIntents,
            networkHasDerivationUseCase = networkHasDerivationUseCase,
            getUserWalletUseCase = getUserWalletUseCase,
            userWalletId = userWalletId,
            yieldSupplyFeatureToggles = yieldSupplyFeatureToggles,
        )
    }

    private val notificationConverter by lazy {
        TokenDetailsNotificationConverter(
            userWalletId = userWalletId,
            getUserWalletUseCase = getUserWalletUseCase,
            clickIntents = tokenDetailsClickIntents,
        )
    }

    private val tokenDetailsLoadedBalanceConverter by lazy {
        TokenDetailsLoadedBalanceConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
            clickIntents = tokenDetailsClickIntents,
            yieldSupplyFeatureToggles = yieldSupplyFeatureToggles,
        )
    }

    private val tokenDetailsButtonsConverter by lazy {
        TokenDetailsActionButtonsConverter(
            currentStateProvider = currentStateProvider,
            clickIntents = tokenDetailsClickIntents,
        )
    }

    private val refreshStateConverter by lazy {
        TokenDetailsRefreshStateConverter(
            currentStateProvider = currentStateProvider,
        )
    }

    private val balanceSelectStateConverter by lazy {
        TokenDetailsBalanceSelectStateConverter(
            currentStateProvider = currentStateProvider,
            appCurrencyProvider = appCurrencyProvider,
            cryptoCurrencyStatusProvider = cryptoCurrencyStatusProvider,
        )
    }

    fun getInitialState(screenArgument: CryptoCurrency): TokenDetailsState {
        return skeletonStateConverter.convert(value = screenArgument)
    }

    fun getCurrencyLoadedBalanceState(
        cryptoCurrencyEither: Either<CurrencyStatusError, CryptoCurrencyStatus>,
    ): TokenDetailsState {
        return tokenDetailsLoadedBalanceConverter.convert(cryptoCurrencyEither)
    }

    fun getStakingInfoState(
        state: TokenDetailsState,
        stakingEntryInfo: StakingEntryInfo?,
        stakingAvailability: StakingAvailability,
        cryptoCurrencyStatus: CryptoCurrencyStatus,
    ): TokenDetailsState {
        return TokenDetailsStakingInfoConverter(
            currentState = state,
            cryptoCurrencyStatus = cryptoCurrencyStatus,
            clickIntents = tokenDetailsClickIntents,
            appCurrencyProvider = appCurrencyProvider,
            stakingEntryInfo = stakingEntryInfo,
        ).convert(stakingAvailability)
    }

    fun getManageButtonsState(actions: List<TokenActionsState.ActionState>): TokenDetailsState {
        return tokenDetailsButtonsConverter.convert(actions)
    }

    fun getStateWithClosedDialog(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(dialogConfig = state.dialogConfig?.copy(isShow = false))
    }

    fun getStateWithConfirmHideTokenDialog(currency: CryptoCurrency): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.ConfirmHideConfig(
                    currencyTitle = currency.name,
                    onConfirmClick = tokenDetailsClickIntents::onHideConfirmed,
                    onCancelClick = expressTransactionsClickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithLinkedTokensDialog(currency: CryptoCurrency): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.HasLinkedTokensConfig(
                    currencyName = currency.name,
                    currencySymbol = currency.symbol,
                    networkName = currency.network.name,
                    onConfirmClick = expressTransactionsClickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithDismissIncompleteTransactionConfirmDialog(): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.RemoveIncompleteTransactionConfirmDialogConfig(
                    onConfirmClick = tokenDetailsClickIntents::onConfirmDismissIncompleteTransactionClick,
                    onCancelClick = expressTransactionsClickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithActionButtonErrorDialog(unavailabilityReason: ScenarioUnavailabilityReason): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.DisabledButtonReasonDialogConfig(
                    text = unavailabilityReason.getUnavailabilityReasonText(),
                    onConfirmClick = expressTransactionsClickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getStateWithErrorDialog(text: TextReference): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.ErrorDialogConfig(
                    text = text,
                    onConfirmClick = expressTransactionsClickIntents::onDismissDialog,
                ),
            ),
        )
    }

    fun getRefreshingState(): TokenDetailsState {
        return refreshStateConverter.convert(true)
    }

    fun getRefreshedState(): TokenDetailsState {
        return refreshStateConverter.convert(false)
    }

    fun getStateWithChooseAddressBottomSheet(
        currency: CryptoCurrency,
        networkAddress: NetworkAddress,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissBottomSheet,
                content = ChooseAddressBottomSheetConfig(
                    currency = currency,
                    networkAddress = networkAddress,
                    onClick = tokenDetailsClickIntents::onAddressTypeSelected,
                ),
            ),
        )
    }

    fun getStateWithClosedBottomSheet(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            bottomSheetConfig = state.bottomSheetConfig?.copy(isShown = false),
        )
    }

    fun getStateWithUpdatedHidden(isBalanceHidden: Boolean): TokenDetailsState {
        val currentState = currentStateProvider()

        return currentState.copy(isBalanceHidden = isBalanceHidden)
    }

    fun getStateWithNotifications(warnings: Set<CryptoCurrencyWarning>): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.convert(warnings))
    }

    fun getStateWithRemovedRentNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeRentInfo(state))
    }

    fun getStateWithRemovedHederaAssociateNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeHederaAssociateWarning(state))
    }

    fun getStateWithRemovedRequiredTrustlineNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(notifications = notificationConverter.removeRequiredTrustlineWarning(state))
    }

    fun getStateWithRemovedKaspaIncompleteTransactionNotification(): TokenDetailsState {
        val state = currentStateProvider()
        return state.copy(
            notifications = notificationConverter.removeKaspaIncompleteTransactionWarning(state),
            dialogConfig = state.dialogConfig?.copy(isShow = false),
        )
    }

    fun getStateWithUpdatedMenu(
        userWallet: UserWallet,
        hasDerivations: Boolean,
        isSupported: Boolean,
    ): TokenDetailsState {
        return with(currentStateProvider()) {
            copy(
                topAppBarConfig = topAppBarConfig.copy(
                    tokenDetailsAppBarMenuConfig = topAppBarConfig.tokenDetailsAppBarMenuConfig
                        ?.updateMenu(userWallet, hasDerivations, isSupported),
                ),
            )
        }
    }

    fun getStateWithUpdatedBalanceSegmentedButtonConfig(
        buttonConfig: TokenBalanceSegmentedButtonConfig,
    ): TokenDetailsState {
        return balanceSelectStateConverter.convert(buttonConfig)
    }

    fun getStateWithUpdatedYieldSupplyDisplayBalance(
        yieldSupplyRewardBalance: YieldSupplyRewardBalance,
    ): TokenDetailsState {
        val state = currentStateProvider()
        val balanceState = state.tokenBalanceBlockState
        return state.copy(
            tokenBalanceBlockState = when (balanceState) {
                is TokenDetailsBalanceBlockState.Content ->
                    balanceState.copy(
                        displayYieldSupplyFiatBalance = yieldSupplyRewardBalance.fiatBalance,
                        displayYieldSupplyCryptoBalance = yieldSupplyRewardBalance.cryptoBalance,
                    )
                is TokenDetailsBalanceBlockState.Error -> balanceState
                is TokenDetailsBalanceBlockState.Loading -> balanceState
            },
        )
    }

    fun getStateWithConfirmHideExpressStatus(): TokenDetailsState {
        return currentStateProvider().copy(
            dialogConfig = TokenDetailsDialogConfig(
                isShow = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissDialog,
                content = TokenDetailsDialogConfig.DialogContentConfig.ConfirmExpressStatusHideDialogConfig(
                    onConfirmClick = {
                        expressTransactionsClickIntents.onDisposeExpressStatus()
                        expressTransactionsClickIntents.onDismissDialog()
                    },
                    onCancelClick = expressTransactionsClickIntents::onDismissDialog,
                ),
            ),
        )
    }

    private fun TokenDetailsAppBarMenuConfig.updateMenu(
        userWallet: UserWallet,
        hasDerivations: Boolean,
        isSupported: Boolean,
    ): TokenDetailsAppBarMenuConfig? {
        if (userWallet is UserWallet.Cold &&
            userWallet.scanResponse.cardTypesResolver.isSingleWalletWithToken()
        ) {
            return null
        }

        return copy(
            items = buildList {
                if (isSupported && hasDerivations) {
                    TangemDropdownMenuItem(
                        title = resourceReference(R.string.token_details_generate_xpub),
                        textColor = themedColor { TangemTheme.colors.text.primary1 },
                        onClick = tokenDetailsClickIntents::onGenerateExtendedKey,
                    ).let(::add)
                }
                TangemDropdownMenuItem(
                    title = TextReference.Res(id = R.string.token_details_hide_token),
                    textColor = themedColor { TangemTheme.colors.text.warning },
                    onClick = tokenDetailsClickIntents::onHideClick,
                ).let(::add)
            }.toImmutableList(),
        )
    }

    // region Clore migration
    // TODO: Remove after Clore migration ends ([REDACTED_TASK_KEY])

    fun getStateWithCloreMigrationBottomSheet(
        message: String,
        signature: String,
        isSigningInProgress: Boolean,
        onMessageChange: (String) -> Unit,
        onSignClick: () -> Unit,
        onCopyClick: () -> Unit,
        onOpenPortalClick: () -> Unit,
    ): TokenDetailsState {
        return currentStateProvider().copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = expressTransactionsClickIntents::onDismissBottomSheet,
                content = CloreMigrationBottomSheetConfig(
                    message = message,
                    signature = signature,
                    isSigningInProgress = isSigningInProgress,
                    onMessageChange = onMessageChange,
                    onSignClick = onSignClick,
                    onCopyClick = onCopyClick,
                    onOpenPortalClick = onOpenPortalClick,
                ),
            ),
        )
    }

    fun getStateWithUpdatedCloreMigrationSignature(signature: String): TokenDetailsState {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetConfig ?: return state
        val content = bottomSheetConfig.content as? CloreMigrationBottomSheetConfig ?: return state

        return state.copy(
            bottomSheetConfig = bottomSheetConfig.copy(
                content = content.copy(
                    signature = signature,
                    isSigningInProgress = false,
                ),
            ),
        )
    }

    fun getStateWithCloreMigrationSigning(): TokenDetailsState {
        val state = currentStateProvider()
        val bottomSheetConfig = state.bottomSheetConfig ?: return state
        val content = bottomSheetConfig.content as? CloreMigrationBottomSheetConfig ?: return state

        return state.copy(
            bottomSheetConfig = bottomSheetConfig.copy(
                content = content.copy(isSigningInProgress = true),
            ),
        )
    }

    // endregion Clore migration
}
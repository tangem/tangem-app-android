package com.tangem.features.walletconnect.connections.utils

import arrow.core.Either
import com.tangem.common.ui.userwallet.converter.UserWalletItemUMConverter
import com.tangem.common.ui.userwallet.state.UserWalletItemUM
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.ui.UiMessageSender
import com.tangem.core.ui.extensions.resourceReference
import com.tangem.core.ui.message.SnackbarMessage
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.appcurrency.error.SelectedAppCurrencyError
import com.tangem.domain.appcurrency.model.AppCurrency
import com.tangem.domain.balancehiding.BalanceHidingSettings
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.core.lce.Lce
import com.tangem.domain.core.lce.lce
import com.tangem.domain.core.utils.getOrElse
import com.tangem.domain.core.utils.toLce
import com.tangem.domain.models.ArtworkModel
import com.tangem.domain.tokens.GetWalletTotalBalanceUseCase
import com.tangem.domain.tokens.error.TokenListError
import com.tangem.domain.tokens.model.TotalFiatBalance
import com.tangem.domain.wallets.models.UserWallet
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.domain.wallets.usecase.GetCardImageUseCase
import com.tangem.domain.wallets.usecase.GetWalletsUseCase
import com.tangem.features.walletconnect.impl.R
import com.tangem.operations.attestation.ArtworkSize
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*

@Suppress("LongParameterList")
@ModelScoped
internal class WcUserWalletsFetcher(
    getWalletsUseCase: GetWalletsUseCase,
    private val getWalletTotalBalanceUseCase: GetWalletTotalBalanceUseCase,
    private val getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    private val getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val messageSender: UiMessageSender,
    private val getCardImageUseCase: GetCardImageUseCase,
    private val onWalletSelected: (UserWalletId) -> Unit,
) {

    private var loadedArtworks: HashMap<UserWalletId, ArtworkModel> = hashMapOf()

    @OptIn(ExperimentalCoroutinesApi::class)
    val userWallets: Flow<ImmutableList<UserWalletItemUM>> = getWalletsUseCase().transformLatest { wallets ->
        val uiModels = UserWalletItemUMConverter(onClick = onWalletSelected)
            .convertList(wallets)
            .toImmutableList()

        emit(uiModels)
        combine(
            flow = getSelectedAppCurrencyUseCase().distinctUntilChanged(),
            flow2 = getBalanceHidingSettingsUseCase().distinctUntilChanged(),
            flow3 = getWalletTotalBalanceUseCase(wallets.map(UserWallet::walletId)).distinctUntilChanged(),
            flow4 = loadArtworks(wallets),
        ) { maybeAppCurrency, balanceHidingSettings, maybeBalances, artworks ->
            val models = createUiModels(
                wallets = wallets,
                maybeAppCurrency = maybeAppCurrency,
                maybeBalances = maybeBalances,
                balanceHidingSettings = balanceHidingSettings,
                artworks = artworks,
            ).getOrElse(
                ifLoading = { return@combine },
                ifError = {
                    val message = resourceReference(R.string.common_unknown_error)
                    messageSender.send(SnackbarMessage(message))

                    return@combine
                },
            )

            emit(models)
        }.collect()
    }

    private fun loadArtworks(wallets: List<UserWallet>): Flow<HashMap<UserWalletId, ArtworkModel>> {
        return flow {
            emit(hashMapOf()) // emits right away so the transform doesn't wait for the images' loading to finish
            wallets.forEach { wallet ->
                val card = wallet.scanResponse.card
                val artwork = getCardImageUseCase(
                    cardId = wallet.cardId,
                    cardPublicKey = card.cardPublicKey,
                    size = ArtworkSize.SMALL,
                    manufacturerName = card.manufacturer.name,
                    firmwareVersion = card.firmwareVersion.toSdkFirmwareVersion(),
                )
                loadedArtworks[wallet.walletId] = artwork
                emit(loadedArtworks)
            }
        }
    }

    private fun createUiModels(
        wallets: List<UserWallet>,
        maybeAppCurrency: Either<SelectedAppCurrencyError, AppCurrency>,
        maybeBalances: Lce<TokenListError, Map<UserWalletId, TotalFiatBalance>>,
        balanceHidingSettings: BalanceHidingSettings,
        artworks: HashMap<UserWalletId, ArtworkModel>,
    ): Lce<Error, ImmutableList<UserWalletItemUM>> = lce {
        val balances = withError(
            transform = { Error.UnableToGetBalances },
            block = {
                maybeBalances.bindOrNull().orEmpty()
                    .filterKeys { userWalletId -> wallets.any { it.walletId == userWalletId } }
                    .mapKeys { entry -> wallets.first { it.walletId == entry.key } }
            },
        )

        val appCurrency = withError(
            transform = { Error.UnableToGetAppCurrency },
            block = { maybeAppCurrency.toLce().bind() },
        )

        balances
            .map { (userWallet, balance) ->
                UserWalletItemUMConverter(
                    onClick = onWalletSelected,
                    appCurrency = appCurrency,
                    balance = balance,
                    isBalanceHidden = balanceHidingSettings.isBalanceHidden,
                    artwork = artworks[userWallet.walletId],
                )
                    .convert(userWallet)
            }
            .toImmutableList()
    }

    sealed class Error {

        data object UnableToGetAppCurrency : Error()

        data object UnableToGetBalances : Error()
    }
}
package com.tangem.features.onboarding.usedcard.alreadyactivated

import com.tangem.core.analytics.api.AnalyticsEventHandler
import com.tangem.core.analytics.models.Basic.SignedInLegacy
import com.tangem.core.analytics.models.Basic.SignedInLegacy.SignInType
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.card.analytics.ParamCardCurrencyConverter
import com.tangem.domain.card.common.util.cardTypesResolver
import com.tangem.domain.common.wallets.UserWalletsListRepository
import com.tangem.domain.common.wallets.error.SaveWalletError
import com.tangem.domain.models.scan.ScanResponse
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.models.wallet.isImported
import com.tangem.domain.redux.ReduxStateHolder
import com.tangem.domain.wallets.builder.ColdUserWalletBuilder
import com.tangem.domain.wallets.usecase.SaveWalletUseCase
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@Suppress("LongParameterList")
@ModelScoped
internal class AlreadyActivatedModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val coldUserWalletBuilderFactory: ColdUserWalletBuilder.Factory,
    private val saveWalletUseCase: SaveWalletUseCase,
    private val userWalletsListRepository: UserWalletsListRepository,
    private val reduxStateHolder: ReduxStateHolder,
    private val analyticsEventHandler: AnalyticsEventHandler,
) : Model() {

    private val params = paramsContainer.require<AlreadyActivatedComponent.Params>()

    val uiState: StateFlow<AlreadyActivatedUM>
        field = MutableStateFlow(
            AlreadyActivatedUM(
                isSavingWallet = false,
                onThisIsMyWalletClick = ::onThisIsMyWalletClick,
                onNewCardClick = ::onNewCardClick,
            ),
        )

    private fun onThisIsMyWalletClick() {
        proceedWithScanResponse(params.scanResponse)
    }

    private fun onNewCardClick() {
        // TODO [REDACTED_TASK_KEY]
    }

    private fun proceedWithScanResponse(scanResponse: ScanResponse) {
        modelScope.launch {
            uiState.update { it.copy(isSavingWallet = true) }

            val userWallet = coldUserWalletBuilderFactory.create(scanResponse = scanResponse).build()

            if (userWallet == null) {
                Timber.e("User wallet not created")
                uiState.update { it.copy(isSavingWallet = false) }
                return@launch
            }

            saveWalletUseCase(userWallet = userWallet).fold(
                ifLeft = { error ->
                    when (error) {
                        is SaveWalletError.DataError -> {
                            Timber.e(error.toString(), "Unable to save user wallet")
                        }
                        is SaveWalletError.WalletAlreadySaved -> {
                            userWalletsListRepository.unlock(
                                userWalletId = userWallet.walletId,
                                unlockMethod = UserWalletsListRepository.UnlockMethod.Scan(scanResponse),
                            ).onRight {
                                userWalletsListRepository.select(userWallet.walletId)
                            }
                        }
                    }
                },
                ifRight = {
                    reduxStateHolder.onUserWalletSelected(userWallet)
                    sendSignedInCardAnalyticsEvent(scanResponse, userWallet)
                },
            )

            uiState.update { it.copy(isSavingWallet = false) }
            params.modelCallback.onWalletSaved()
        }
    }

    private suspend fun sendSignedInCardAnalyticsEvent(scanResponse: ScanResponse, userWallet: UserWallet) {
        val currency = ParamCardCurrencyConverter().convert(value = scanResponse.cardTypesResolver)
        if (currency != null) {
            analyticsEventHandler.send(
                SignedInLegacy(
                    currency = currency,
                    batch = scanResponse.card.batchId,
                    signInType = SignInType.Card,
                    walletsCount = userWalletsListRepository.userWalletsSync().size.toString(),
                    isImported = userWallet.isImported(),
                    hasBackup = scanResponse.card.backupStatus?.isActive,
                ),
            )
        }
    }
}
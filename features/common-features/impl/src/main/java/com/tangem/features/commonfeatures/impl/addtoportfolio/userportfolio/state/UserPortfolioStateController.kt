package com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.state

import com.tangem.common.ui.userwallet.converter.WalletIconUMConverter
import com.tangem.domain.appcurrency.GetSelectedAppCurrencyUseCase
import com.tangem.domain.balancehiding.GetBalanceHidingSettingsUseCase
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.wallets.usecase.GetWalletIconUseCase
import com.tangem.features.commonfeatures.api.addtoportfolio.AddToPortfolioManager
import com.tangem.features.commonfeatures.api.addtoportfolio.AvailableToAddData
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.model.UserPortfolioUM
import com.tangem.features.commonfeatures.impl.addtoportfolio.userportfolio.transformer.UserPortfolioSectionsTransformer
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

internal class UserPortfolioStateController @AssistedInject constructor(
    getSelectedAppCurrencyUseCase: GetSelectedAppCurrencyUseCase,
    getBalanceHidingSettingsUseCase: GetBalanceHidingSettingsUseCase,
    private val getWalletIconUseCase: GetWalletIconUseCase,
    private val walletIconUMConverter: WalletIconUMConverter,
    @Assisted private val modelScope: CoroutineScope,
    @Assisted private val onTokenSelected: (AddToPortfolioManager.Result) -> Unit,
) {

    private val requiredDataFlow = MutableSharedFlow<Pair<AvailableToAddData, CryptoCurrency.RawID>>(
        replay = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    val uiState: StateFlow<UserPortfolioUM?> = combine(
        flow = requiredDataFlow.distinctUntilChanged(),
        flow2 = getSelectedAppCurrencyUseCase.invokeOrDefault(),
        flow3 = getBalanceHidingSettingsUseCase.isBalanceHidden(),
    ) { (allAvailableData, rawCurrencyId), appCurrency, isBalanceHidden ->
        UserPortfolioSectionsTransformer(
            availableData = allAvailableData,
            rawCurrencyId = rawCurrencyId,
            appCurrency = appCurrency,
            isBalanceHidden = isBalanceHidden,
            resolveWalletDeviceIcon = {
                walletIconUMConverter.convert(getWalletIconUseCase(it))
            },
            onTokenSelected = onTokenSelected,
        ).transform()
    }
        .distinctUntilChanged()
        .stateIn(modelScope, SharingStarted.Lazily, null)

    suspend fun updateAndWaitNotNullState(allAvailableData: AvailableToAddData, rawCurrencyId: CryptoCurrency.RawID) {
        requiredDataFlow.tryEmit(allAvailableData to rawCurrencyId)
        uiState.filterNotNull().firstOrNull()
    }

    @AssistedFactory
    interface Factory {
        fun create(
            modelScope: CoroutineScope,
            onTokenSelected: (AddToPortfolioManager.Result) -> Unit,
        ): UserPortfolioStateController
    }
}
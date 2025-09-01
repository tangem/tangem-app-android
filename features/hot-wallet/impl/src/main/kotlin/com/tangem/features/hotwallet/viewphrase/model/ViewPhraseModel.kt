package com.tangem.features.hotwallet.viewphrase.model

import androidx.compose.runtime.Stable
import arrow.core.getOrElse
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.decompose.navigation.Router
import com.tangem.core.ui.components.grid.entity.EnumeratedTwoColumnGridItem
import com.tangem.domain.models.wallet.UserWallet
import com.tangem.domain.wallets.usecase.ClearHotWalletContextualUnlockUseCase
import com.tangem.domain.wallets.usecase.ExportSeedPhraseUseCase
import com.tangem.domain.wallets.usecase.GetUserWalletUseCase
import com.tangem.features.hotwallet.ViewPhraseComponent
import com.tangem.features.hotwallet.viewphrase.entity.ViewPhraseUM
import com.tangem.hot.sdk.model.HotWalletId
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class ViewPhraseModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val router: Router,
    private val getUserWalletUseCase: GetUserWalletUseCase,
    private val exportSeedPhraseUseCase: ExportSeedPhraseUseCase,
    private val clearHotWalletContextualUnlockUseCase: ClearHotWalletContextualUnlockUseCase,
) : Model() {

    private val params = paramsContainer.require<ViewPhraseComponent.Params>()

    private var hotWalletId: HotWalletId? = null

    internal val uiState: StateFlow<ViewPhraseUM>
    field = MutableStateFlow(
        ViewPhraseUM(
            onBackClick = { router.pop() },
        ),
    )

    init {
        loadSeedPhrase()
    }

    private fun loadSeedPhrase() {
        val userWallet = getUserWalletUseCase(params.userWalletId)
            .getOrElse { error("User wallet with id ${params.userWalletId} not found") }
        if (userWallet is UserWallet.Hot) {
            hotWalletId = userWallet.hotWalletId
            modelScope.launch {
                val words = exportSeedPhraseUseCase.invoke(userWallet.hotWalletId)
                    .getOrElse { error("Unable to export seed phrase for wallet with id ${params.userWalletId}") }
                    .mnemonic
                    .mnemonicComponents
                uiState.update {
                    it.copy(
                        words = words.mapIndexed { index, s ->
                            EnumeratedTwoColumnGridItem(index + 1, s)
                        }.toImmutableList(),
                    )
                }
            }
        }
    }

    override fun onDestroy() {
        hotWalletId?.let { clearHotWalletContextualUnlockUseCase.invoke(it) }
        super.onDestroy()
    }
}
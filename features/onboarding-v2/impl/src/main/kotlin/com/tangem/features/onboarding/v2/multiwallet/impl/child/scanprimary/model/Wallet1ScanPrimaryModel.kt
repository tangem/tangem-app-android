package com.tangem.features.onboarding.v2.multiwallet.impl.child.scanprimary.model

import androidx.compose.runtime.Stable
import com.tangem.common.CompletionResult
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.domain.common.util.cardTypesResolver
import com.tangem.features.onboarding.v2.impl.R
import com.tangem.features.onboarding.v2.multiwallet.impl.child.MultiWalletChildParams
import com.tangem.sdk.api.BackupServiceHolder
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@ModelScoped
internal class Wallet1ScanPrimaryModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val backupServiceHolder: BackupServiceHolder,
) : Model() {

    private val params = paramsContainer.require<MultiWalletChildParams>()
    private val scanResponse = params.multiWalletState.value.currentScanResponse

    val isRing = scanResponse.cardTypesResolver.isRing()
    val onDone = MutableSharedFlow<Unit>()

    fun onScanPrimaryClick() {
        val backupService = backupServiceHolder.backupService.get() ?: return
        val iconScanRes = R.drawable.img_hand_scan_ring.takeIf { isRing }
        backupService.readPrimaryCard(iconScanRes = iconScanRes, cardId = scanResponse.card.cardId) { result ->
            when (result) {
                is CompletionResult.Success -> {
                    modelScope.launch { onDone.emit(Unit) }
                }
                is CompletionResult.Failure -> Unit
            }
        }
    }
}
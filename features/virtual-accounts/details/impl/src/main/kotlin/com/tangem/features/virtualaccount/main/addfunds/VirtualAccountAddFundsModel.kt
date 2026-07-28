package com.tangem.features.virtualaccount.main.addfunds

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.features.virtualaccount.details.component.VirtualAccountAddFundsBottomSheetComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class VirtualAccountAddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
) : Model() {

    private val params = paramsContainer.require<VirtualAccountAddFundsBottomSheetComponent.Params>()

    val uiState: StateFlow<VirtualAccountAddFundsUM>
        field = MutableStateFlow(
            VirtualAccountAddFundsUM(
                onDismiss = ::onDismiss,
                content = if (params.shouldSkipIntro) buildDetailsContent() else buildIntroContent(),
            ),
        )

    init {
        if (params.shouldSkipIntro) {
            // send analytics
            params.onDetailsShown()
        }
    }

    fun onDismiss() {
        params.listener.onAddFundsDismiss()
    }

    private fun buildIntroContent() = VirtualAccountAddFundsUM.Content.Intro(
        onShowDetailsClick = ::showDetailsContent,
    )

    private fun showDetailsContent() {
        params.onDetailsShown()
        uiState.update { state -> state.copy(content = buildDetailsContent()) }
    }

    private fun buildDetailsContent(): VirtualAccountAddFundsUM.Content.Details {
        return VirtualAccountAddFundsUM.Content.Details(
            items = params.requisites
                .map(::detailItem)
                .toImmutableList(),
            dailyLimit = params.dailyDepositLimit,
            onShareClick = {
                params.onShareClicked()
                shareManager.shareText(buildShareText())
            },
        )
    }

    private fun detailItem(
        requisitesRow: VirtualAccountAddFundsBottomSheetComponent.RequisitesRow,
    ): VirtualAccountAddFundsUM.DetailItem {
        return VirtualAccountAddFundsUM.DetailItem(
            label = requisitesRow.title,
            value = requisitesRow.value,
            onCopyClick = {
                params.onFieldCopied(requisitesRow.titleForShare)
                clipboardManager.setText(text = requisitesRow.value, isSensitive = true)
            },
        )
    }

    private fun buildShareText(): String {
        return buildString {
            params.requisites.fastForEach { item ->
                appendLine("${item.titleForShare}: ${item.value}")
            }
        }
    }
}
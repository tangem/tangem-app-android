package com.tangem.features.virtualaccount.main.addfunds

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.core.ui.extensions.stringReference
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

    fun onDismiss() {
        params.listener.onAddFundsDismiss()
    }

    private fun buildIntroContent() = VirtualAccountAddFundsUM.Content.Intro(
        onShowDetailsClick = ::showDetailsContent,
    )

    private fun showDetailsContent() {
        uiState.update { state -> state.copy(content = buildDetailsContent()) }
    }

    private fun buildDetailsContent() = VirtualAccountAddFundsUM.Content.Details(
        items = params.requisites
            .map { detailItem(label = it.title, value = it.value) }
            .toImmutableList(),
        dailyLimit = params.dailyDepositLimit,
        onShareClick = { shareManager.shareText(buildShareText()) },
    )

    private fun detailItem(label: String, value: String) = VirtualAccountAddFundsUM.DetailItem(
        label = stringReference(label),
        value = value,
        onCopyClick = { clipboardManager.setText(text = value, isSensitive = true) },
    )

    private fun buildShareText(): String {
        return buildString {
            params.requisites.fastForEach { item ->
                appendLine("${item.titleForShare}: ${item.value}")
            }
        }
    }
}
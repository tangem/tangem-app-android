package com.tangem.features.moneyaccount.main.addfunds

import androidx.compose.runtime.Stable
import androidx.compose.ui.util.fastForEach
import com.tangem.core.decompose.di.ModelScoped
import com.tangem.core.decompose.model.Model
import com.tangem.core.decompose.model.ParamsContainer
import com.tangem.core.navigation.share.ShareManager
import com.tangem.core.ui.clipboard.ClipboardManager
import com.tangem.features.moneyaccount.details.component.MoneyAccountAddFundsBottomSheetComponent
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@Stable
@ModelScoped
internal class MoneyAccountAddFundsModel @Inject constructor(
    paramsContainer: ParamsContainer,
    override val dispatchers: CoroutineDispatcherProvider,
    private val clipboardManager: ClipboardManager,
    private val shareManager: ShareManager,
) : Model() {

    private val params = paramsContainer.require<MoneyAccountAddFundsBottomSheetComponent.Params>()

    val uiState: StateFlow<MoneyAccountAddFundsUM>
        field = MutableStateFlow(
            MoneyAccountAddFundsUM(
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

    private fun buildIntroContent() = MoneyAccountAddFundsUM.Content.Intro(
        onShowDetailsClick = ::showDetailsContent,
    )

    private fun showDetailsContent() {
        params.onDetailsShown()
        uiState.update { state -> state.copy(content = buildDetailsContent()) }
    }

    private fun buildDetailsContent(): MoneyAccountAddFundsUM.Content.Details {
        return MoneyAccountAddFundsUM.Content.Details(
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
        requisitesRow: MoneyAccountAddFundsBottomSheetComponent.RequisitesRow,
    ): MoneyAccountAddFundsUM.DetailItem {
        return MoneyAccountAddFundsUM.DetailItem(
            key = requisitesRow.titleForShare,
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
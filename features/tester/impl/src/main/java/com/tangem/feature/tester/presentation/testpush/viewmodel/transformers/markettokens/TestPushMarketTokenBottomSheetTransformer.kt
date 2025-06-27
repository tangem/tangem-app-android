package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.domain.markets.TokenMarket
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMarketsTokenConfigUM
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.ImmutableList

internal class TestPushMarketTokenBottomSheetTransformer(
    private val item: ImmutableList<TokenMarket>,
    private val onDismissBottomSheet: () -> Unit,
    private val onSearchEdit: (TextFieldValue) -> Unit,
    private val onItemClick: (TokenMarket) -> Unit,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        val content = (prevState.bottomSheetConfig?.content as? TestPushMarketsTokenConfigUM)?.copy(
            itemList = item,
            onSearchValueEdit = onSearchEdit,
            onItemClick = onItemClick,
        ) ?: TestPushMarketsTokenConfigUM(
            itemList = item,
            searchValue = TextFieldValue(""),
            onSearchValueEdit = onSearchEdit,
            onItemClick = onItemClick,
        )
        return prevState.copy(
            bottomSheetConfig = prevState.bottomSheetConfig?.let {
                prevState.bottomSheetConfig.copy(
                    content = content,
                )
            } ?: TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onDismissBottomSheet,
                content = content,
            ),
        )
    }
}
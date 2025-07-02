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
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = onDismissBottomSheet,
                content = TestPushMarketsTokenConfigUM(
                    itemList = item,
                    searchValue = TextFieldValue(""),
                    onSearchValueEdit = onSearchEdit,
                    onItemClick = onItemClick,
                ),
            ),
        )
    }
}
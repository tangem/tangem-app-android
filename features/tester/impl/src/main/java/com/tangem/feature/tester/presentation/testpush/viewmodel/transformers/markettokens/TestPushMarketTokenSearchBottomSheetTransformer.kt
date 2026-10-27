package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMarketsTokenConfigUM
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal class TestPushMarketTokenSearchBottomSheetTransformer(
    private val searchValue: TextFieldValue,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        val bottomSheetConfig = prevState.bottomSheetConfig ?: return prevState
        val content = bottomSheetConfig.content as? TestPushMarketsTokenConfigUM ?: return prevState

        return prevState.copy(
            bottomSheetConfig = bottomSheetConfig.copy(
                content = content.copy(searchValue = searchValue),
            ),
        )
    }
}
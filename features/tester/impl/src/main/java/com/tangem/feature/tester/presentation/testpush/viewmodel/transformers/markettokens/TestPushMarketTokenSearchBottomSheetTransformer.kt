package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers.markettokens

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMarketsTokenConfigUM
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal class TestPushMarketTokenSearchBottomSheetTransformer(
    private val searchValue: TextFieldValue,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        val content = prevState.bottomSheetConfig?.content as? TestPushMarketsTokenConfigUM

        return prevState.copy(
            bottomSheetConfig = prevState.bottomSheetConfig?.copy(
                content = content?.copy(
                    searchValue = searchValue,
                ) ?: content as TangemBottomSheetConfigContent,
            ),
        )
    }
}
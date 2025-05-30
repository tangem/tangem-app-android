package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal class TestPushChangeTitleTransformer(
    val title: TextFieldValue,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        return prevState.copy(title = title)
    }
}
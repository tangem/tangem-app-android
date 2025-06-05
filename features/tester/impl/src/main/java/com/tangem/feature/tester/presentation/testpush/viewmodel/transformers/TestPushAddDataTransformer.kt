package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal object TestPushAddDataTransformer : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        return prevState.copy(
            data = prevState.data.plus(TextFieldValue("") to TextFieldValue("")),
        )
    }
}
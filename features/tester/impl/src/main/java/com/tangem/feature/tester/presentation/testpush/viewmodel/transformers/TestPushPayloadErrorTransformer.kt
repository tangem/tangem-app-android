package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers

import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal class TestPushPayloadErrorTransformer(
    val error: String,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        return prevState.copy(payloadError = error)
    }
}
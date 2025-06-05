package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers

import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal class TestPushRemoveDataTransformer(
    private val index: Int,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        return prevState.copy(
            data = prevState.data.toMutableList().apply {
                removeAt(index = index)
            },
        )
    }
}
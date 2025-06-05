package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal class TestPushAddKeyDataTransformer(
    private val index: Int,
    private val value: TextFieldValue,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        val mutableData = prevState.data.toMutableList()
        val updated = mutableData[index].copy(first = value)
        mutableData.set(index = index, updated)

        return prevState.copy(data = mutableData)
    }
}
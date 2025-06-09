package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers

import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer

internal object TestPushDismissBottomSheetTransformer : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        return prevState.copy(bottomSheetConfig = null)
    }
}
package com.tangem.feature.tester.presentation.testpush.viewmodel.transformers

import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig
import com.tangem.feature.tester.presentation.testpush.entity.TestPushMenuConfigUM
import com.tangem.feature.tester.presentation.testpush.entity.TestPushUM
import com.tangem.utils.transformer.Transformer
import kotlinx.collections.immutable.persistentListOf

internal class TestPushMenuTransformer(
    private val dismissBottomSheet: () -> Unit,
    private val onDeepLinkParamClick: (TestPushMenuConfigUM.PushMenu) -> Unit,
) : Transformer<TestPushUM> {
    override fun transform(prevState: TestPushUM): TestPushUM {
        return prevState.copy(
            bottomSheetConfig = TangemBottomSheetConfig(
                isShown = true,
                onDismissRequest = dismissBottomSheet,
                content = TestPushMenuConfigUM(
                    itemList = persistentListOf(
                        TestPushMenuConfigUM.PushMenu.MarketTokenDetails,
                    ),
                    onItemClick = onDeepLinkParamClick,
                ),
            ),
        )
    }
}
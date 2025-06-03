package com.tangem.feature.tester.presentation.testpush.entity

import androidx.compose.ui.text.input.TextFieldValue
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfig

internal data class TestPushUM(
    val fcmToken: String,
    val title: TextFieldValue,
    val message: TextFieldValue,
    val data: List<Pair<TextFieldValue, TextFieldValue>>,
    val bottomSheetConfig: TangemBottomSheetConfig?,
)
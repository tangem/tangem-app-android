package com.tangem.feature.tester.presentation.testpush.entity

import androidx.compose.ui.text.input.TextFieldValue

internal data class TestPushUM(
    val fcmToken: String,
    val title: TextFieldValue,
    val message: TextFieldValue,
    val data: List<Pair<TextFieldValue, TextFieldValue>>,
)
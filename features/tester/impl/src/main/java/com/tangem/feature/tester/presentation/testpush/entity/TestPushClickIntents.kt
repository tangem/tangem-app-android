package com.tangem.feature.tester.presentation.testpush.entity

import androidx.compose.ui.text.input.TextFieldValue

interface TestPushClickIntents {
    fun onTitleChange(value: TextFieldValue)
    fun onMessageChange(value: TextFieldValue)

    fun onDataKeyChange(index: Int, value: TextFieldValue)
    fun onDataValueChange(index: Int, value: TextFieldValue)
    fun onDataAdd()
    fun onDataRemove(index: Int)

    fun onDeeplinkParamMenu()

    fun onBackClick()
    fun onSendPush()
}
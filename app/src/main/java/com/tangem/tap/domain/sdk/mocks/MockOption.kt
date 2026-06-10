package com.tangem.tap.domain.sdk.mocks

import androidx.appcompat.app.AppCompatActivity

class MockOption(
    val title: String,
    val resolve: suspend (AppCompatActivity) -> MockContent?,
)
package com.tangem.helpers.base

import com.tangem.tap.TapApplication
import dagger.hilt.android.testing.CustomTestApplication

@CustomTestApplication(TapApplication::class)
internal class TestApplication : TapApplication()
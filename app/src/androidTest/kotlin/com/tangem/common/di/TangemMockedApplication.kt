package com.tangem.common.di

import com.tangem.common.TangemEmptyApplication
import dagger.hilt.android.testing.CustomTestApplication

@CustomTestApplication(TangemEmptyApplication::class)
internal class TangemMockedApplication
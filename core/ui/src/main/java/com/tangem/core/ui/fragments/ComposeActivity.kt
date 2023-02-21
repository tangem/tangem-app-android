package com.tangem.core.ui.fragments

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

abstract class ComposeActivity<ScreenState> : AppCompatActivity(), ComposeScreen<ScreenState> {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(createComposeView(context = this))
    }
}

package com.tangem.tap

import android.os.Bundle
import android.view.View
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentManager.FragmentLifecycleCallbacks

class NavBarInsetsFragmentLifecycleCallback : FragmentLifecycleCallbacks() {
    override fun onFragmentViewCreated(
        fm: FragmentManager,
        f: Fragment,
        v: View,
        savedInstanceState: Bundle?,
    ) {
        if (v is ComposeView) return

        ViewCompat.setOnApplyWindowInsetsListener(v) { view, windowInsets ->
            val statusBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val navigationBarInsets = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars())

            if (view.fitsSystemWindows) {
                view.updatePadding(
                    top = statusBarInsets.top,
                    bottom = navigationBarInsets.bottom,
                )
            } else {
                view.updatePadding(
                    bottom = navigationBarInsets.bottom,
                )
            }
            windowInsets
        }
    }
}

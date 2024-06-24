package com.tangem.tap.common.extensions

import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.tangem.utils.Provider
import com.tangem.wallet.R
import timber.log.Timber

fun FragmentManager.showFragmentAllowingStateLoss(name: String, fragmentProvider: Provider<Fragment>) {
    Timber.d("Showing $name route")

    val isPoppedBack = popBackStackImmediate(name, 0)

    if (!isPoppedBack) {
        val fragment = fragmentProvider()

        if (fragment is DialogFragment) {
            fragment.showDialog(fragmentManager = this, name)
        } else {
            fragment.showFragment(fragmentManager = this, name)
        }

        Timber.d("Route $name is shown")
    } else {
        Timber.d("Route $name is found in backstack and shown")
    }
}

private fun DialogFragment.showDialog(fragmentManager: FragmentManager, name: String) {
    val transaction = fragmentManager.beginTransaction()

    try {
        transaction.addToBackStack(name)
        show(transaction, name)
    } catch (e: IllegalStateException) {
        transaction.add(this, name)
        transaction.addToBackStack(name)

        transaction.commitAllowingStateLoss()
    }
}

private fun Fragment.showFragment(fragmentManager: FragmentManager, name: String) {
    val transaction = fragmentManager.beginTransaction()

    transaction.replace(R.id.fragment_container, this, name)
    transaction.addToBackStack(name)

    transaction.commitAllowingStateLoss()
}

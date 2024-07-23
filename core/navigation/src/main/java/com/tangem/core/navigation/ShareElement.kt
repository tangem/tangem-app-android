package com.tangem.core.navigation

import android.view.View
import java.lang.ref.WeakReference

/**
 * For ease of use, the name is used as transitionName\name into the FragmentTransaction.addSharedElement
 */
class ShareElement(view: View, name: String? = null) {

    val wView: WeakReference<View>
    val elementName: String

    init {
        name?.let { view.transitionName = it }
        elementName = view.transitionName
            ?: throw UnsupportedOperationException("ShareElement require the name")
        wView = WeakReference(view)
    }

    companion object {
        const val imvFrontCard = "imv_front_card"
        const val imvBackCard = "imv_back_card"
    }
}

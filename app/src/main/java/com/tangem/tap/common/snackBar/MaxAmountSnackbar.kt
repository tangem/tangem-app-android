package com.tangem.tap.common.snackBar

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.ContentViewCallback
import com.google.android.material.snackbar.Snackbar
import com.tangem.wallet.R

/**
[REDACTED_AUTHOR]
 */
class MaxAmountSnackbar(
        parent: ViewGroup,
        content: MaxAmountSnackbarView
) : BaseTransientBottomBar<MaxAmountSnackbar>(parent, content, content) {

    companion object {

        fun make(view: View, onClick: () -> Unit): MaxAmountSnackbar {
            val parent = view.findSuitableParent() ?: throw IllegalArgumentException(
                    "No suitable parent found from the given view. Please provide a valid view."
            )
            val inflater = LayoutInflater.from(view.context)
            val customView = inflater.inflate(R.layout.view_snackbar_max_amount, parent, false) as MaxAmountSnackbarView
            customView.setOnClickListener { onClick() }

            return MaxAmountSnackbar(parent, customView).apply {
                duration = Snackbar.LENGTH_INDEFINITE
            }
        }

        private fun View?.findSuitableParent(): ViewGroup? {
            var view = this
            var fallback: ViewGroup? = null
            do {
                if (view is CoordinatorLayout) {
                    return view
                } else if (view is FrameLayout) {
                    if (view.id == android.R.id.content) {
                        return view
                    } else {
                        fallback = view
                    }
                }

                if (view != null) {
                    val parent = view.parent
                    view = if (parent is View) parent else null
                }
            } while (view != null)
            return fallback
        }

    }
}

class MaxAmountSnackbarView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr), ContentViewCallback {

    init {
        View.inflate(context, R.layout.view_snackbar_max_amount_content, this)
        clipToPadding = false
    }


    override fun animateContentIn(delay: Int, duration: Int) {
    }

    override fun animateContentOut(delay: Int, duration: Int) {
    }
}

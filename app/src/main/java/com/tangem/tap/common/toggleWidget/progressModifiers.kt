package com.tangem.tap.common.toggleWidget

import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.TextViewCompat
import com.google.android.material.button.MaterialButton
import com.tangem.merchant.common.toggleWidget.StateModifier
import com.tangem.merchant.common.toggleWidget.ToggleState
import com.tangem.tap.common.extensions.beginDelayedTransition

/**
* [REDACTED_AUTHOR]
 */

sealed class ProgressState : ToggleState {
    class Progress : ProgressState()
    class None : ProgressState()
}

class ReplaceTextStateModifier(
        private val initialText: String,
        private val replaceText: String = ""
) : StateModifier {

    override fun stateChanged(container: ViewGroup, view: View, state: ToggleState) {
        val tv = view as? TextView ?: return

        when (state) {
            is ProgressState.Progress -> {
                tv.text = replaceText
            }
            is ProgressState.None -> {
                tv.text = initialText
            }
        }
    }
}

class TextViewDrawableStateModifier(
        private val initialDrawable: Drawable?,
        private val replaceDrawable: Drawable?,
        private val position: Int
) : StateModifier {
    companion object {
        val LEFT = 1
        val RIGHT = 2
    }

    override fun stateChanged(container: ViewGroup, view: View, state: ToggleState) {
        val drawable = when (state) {
            is ProgressState.Progress -> replaceDrawable
            is ProgressState.None -> initialDrawable
            else -> null
        }
        val drawableChanger = getChanger(view)
        drawableChanger?.change(drawable, position)
    }

    private fun getChanger(view: View): DrawableChanger? = when (view) {
        is MaterialButton -> MaterialButtonChanger(view)
        is Button -> TextViewChanger(view)
        is TextView -> TextViewChanger(view)
        else -> null
    }

    internal interface DrawableChanger {
        fun change(drawable: Drawable?, position: Int)
    }

    internal class TextViewChanger(private val view: TextView) : DrawableChanger {
        override fun change(drawable: Drawable?, position: Int) {
            when (position) {
                LEFT -> setLeft(drawable)
                RIGHT -> setRight(drawable)
            }
        }

        private fun setLeft(drawable: Drawable?) {
            if (drawable == null) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(view, 0, 0, 0, 0)
            } else {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(view, drawable, null, null, null)
            }
        }

        private fun setRight(drawable: Drawable?) {
            if (drawable == null) {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(view, 0, 0, 0, 0)
            } else {
                TextViewCompat.setCompoundDrawablesRelativeWithIntrinsicBounds(view, null, null, drawable, null)
            }
        }
    }

    internal class MaterialButtonChanger(private val view: MaterialButton) : DrawableChanger {
        override fun change(drawable: Drawable?, position: Int) {
            when (position) {
                LEFT -> setLeft(drawable)
                RIGHT -> setRight(drawable)
            }
        }

        private fun setLeft(drawable: Drawable?) {
            view.iconGravity = MaterialButton.ICON_GRAVITY_START
            view.icon = drawable
        }

        private fun setRight(drawable: Drawable?) {
            view.iconGravity = MaterialButton.ICON_GRAVITY_END
            view.icon = drawable
        }
    }
}

class ShowHideStateModifier(
        private val isShowOnLoading: Boolean = true,
        private val typeOfHiding: Int = View.INVISIBLE
) : StateModifier {

    override fun stateChanged(container: ViewGroup, view: View, state: ToggleState) {
        container.beginDelayedTransition()
        view.visibility = when (state) {
            is ProgressState.Progress -> if (isShowOnLoading) View.VISIBLE else typeOfHiding
            is ProgressState.None -> if (isShowOnLoading) typeOfHiding else View.VISIBLE
            else -> return
        }
    }
}

class ClickableStateModifier(
        private val isClickableOnLoading: Boolean = false
) : StateModifier {

    override fun stateChanged(container: ViewGroup, view: View, state: ToggleState) {
        view.isClickable = when (state) {
            is ProgressState.Progress -> isClickableOnLoading
            is ProgressState.None -> !isClickableOnLoading
            else -> return

        }
    }
}
package com.tangem.tangemtest._arch.widget.abstraction

import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.ColorRes
import com.tangem.tangemtest.R
import com.tangem.tangemtest._arch.structure.StringId
import com.tangem.tangemtest._arch.structure.StringResId
import com.tangem.tangemtest._arch.structure.abstraction.Item
import ru.dev.gbixahue.eu4d.lib.android._android.views.colorFrom
import ru.dev.gbixahue.eu4d.lib.kotlin.common.LayoutHolder

/**
[REDACTED_AUTHOR]
 */
interface ViewWidget : LayoutHolder {
    val view: View
    var item: Item

    fun getName(): String
    fun setBackgroundColor(@ColorRes colorId: Int?)
}

abstract class BaseViewWidget(
        parent: ViewGroup,
        override var item: Item
) : ViewWidget {

    override val view: View = inflate(getLayoutId(), parent)

    private var defaultBackground: Drawable? = view.background

    init {
        if (item.viewModel.viewState.isHidden) {
            view.visibility = View.GONE
        } else {
            setBackgroundColor(item.viewModel.viewState.backgroundColor)
        }
    }

    override fun getName(): String {
        return when (val id = item.id) {
            is StringId -> id.value
            is StringResId -> view.resources.getString(id.value)
            else -> view.resources.getString(R.string.unknown)
        }
    }

    override fun setBackgroundColor(colorId: Int?) {
        val background = when {
            colorId == null -> null
            colorId == -1 -> defaultBackground
            else -> ColorDrawable(view.colorFrom(colorId))
        }
        view.background = background
    }
}

internal fun inflate(id: Int, parent: ViewGroup): View {
    val layoutId = if (id <= 0) R.layout.w_empty else id
    val inflatedView = LayoutInflater.from(parent.context).inflate(layoutId, parent, false)
    parent.addView(inflatedView)
    return inflatedView
}
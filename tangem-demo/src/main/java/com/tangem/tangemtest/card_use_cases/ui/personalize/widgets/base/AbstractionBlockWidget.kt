package com.tangem.tangemtest.card_use_cases.ui.personalize.widgets.base

import android.view.View
import android.view.ViewGroup
import com.tangem.tangemtest.R
import ru.dev.gbixahue.eu4d.lib.android._android.views.inflate

/**
[REDACTED_AUTHOR]
 */
interface BlockWidget : ViewWidget

class EmptyWidget(parent: ViewGroup) : BlockWidget {
    override val view: View = parent.inflate(getLayoutId(), parent, false)
    override fun getLayoutId(): Int = R.layout.w_empty
}

abstract class BaseBlockWidget<D>(parent: ViewGroup) : BaseViewWidget(parent), BlockWidget
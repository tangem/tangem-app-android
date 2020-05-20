package com.tangem.devkit.ucase.variants.depersonalize.ui

import android.os.Bundle
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.tangem.commands.Card
import com.tangem.devkit.R
import com.tangem.devkit.ucase.domain.paramsManager.ItemsManager
import com.tangem.devkit.ucase.domain.paramsManager.managers.DepersonalizeItemsManager
import com.tangem.devkit.ucase.ui.BaseCardActionFragment
import com.tangem.devkit.ucase.variants.responses.ui.ResponseFragment
import ru.dev.gbixahue.eu4d.lib.android._android.views.dpToPx

/**
[REDACTED_AUTHOR]
 */
class DepersonalizeActionFragment : BaseCardActionFragment() {

    override val itemsManager: ItemsManager by lazy { DepersonalizeItemsManager() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val howToUseView = createHowToUse()
        contentContainer.layoutParams = FrameLayout.LayoutParams(-1, -1, Gravity.CENTER)
        contentContainer.addView(howToUseView)
    }

    private fun createHowToUse(): View {
        val fl = FrameLayout(requireContext())
        fl.layoutParams = FrameLayout.LayoutParams(-1, -1, Gravity.CENTER)
        val tv = TextView(requireContext()).apply {
            val padding = dpToPx(16f).toInt()
            setPadding(padding, 0, padding, 0)
            gravity = Gravity.CENTER
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
            setText(R.string.htu_depersonalize_action)
        }
        fl.addView(tv)
        return fl
    }

    override fun initViews() {
        swrLayout.isEnabled = false
        actionFab.setOnClickListener { actionVM.invokeMainAction() }
    }

    override fun handleResponseCardData(card: Card) {
        super.handleResponseCardData(card)
        val bundle = ResponseFragment.setTittle(R.string.fg_name_response_depersonalization)
        navigateTo(R.id.action_nav_card_action_to_response_screen, bundle, null)
    }
}
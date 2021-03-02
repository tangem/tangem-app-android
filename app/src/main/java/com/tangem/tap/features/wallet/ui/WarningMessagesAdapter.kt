package com.tangem.tap.features.wallet.ui

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.play.core.review.ReviewManagerFactory
import com.tangem.tap.common.analytics.AnalyticsEvent
import com.tangem.tap.common.analytics.FirebaseAnalyticsHandler
import com.tangem.tap.common.extensions.getString
import com.tangem.tap.common.extensions.hide
import com.tangem.tap.common.extensions.show
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.features.feedback.RateCanBeBetterEmail
import com.tangem.tap.features.wallet.redux.WalletAction
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.layout_warning.view.*

class WarningMessagesAdapter : ListAdapter<WarningMessage, WarningMessageVH>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningMessageVH {
        val inflater = LayoutInflater.from(parent.context)
        val layout = inflater.inflate(R.layout.layout_warning, parent, false)
        return WarningMessageVH(layout)
    }

    override fun onBindViewHolder(holder: WarningMessageVH, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<WarningMessage>() {
        override fun areContentsTheSame(oldItem: WarningMessage, newItem: WarningMessage) = oldItem == newItem

        override fun areItemsTheSame(oldItem: WarningMessage, newItem: WarningMessage) = oldItem == newItem
    }
}

class WarningMessageVH(val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(warning: WarningMessage) {
        setBgColor(warning.priority)
        setText(warning)
        setupControlButtons(warning)
    }

    private fun setText(warning: WarningMessage) {
        fun getString(resId: Int?, default: String) = if (resId == null) default else view.getString(resId)

        view.tv_title.text = getString(warning.titleResId, warning.title)
        view.tv_message.text = getString(warning.messageResId, warning.message)
    }

    private fun setBgColor(priority: WarningMessage.Priority) {
        val color = when (priority) {
            WarningMessage.Priority.Info -> R.color.warning_info
            WarningMessage.Priority.Warning -> R.color.warning_warning
            WarningMessage.Priority.Critical -> R.color.warning_critical
        }
        view.card_view.setCardBackgroundColor(view.context.resources.getColor(color))
    }

    private fun setupControlButtons(warning: WarningMessage) {
        when (warning.type) {
            WarningMessage.Type.Permanent -> {
                view.group_controls_temporary.hide()
                view.group_controls_rating.hide()
            }
            WarningMessage.Type.Temporary -> {
                view.group_controls_rating.hide()
                view.group_controls_temporary.show()
                view.btn_got_it.setOnClickListener { store.dispatch(GlobalAction.HideWarningMessage(warning)) }
            }
            WarningMessage.Type.AppRating -> {
                view.group_controls_temporary.hide()
                view.group_controls_rating.show()
                view.btn_close.setOnClickListener {
                    FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.APP_RATING_DISMISS)
                    store.dispatch(GlobalAction.HideWarningMessage(warning))
                    store.dispatch(WalletAction.Warnings.AppRating.RemindLater)
                }
                view.btn_can_be_better.setOnClickListener {
                    FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.APP_RATING_NEGATIVE)
                    store.dispatch(GlobalAction.HideWarningMessage(warning))
                    store.dispatch(GlobalAction.SendFeedback(RateCanBeBetterEmail()))
                }
                store.dispatch(WalletAction.Warnings.AppRating.SetNeverToShow)
                view.btn_really_cool.setOnClickListener {
                    FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.APP_RATING_POSITIVE)
                    val context = view.context
                    val reviewManager = ReviewManagerFactory.create(context)
                    val flow = reviewManager.requestReviewFlow()
                    flow.addOnCompleteListener {
                        if (it.isSuccessful) {
//                            val info = it.result
//                            Toast.makeText(context, "success", Toast.LENGTH_SHORT).show()
                        } else {
//                            Toast.makeText(context, "fail", Toast.LENGTH_SHORT).show()
                        }
                    }.addOnFailureListener {
//                        Toast.makeText(context, "failure", Toast.LENGTH_SHORT).show()
                    }
                    store.dispatch(GlobalAction.HideWarningMessage(warning))
                }
                store.dispatch(WalletAction.Warnings.AppRating.SetNeverToShow)
            }
        }
    }
}

class SpacesItemDecoration(private val spacePx: Int) : ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        outRect.left = spacePx
        outRect.right = spacePx

        outRect.top = spacePx / 2
        outRect.top = spacePx / 2
    }
}

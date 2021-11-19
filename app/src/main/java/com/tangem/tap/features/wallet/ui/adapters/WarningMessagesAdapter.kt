package com.tangem.tap.features.wallet.ui.adapters

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
import com.tangem.tap.common.extensions.getActivity
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
import timber.log.Timber

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
        override fun areContentsTheSame(oldItem: WarningMessage, newItem: WarningMessage) =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: WarningMessage, newItem: WarningMessage) =
            oldItem == newItem
    }
}

class WarningMessageVH(val view: View) : RecyclerView.ViewHolder(view) {

    fun bind(warning: WarningMessage) {
        setBgColor(warning.priority)
        setText(warning)
        setupControlButtons(warning)
    }

    private fun setText(warning: WarningMessage) {
        fun getString(resId: Int?, default: String, formatArgs: String? = null) =
            if (resId == null) default else view.context.getString(resId, formatArgs)

        view.tv_title.text = getString(warning.titleResId, warning.title)
        view.tv_message.text = getString(
            resId = warning.messageResId, default = warning.message, formatArgs = warning.messageFormatArg
        )
    }

    private fun setBgColor(priority: WarningMessage.Priority) {
        val color = when (priority) {
            WarningMessage.Priority.Info -> R.color.warning_info
            WarningMessage.Priority.Warning -> R.color.warning_warning
            WarningMessage.Priority.Critical -> R.color.warning_critical
        }
        view.card_view.setCardBackgroundColor(view.context.resources.getColor(color))
    }

    private fun setupControlButtons(warning: WarningMessage) = when (warning.type) {
        WarningMessage.Type.Permanent, WarningMessage.Type.TestCard -> {
            view.group_controls_temporary.hide()
            view.group_controls_rating.hide()
        }
        WarningMessage.Type.Temporary -> {
            view.group_controls_rating.hide()
            view.group_controls_temporary.show()
            val buttonAction =
                when (warning.titleResId) {
                    R.string.warning_important_security_info -> {
                        View.OnClickListener {
                            store.dispatch(WalletAction.ShowDialog.SignedHashesMultiWalletDialog)
                        }
                    }
                    else -> {
                        View.OnClickListener {
                            store.dispatch(GlobalAction.HideWarningMessage(warning))
                        }
                    }
                }
            val buttonTitle = view.getString(
                warning.buttonTextId ?: R.string.how_to_got_it_button
            )
            view.btn_got_it.setOnClickListener (buttonAction)
            view.btn_got_it.text = buttonTitle
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
                store.dispatch(WalletAction.Warnings.AppRating.SetNeverToShow)
                store.dispatch(GlobalAction.HideWarningMessage(warning))
                store.dispatch(GlobalAction.SendFeedback(RateCanBeBetterEmail()))
            }
            view.btn_really_cool.setOnClickListener {
                val activity = view.context.getActivity() ?: return@setOnClickListener

                FirebaseAnalyticsHandler.triggerEvent(AnalyticsEvent.APP_RATING_POSITIVE)
                store.dispatch(WalletAction.Warnings.AppRating.SetNeverToShow)
                val reviewManager = ReviewManagerFactory.create(activity)
                val task = reviewManager.requestReviewFlow()
                task.addOnCompleteListener {
                    if (it.isSuccessful) {
                        val reviewFlow = reviewManager.launchReviewFlow(activity, it.result)
                        reviewFlow.addOnCompleteListener {
                            if (it.isSuccessful) {
                                // send review was succeed
                            } else {
                                // send fails
                            }
                        }
                    } else {
                        Timber.e(task.exception)
                    }
                }.addOnFailureListener {
                    Timber.e(it)
                }
                store.dispatch(GlobalAction.HideWarningMessage(warning))
            }
        }
    }
}

class SpacesItemDecoration(private val spacePx: Int) : ItemDecoration() {
    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        outRect.left = spacePx
        outRect.right = spacePx

        outRect.top = spacePx / 2
        outRect.top = spacePx / 2
    }
}
package com.tangem.tap.features.send.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.google.android.play.core.review.ReviewManagerFactory
import com.tangem.core.analytics.Analytics
import com.tangem.tap.common.analytics.events.AnalyticsParam
import com.tangem.tap.common.analytics.events.MainScreen
import com.tangem.tap.common.extensions.*
import com.tangem.tap.common.redux.global.GlobalAction
import com.tangem.tap.domain.configurable.warningMessage.WarningMessage
import com.tangem.tap.store
import com.tangem.wallet.R
import com.tangem.wallet.databinding.LayoutWarningCardActionBinding
import timber.log.Timber

// TODO: Delete with SendFeatureToggles
@Deprecated(message = "Used only in old send screen")
class WarningMessagesAdapter : ListAdapter<WarningMessage, WarningMessageVH>(DiffUtilCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WarningMessageVH {
        val binding = LayoutWarningCardActionBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return WarningMessageVH(binding)
    }

    override fun onBindViewHolder(holder: WarningMessageVH, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<WarningMessage>() {
        override fun areContentsTheSame(oldItem: WarningMessage, newItem: WarningMessage) = oldItem == newItem

        override fun areItemsTheSame(oldItem: WarningMessage, newItem: WarningMessage) = oldItem == newItem
    }
}

class WarningMessageVH(val binding: LayoutWarningCardActionBinding) : RecyclerView.ViewHolder(binding.root) {

    fun bind(warning: WarningMessage) {
        setBgColor(warning.priority)
        setText(warning)
        setupControlButtons(warning)
    }

    private fun setText(warning: WarningMessage) = with(binding.warningContentContainer) {
        fun getString(resId: Int?, default: String, formatArgs: String? = null) =
            if (resId == null) default else root.getString(resId, formatArgs)

        tvTitle.text = getString(
            resId = warning.titleResId,
            default = warning.title,
            formatArgs = warning.titleFormatArg,
        )

        tvMessage.text = getString(
            resId = warning.messageResId,
            default = warning.message,
            formatArgs = warning.messageFormatArg,
        )
    }

    private fun setBgColor(priority: WarningMessage.Priority) {
        val color = when (priority) {
            WarningMessage.Priority.Info -> R.color.warning_info
            WarningMessage.Priority.Warning -> R.color.warning_warning
            WarningMessage.Priority.Critical -> R.color.warning_critical
        }
        binding.warningCardAction.setCardBackgroundColor(binding.root.getColor(color))
    }

    @Suppress("LongMethod")
    private fun setupControlButtons(warning: WarningMessage) = when (warning.type) {
        WarningMessage.Type.Permanent, WarningMessage.Type.TestCard -> {
            binding.groupControlsTemporary.hide()
            binding.btnClose.hide()
        }
        WarningMessage.Type.Temporary -> {
            binding.groupControlsTemporary.show()
            binding.btnClose.hide()

            val buttonAction = View.OnClickListener {
                store.dispatch(GlobalAction.HideWarningMessage(warning))
            }
            val buttonTitle = binding.root.getString(
                warning.buttonTextId ?: R.string.how_to_got_it_button,
            )
            binding.btnGotIt.setOnClickListener(buttonAction)
            binding.btnGotIt.text = buttonTitle
        }
        WarningMessage.Type.AppRating -> {
            binding.groupControlsTemporary.hide()
            binding.btnClose.show()
            binding.btnClose.setOnClickListener {
                Analytics.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Closed))
                store.dispatch(GlobalAction.HideWarningMessage(warning))
            }
            binding.btnReallyCool.setOnClickListener {
                val activity = binding.root.context.getActivity() ?: return@setOnClickListener

                Analytics.send(MainScreen.NoticeRateAppButton(AnalyticsParam.RateApp.Liked))
                val reviewManager = ReviewManagerFactory.create(activity)
                val task = reviewManager.requestReviewFlow()
                task.addOnCompleteListener {
                    if (!it.isSuccessful) {
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
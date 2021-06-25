package com.tangem.tap.features.details.ui.walletconnect

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectAction
import com.tangem.tap.features.details.redux.walletconnect.WalletConnectSession
import com.tangem.tap.store
import com.tangem.wallet.R
import kotlinx.android.synthetic.main.item_wallet_connect_session.view.*

class WalletConnectSessionsAdapter
    : ListAdapter<WalletConnectSession, WalletConnectSessionsAdapter.SessionsViewHolder>(
    DiffUtilCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SessionsViewHolder {
        val layout = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wallet_connect_session, parent, false)
        return SessionsViewHolder(layout)
    }

    override fun onBindViewHolder(holder: SessionsViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    object DiffUtilCallback : DiffUtil.ItemCallback<WalletConnectSession>() {
        override fun areContentsTheSame(
            oldItem: WalletConnectSession, newItem: WalletConnectSession,
        ) = oldItem == newItem

        override fun areItemsTheSame(
            oldItem: WalletConnectSession, newItem: WalletConnectSession,
        ) = oldItem == newItem
    }

    class SessionsViewHolder(val view: View) :
        RecyclerView.ViewHolder(view) {

        fun bind(session: WalletConnectSession) {
            view.tv_card_id.text = view.context.getString(
                R.string.wallet_connect_card_number, session.wallet.cardId
            )
            view.tv_d_app_name.text = session.peerMeta.name

            view.btn_disconnect.setOnClickListener {
                store.dispatch(WalletConnectAction.DisconnectSession(session.session))
            }

        }
    }
}

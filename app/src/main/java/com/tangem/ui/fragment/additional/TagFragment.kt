package com.tangem.ui.fragment.additional

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.Html
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.tangem.data.network.ServerApiCommon
import com.tangem.server_android.ServerApiTangem
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.fragment.wallet.LoadedWalletFragment
import com.tangem.ui.fragment.wallet.LoadedWalletViewModel
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.util.LOG
import com.tangem.util.UtilHelper
import com.tangem.wallet.*
import com.tangem.wallet.xlmtag.XlmTagEngine
import kotlinx.android.synthetic.main.fr_loaded_wallet.*
import kotlinx.android.synthetic.main.layout_btn_details.*
import kotlinx.android.synthetic.main.layout_tangem_card.*


class TagFragment : BaseFragment(), NavigationResultListener {
    override val layoutId = R.layout.fragment_tag
    private lateinit var viewModel: LoadedWalletViewModel
    private lateinit var ctx: TangemContext
    private var serverApiCommon: ServerApiCommon = ServerApiCommon()
    private var serverApiTangem: ServerApiTangem = ServerApiTangem()

    private var requestCounter: Int = 0
        set(value) {
            field = value
            LOG.i(LoadedWalletFragment.TAG, "requestCounter, set $field")
            if (field <= 0) {
                LOG.e(LoadedWalletFragment.TAG, "+++++++++++ FINISH REFRESH")
                if (srl != null && srl.isRefreshing)
                    srl.isRefreshing = false
            } else if (srl != null && !srl.isRefreshing)
                srl.isRefreshing = true
        }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = TangemContext.loadFromBundle(context, arguments)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val engine = XlmTagEngine(ctx)


        btnLoad.visibility = View.GONE
        btnDetails.visibility = View.GONE
        btnExtract.text = getString(R.string.tag_claim)
        btnExtract.isEnabled = false //TODO: enable when we implement extraction
        btnExtract.backgroundTintList =
                ContextCompat.getColorStateList(requireContext(), R.color.btn_dark)

        ivTangemCard.setImageResource(R.drawable.card_default_nft)

        tvBalance.setSingleLine(!engine.needMultipleLinesForBalance())
        tvWallet.text = ctx.coinData.wallet
        tvWallet.setOnClickListener { shareWallet() }
        btnExplore.setOnClickListener { startActivity(Intent(Intent.ACTION_VIEW, engine.walletExplorerUri)) }
        btnCopy.setOnClickListener { shareWallet() }
        btnNewScan.setOnClickListener { navigateUp() }
        srl?.setOnRefreshListener { refresh(true) }

        requestBalanceAndUnspentTransactions()
//        update()
    }


    private fun shareWallet() {
        val txtShare = ctx.coinData.wallet
        val clipboard = activity?.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.primaryClip = ClipData.newPlainText(txtShare, txtShare)
        Toast.makeText(activity, R.string.loaded_wallet_toast_copied, Toast.LENGTH_LONG).show()
    }


    private fun update() {
        ctx.coinData.setIsBalanceEqual(true)

        if (srl.isRefreshing) {
            tvBalanceLine1.setTextColor(resources.getColor(R.color.primary))
            tvBalanceLine1.text = getString(R.string.loaded_wallet_verifying_in_blockchain)
            tvBalanceLine2.text = ""
            tvBalance.text = ""
            tvBalanceEquivalent.text = ""
        } else {
            val validator = BalanceValidator()
            validator.check(ctx, false)
            context?.let { ContextCompat.getColor(it, validator.color) }?.let { tvBalanceLine1?.setTextColor(it) }
            tvBalanceLine1?.text = getString(validator.firstLine)
            tvBalanceLine2?.text = getString(validator.getSecondLine(false))
        }

        val engine = CoinEngineFactory.create(ctx)
        when {
            engine!!.hasBalanceInfo() -> {
                @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.fromHtml(engine.balanceHTML, Html.FROM_HTML_MODE_LEGACY)
                else
                    Html.fromHtml(engine.balanceHTML)
                tvBalance.text = html
                tvBalanceEquivalent.text = engine.balanceEquivalent
            }

            ctx.card?.offlineBalance != null -> {
                @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                    Html.fromHtml(engine.offlineBalanceHTML, Html.FROM_HTML_MODE_LEGACY)
                else
                    Html.fromHtml(engine.offlineBalanceHTML)
                tvBalance.text = html
            }

            else -> tvBalance.text = ""
        }

        if (ctx.card!!.tokenSymbol.length > 1) {
            @Suppress("DEPRECATION") val html = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                Html.fromHtml(ctx.blockchainName, Html.FROM_HTML_MODE_LEGACY)
            else
                Html.fromHtml(ctx.blockchainName)
            tvBlockchain.text = html
        } else
            tvBlockchain.text = ctx.blockchainName

    }

    private fun requestBalanceAndUnspentTransactions() {
        if (UtilHelper.isOnline(context as Activity)) {
            val coinEngine = CoinEngineFactory.create(ctx)
            requestCounter++
            coinEngine!!.requestBalanceAndUnspentTransactions(
                    object : CoinEngine.BlockchainRequestsCallbacks {

                        override fun onComplete(success: Boolean) {
                            LOG.i(TAG, "requestBalanceAndUnspentTransactions onComplete: $success, request counter $requestCounter")
                            if (activity == null) return
                            requestCounter--
                            if (!success) {
                                LOG.e(TAG, "requestBalanceAndUnspentTransactions ctx.error: " + ctx.error)
                            }
                            update()
                        }

                        override fun onProgress() {
                            if (activity == null) return
                            LOG.i(TAG, "requestBalanceAndUnspentTransactions onProgress")
//                            update()
                        }

                        override fun allowAdvance(): Boolean {
                            return try {
                                context?.let { UtilHelper.isOnline(it) }!!
                            } catch (e: KotlinNullPointerException) {
                                e.printStackTrace()
                                false
                            }
                        }
                    }
            )
        } else {
            ctx.error = getString(R.string.general_error_no_connection)
            update()
        }
    }

    private fun refresh(clearData: Boolean = true) {
        if (ctx.card == null) return

        // clear all card data and request again
        ctx.coinData.clearInfo()

        if (clearData) {
            ctx.error = null
            ctx.message = null
        }

        LOG.w(TAG, "============= START REFRESH")
        requestCounter = 0
        srl?.isRefreshing = true

        update()

        ctx.coinData.setIsBalanceEqual(true)


        requestBalanceAndUnspentTransactions()

        if (requestCounter == 0) {
            // if no connection and no requests posted
            srl?.isRefreshing = false
            update()
        }
    }

    companion object {
        val TAG: String = TagFragment::class.java.simpleName
    }
}
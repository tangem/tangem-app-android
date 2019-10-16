package com.tangem.ui.fragment.additional

import android.app.Activity
import android.graphics.Color
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.os.Bundle
import android.view.View
import com.tangem.Constant
import com.tangem.data.Blockchain
import com.tangem.data.network.CryptonitOtherApi
import com.tangem.ui.activity.MainActivity
import com.tangem.ui.fragment.BaseFragment
import com.tangem.ui.fragment.qr.CameraPermissionManager
import com.tangem.ui.navigation.NavigationResultListener
import com.tangem.wallet.CoinEngineFactory
import com.tangem.wallet.R
import com.tangem.wallet.TangemContext
import kotlinx.android.synthetic.main.fragment_prepare_cryptonit_other_api_withdrawal.*
import java.io.IOException

class PrepareCryptonitOtherApiWithdrawalFragment : BaseFragment(), NavigationResultListener,
        NfcAdapter.ReaderCallback {

    companion object {
        val TAG: String = PrepareCryptonitOtherApiWithdrawalFragment::class.java.simpleName
    }

    override val layoutId = R.layout.fragment_prepare_cryptonit_other_api_withdrawal

    private val cameraPermissionManager: CameraPermissionManager by lazy { CameraPermissionManager(this) }
    private val ctx: TangemContext by lazy { TangemContext.loadFromBundle(context, arguments) }
    private val cryptonit: CryptonitOtherApi by lazy { CryptonitOtherApi(context) }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tvKey.text = cryptonit.key
        tvUserID.text = cryptonit.userId
        tvSecret.text = cryptonit.secretDescription

        tvCardID.text = ctx.card!!.cidDescription
        tvWallet.text = ctx.coinData!!.wallet
        val engine = CoinEngineFactory.create(ctx)

        tvCurrency.text = engine!!.balanceCurrency

        etAmount.setText(engine.convertToAmount(engine.convertToInternalAmount(ctx.card!!.denomination)).toValueString())
        etAmount.filters = engine.amountInputFilters

        // set listeners
        btnLoad.setOnClickListener {
            try {
                val strAmount: String = etAmount.text.toString().replace(",", ".")
//                if (!engine.checkAmount(card, strAmount))
//                    etAmount.error = getString(R.string.unknown_amount_format)
                val dblAmount: Double = strAmount.toDouble()

                rlProgressBar.visibility = View.VISIBLE
                tvProgressDescription.text = getString(R.string.cryptonit_request_withdrawal)

                cryptonit.requestCryptoWithdrawal(ctx.blockchain.currency, dblAmount.toString(), ctx.coinData!!.wallet)
            } catch (e: Exception) {
                etAmount.error = getString(R.string.prepare_transaction_error_unknown_amount_format)
            }

            //Toast.makeText(this, strAmount, Toast.LENGTH_LONG).show()
//            val balance = engine.getBalanceLong(card)!! / (card!!.blockchain.multiplier / 1000.0)
//            if (etAmount.text.toString().replace(",", ".").toDouble() > balance) {
//                etAmount.error = getString(R.string.not_enough_funds_on_your_account)
//                return@setOnClickListener
//            }

        }
        ivCameraKey.setOnClickListener { checkPermissionsAndRunCamera() }

        ivCameraSecret.setOnClickListener { checkPermissionsAndRunCamera() }

        ivCameraUserId.setOnClickListener { checkPermissionsAndRunCamera() }

        ivRefreshBalance.setOnClickListener { doRequestBalance() }

        cryptonit.setBalanceListener { response ->
            when (ctx.blockchain) {
                Blockchain.Ethereum, Blockchain.EthereumTestNet -> {
                    tvBalance.text = response.eth_available
                }
                Blockchain.Bitcoin, Blockchain.BitcoinTestNet, Blockchain.BitcoinCash -> {
                    tvBalance.text = response.btc_available
                }
                else -> {
                }
            }

            tvBalanceCurrency.text = ctx.blockchain.currency
            tvBalance.setTextColor(Color.BLACK)
            rlProgressBar.visibility = View.INVISIBLE
            btnLoad.isActivated = true
        }
        cryptonit.setErrorListener { throwable ->
            throwable.printStackTrace()
            rlProgressBar.visibility = View.INVISIBLE
            tvError.visibility = View.VISIBLE
            tvError.text = throwable.message
        }
        cryptonit.setWithdrawalListener { response ->
            rlProgressBar.visibility = View.INVISIBLE
            if (response.success != null && response.success!!) {
                navigateUp()
            } else {
                tvError.visibility = View.VISIBLE
                tvError.text = response.reason!!.toString()
            }
        }
        btnLoad.isActivated = false
        doRequestBalance()
    }

    private fun checkPermissionsAndRunCamera() {
        if (cameraPermissionManager.isPermissionGranted()) {
            navigateForResult(Constant.REQUEST_CODE_SCAN_QR, R.id.action_prepareCryptonitOtherApiWithdrawalFragment_to_qrScanFragment)
        } else {
            cameraPermissionManager.requirePermission()
        }
    }

    override fun onNavigationResult(requestCode: String, resultCode: Int, data: Bundle?) {
        if (resultCode == Activity.RESULT_OK && data != null && data.containsKey("QRCode")) {
            when (requestCode) {
                Constant.REQUEST_CODE_SCAN_QR_KEY -> {
                    cryptonit.key = data.getString("QRCode")
                    tvKey?.text = cryptonit.key
                }
                Constant.REQUEST_CODE_SCAN_QR_SECRET -> {
                    cryptonit.secret = data.getString("QRCode")
                    tvSecret?.text = cryptonit.secretDescription
                }
                Constant.REQUEST_CODE_SCAN_QR_USER_ID -> {
                    cryptonit.userId = data.getString("QRCode")
                    tvUserID?.text = cryptonit.userId
                }
            }
            cryptonit.saveAccountInfo()
            doRequestBalance()
        }
    }

    override fun onTagDiscovered(tag: Tag) {
        try {
            (activity as MainActivity).nfcManager.ignoreTag(tag)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun doRequestBalance() {
        if (cryptonit.havaAccountInfo()) {
            rlProgressBar.visibility = View.VISIBLE
            tvProgressDescription.text = getString(R.string.cryptonit_request_balance)
            tvError.visibility = View.INVISIBLE
            cryptonit.requestBalance(ctx.blockchain.currency, "USD")
        } else {
            tvError.visibility = View.VISIBLE
            tvError.text = getString(R.string.cryptonit_not_enough_account_data)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        cameraPermissionManager.handleRequestPermissionResult(requestCode, grantResults) {
            navigateForResult(Constant.REQUEST_CODE_SCAN_QR, R.id.action_prepareCryptonitOtherApiWithdrawalFragment_to_qrScanFragment)
        }
    }

}
package com.tangem.data.nfc;

import android.app.Activity;
import android.content.Intent;
import android.nfc.tech.IsoDep;
import android.util.Log;

import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.activity.SendTransactionActivity;
import com.tangem.presentation.activity.SignPaymentActivity;
import com.tangem.util.BTCUtils;

import java.io.IOException;

public class SignPaymentTask extends Thread {
    public static final String TAG = SignPaymentTask.class.getSimpleName();

    private String txAmount = "";
    private String txFee = "";
    private Boolean txIncFee = true;

    public void SetTransactionValue(String amount, String fee, Boolean incfee) {
        txAmount = amount;
        txFee = fee;
        txIncFee = incfee;
    }

    private String txOutAddress;
    private Activity mContext;
    private TangemCard mCard;
    private NfcManager mNfcManager;
    private IsoDep mIsoDep;
    private CardProtocol.Notifications mNotifications;
    private boolean isCancelled = false;

    public SignPaymentTask(Activity context, TangemCard card, NfcManager nfcManager, IsoDep isoDep, CardProtocol.Notifications notifications, String amount, String fee, Boolean IncFee, String outAddress) {
        mCard = card;
        mContext = context;
        mNfcManager = nfcManager;
        mIsoDep = isoDep;
        mNotifications = notifications;
        txOutAddress = outAddress;
        SetTransactionValue(amount, fee, IncFee);
    }

    @Override
    public void run() {
        if (mIsoDep == null) {
            return;
        }
        CardProtocol protocol = new CardProtocol(mContext, mIsoDep, mCard, mNotifications);

        mNotifications.onReadStart(protocol);
        try {

            // for Samsung's bugs -
            // Workaround for the Samsung Galaxy S5 (since the
            // first connection always hangs on transceive).
            int timeout = mIsoDep.getTimeout();
            mIsoDep.connect();
            mIsoDep.close();
            mIsoDep.connect();
            mIsoDep.setTimeout(timeout);
            try {
                mNotifications.onReadProgress(protocol, 5);

                Log.i(TAG, "[-- Start sign payment --]");

                if (isCancelled) return;
                protocol.run_Read(false);
                protocol.run_VerifyCard();

                Log.i(TAG, "Manufacturer: " + protocol.getCard().getManufacturer().getOfficialName());

                mNotifications.onReadProgress(protocol, 30);
                if (isCancelled) return;
//
//                    if (mCard.getBlockchain() == Blockchain.Ethereum) {
//                        SignETH_TX(protocol);
//                    } else {
//                        SignBTC_TX(protocol);
//                    }

                CoinEngine engine = CoinEngineFactory.create(mCard.getBlockchain());
                if (engine != null) {
                    if (mCard.getPauseBeforePIN2() > 0) {
                        mNotifications.onReadWait(mCard.getPauseBeforePIN2());
                    }

                    byte[] tx = null;
                    try {
                        tx = engine.sign(txFee, txAmount, txIncFee, txOutAddress, mCard, protocol);
                    }
                    catch (IOException e) {
                        e.printStackTrace();
                        protocol.setError(e);
                    } finally {
                        mNotifications.onReadWait(0);
                    }

                    if (tx != null) {
                        String txStr = BTCUtils.toHex(tx);
                        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {
                            txStr = String.format("0x%s", txStr);
                        }

                        Intent intent = new Intent(mContext, SendTransactionActivity.class);
                        intent.putExtra(TangemCard.EXTRA_UID, mCard.getUID());
                        intent.putExtra(TangemCard.EXTRA_CARD, mCard.getAsBundle());
                        intent.putExtra(SendTransactionActivity.EXTRA_TX, txStr);
                        mContext.startActivityForResult(intent, SignPaymentActivity.REQUEST_CODE_SEND_PAYMENT);
                    }
                }
                mNotifications.onReadProgress(protocol, 100);


                if (isCancelled) return;

            } finally {
                mNfcManager.ignoreTag(mIsoDep.getTag());
                mNotifications.onReadWait(0);
            }
        } catch (CardProtocol.TangemException_InvalidPIN e) {
            e.printStackTrace();
            protocol.setError(e);
        } catch (CardProtocol.TangemException_WrongAmount e) {
            e.printStackTrace();
            protocol.setError(e);
        } catch (Exception e) {
            e.printStackTrace();
            protocol.setError(e);

        } finally {
            Log.i(TAG, "[-- Finish sign payment --]");
            mNotifications.onReadFinish(protocol);
        }
    }

    public void cancel(Boolean AllowInterrupt) {
        try {
            if (isAlive()) {
                isCancelled = true;
                join(500);
            }
            if (isAlive() && AllowInterrupt) {
                interrupt();
                mNotifications.onReadCancel();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
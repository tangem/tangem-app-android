package com.tangem.presentation.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;
import com.tangem.presentation.activity.CreateNewWalletActivity;
import com.tangem.presentation.activity.PreparePaymentActivity;
import com.tangem.presentation.activity.PurgeActivity;
import com.tangem.presentation.activity.RequestPINActivity;
import com.tangem.presentation.activity.SwapPINActivity;
import com.tangem.presentation.activity.VerifyCardActivity;
import com.tangem.domain.wallet.BTCUtils;
import com.tangem.domain.wallet.BitcoinException;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.data.network.request.ExchangeRequest;
import com.tangem.data.network.task.ExchangeTask;
import com.tangem.domain.wallet.Infura_Request;
import com.tangem.data.network.task.InfuraTask;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.wallet.R;
import com.tangem.domain.wallet.SharedData;
import com.tangem.data.network.task.VerifyCardTask;
import com.tangem.presentation.dialog.WaitSecurityDelayDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.CLIPBOARD_SERVICE;

/**
 * A placeholder fragment containing a simple view.
 */
public class LoadedWalletFragment extends Fragment implements SwipeRefreshLayout.OnRefreshListener, NfcAdapter.ReaderCallback, CardProtocol.Notifications {


    private static final int REQUEST_CODE_SEND_PAYMENT = 1;
    private static final int REQUEST_CODE_PURGE = 2;
    private static final int REQUEST_CODE_REQUEST_PIN2_FOR_PURGE = 3;
    private static final int REQUEST_CODE_VERIFY_CARD = 4;
    private static final int REQUEST_CODE_ENTER_NEW_PIN = 5;
    private static final int REQUEST_CODE_ENTER_NEW_PIN2 = 6;
    private static final int REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN = 7;
    private static final int REQUEST_CODE_SWAP_PIN = 8;
    TangemCard mCard;
    TextView tvCardID, tvBalance, tvOffline, tvBalanceEquivalent, tvWallet, tvInputs, lbInputs, tvOutputs, tvSend, tvPurge, tvError, tvMessage, tvIssuer, tvIssuerData, tvBlockchain, tvLastInput, tvLastOutput, lbLastOutput, tvValidationNode;
    TextView tvHeader, tvCaution;
    ImageView imgLookup;
    TextView tvLookup;
    ProgressBar progressBar;
    ImageView ivBlockchain, ivPIN, ivPIN2orSecurityDelay, ivDeveloperVersion, ivQR;
    SwipeRefreshLayout mSwipeRefreshLayout;
    List<UpdateWalletInfoTask> updateTasks = new ArrayList<>();
    private NfcManager mNfcManager;
    private final String logTag = "LoadedWalletFragment";
    private boolean lastReadSuccess = true;
    private VerifyCardTask verifyCardTask = null;
    private int requestPIN2Count = 0;

    public LoadedWalletFragment() {

    }

    public void onRefresh() {
        //Update
        // Showing refresh animation before making http call
        if (updateTasks.size() > 0) return;
        ;
        mSwipeRefreshLayout.setRefreshing(true);
        mCard.clearInfo();
        mCard.setError(null);
        mCard.setMessage(null);

        boolean needResendTX = LastSignStorage.getNeedTxSend(mCard.getWallet());

        UpdateViews();

        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

        if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet) {
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            for (int i = 0; i < data.allRequest; ++i) {
                String nodeAddress = engine.GetNextNode(mCard);
                int nodePort = engine.GetNextNodePort(mCard);
                UpdateWalletInfoTask connectTaskEx = new UpdateWalletInfoTask(nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(mCard.getWallet()));
            }

            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);
            UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(nodeAddress, nodePort, data);
            updateTasks.add(updateWalletInfoTask);
            updateWalletInfoTask.execute(ElectrumRequest.ListUnspent(mCard.getWallet()), ElectrumRequest.ListHistory(mCard.getWallet()));

            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "bitcoin", "bitcoin");
            taskRate.execute(rate);


        } else if (mCard.getBlockchain() == Blockchain.BitcoinCash || mCard.getBlockchain() == Blockchain.BitcoinCashTestNet) {
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            for (int i = 0; i < data.allRequest; ++i) {
                String nodeAddress = engine.GetNextNode(mCard);
                int nodePort = engine.GetNextNodePort(mCard);
                UpdateWalletInfoTask connectTaskEx = new UpdateWalletInfoTask(nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(mCard.getWallet()));
            }

            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);
            UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(nodeAddress, nodePort, data);

            updateTasks.add(updateWalletInfoTask);
            updateWalletInfoTask.execute(ElectrumRequest.ListUnspent(mCard.getWallet()), ElectrumRequest.ListHistory(mCard.getWallet()));

            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "bitcoin-cash", "bitcoin-cash");
            taskRate.execute(rate);


        }
        else if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet) {
            ETHRequestTask updateETH = new ETHRequestTask(mCard.getBlockchain());
            Infura_Request reqETH = Infura_Request.GetBalance(mCard.getWallet());
            reqETH.setID(67);
            reqETH.setBlockchain(mCard.getBlockchain());

            Infura_Request reqNonce = Infura_Request.GetOutTransactionCount(mCard.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(mCard.getBlockchain());

            updateETH.execute(reqETH, reqNonce);


            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "ethereum", "ethereum");
            taskRate.execute(rate);

        } else if (mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask updateETH = new ETHRequestTask(mCard.getBlockchain());
            Infura_Request reqETH = Infura_Request.GetTokenBalance(mCard.getWallet(), engine.GetContractAddress(mCard), engine.GetTokenDecimals(mCard));
            reqETH.setID(67);
            reqETH.setBlockchain(mCard.getBlockchain());

            Infura_Request reqBalance = Infura_Request.GetBalance(mCard.getWallet());
            reqBalance.setID(67);
            reqBalance.setBlockchain(mCard.getBlockchain());

            Infura_Request reqNonce = Infura_Request.GetOutTransactionCount(mCard.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(mCard.getBlockchain());
            updateETH.execute(reqETH, reqNonce, reqBalance);


            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "basic-attention-token", "ethereum");
            taskRate.execute(rate);
        }

        if (needResendTX) {
            SendTransaction(LastSignStorage.getTxForSend(mCard.getWallet()));
        }
    }

    void doShareWallet(boolean useURI) {
        if (useURI) {
            String txtShare = CoinEngineFactory.Create(mCard.getBlockchain()).getShareWalletURI(mCard).toString();
            //String txtShare = Blockchain.getShareWalletURI(mCard).toString();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Wallet address");
            intent.putExtra(Intent.EXTRA_TEXT, txtShare);

            PackageManager packageManager = getActivity().getPackageManager();
            List activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                String title = "Share wallet address with:";
                // Create intent to show chooser
                Intent chooser = Intent.createChooser(intent, title);
                // Verify the intent will resolve to at least one activity
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(chooser);
                }
            } else {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(txtShare, txtShare));
                Toast.makeText(getContext(), "Copied to clipboard", Toast.LENGTH_LONG).show();
            }
        } else {
            String txtShare = mCard.getWallet();
            ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
            clipboard.setPrimaryClip(ClipData.newPlainText(txtShare, txtShare));
            Toast.makeText(getContext(), "Copied to clipboard", Toast.LENGTH_LONG).show();
        }


    }

    @Override
    public View onCreateView(final LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fr_loaded_wallet, container, false);

        mNfcManager = new NfcManager(this.getActivity(), this);


        // SwipeRefreshLayout
        mSwipeRefreshLayout = v.findViewById(R.id.swipe_container);
        mSwipeRefreshLayout.setOnRefreshListener(this);

        mCard = new TangemCard(getActivity().getIntent().getStringExtra("UID"));
        mCard.LoadFromBundle(getActivity().getIntent().getExtras().getBundle("Card"));
        tvCardID = v.findViewById(R.id.tvCardID);

        tvBalance = v.findViewById(R.id.tvBalance);
        tvOffline = v.findViewById(R.id.tvOffline);

        if (mCard.getBlockchain() == Blockchain.Token) {
            //tvBalance.setLines(2);
            tvBalance.setSingleLine(false);
        }

        tvBalanceEquivalent = v.findViewById(R.id.tvBalanceEquivalent);

        tvWallet = v.findViewById(R.id.tvWallet);
        tvWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doShareWallet(false);
            }
        });

        tvInputs = v.findViewById(R.id.tvInputs);
        lbInputs = v.findViewById(R.id.lbInputs);

        tvLastOutput = v.findViewById(R.id.tvLastOutput);
        lbLastOutput = v.findViewById(R.id.lbLastOutput);

        final CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

        boolean visibleFlag = engine != null ? engine.InOutPutVisible() : true;
        int visibleIOPuts = visibleFlag ? View.VISIBLE : View.GONE;
        if (tvInputs != null) {
            tvInputs.setVisibility(visibleIOPuts);
        }

        if (lbInputs != null) {
            lbInputs.setVisibility(visibleIOPuts);
        }

        if (tvLastOutput != null) {
            tvLastOutput.setVisibility(visibleIOPuts);
        }

        if (lbLastOutput != null) {
            lbLastOutput.setVisibility(visibleIOPuts);
        }

        tvValidationNode = v.findViewById(R.id.tvValidationNode);

        progressBar = v.findViewById(R.id.progressBar);

        tvBlockchain = v.findViewById(R.id.tvBlockchain);
        ivBlockchain = v.findViewById(R.id.imgBlockchain);
        ivPIN = v.findViewById(R.id.imgPIN);
        ivPIN2orSecurityDelay = v.findViewById(R.id.imgPIN2orSecurityDelay);
        ivDeveloperVersion = v.findViewById(R.id.imgDeveloperVersion);

        ivQR = v.findViewById(R.id.qrWallet);


        try {
            ivQR.setImageBitmap(generateQrCode(engine.getShareWalletURI(mCard).toString()));
        } catch (WriterException e) {
            e.printStackTrace();
        }
        ivQR.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                doShareWallet(true);
            }
        });

        tvError = v.findViewById(R.id.tvError);
        tvMessage = v.findViewById(R.id.tvMessage);

        tvIssuer = v.findViewById(R.id.tvIssuer);
        tvIssuerData = v.findViewById(R.id.tvIssuerData);

        tvHeader = v.findViewById(R.id.tvHeader);
        tvCaution = v.findViewById(R.id.tvCaution);

        tvSend = v.findViewById(R.id.tvSend);

        imgLookup = v.findViewById(R.id.imgLookup);

        if (imgLookup != null) {

            imgLookup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mCard.hasBalanceInfo()) {
                        return;
                    }
                    CoinEngine engineClick = CoinEngineFactory.Create(mCard.getBlockchain());

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, engineClick.getShareWalletURIExplorer(mCard));
                    startActivity(browserIntent);
                }
            });

        }

        tvLookup = v.findViewById(R.id.tvLookup);

        if (tvLookup != null) {

            tvLookup.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mCard.hasBalanceInfo()) {
                        return;
                    }
                    CoinEngine engineClick = CoinEngineFactory.Create(mCard.getBlockchain());

                    Intent browserIntent = new Intent(Intent.ACTION_VIEW, engineClick.getShareWalletURIExplorer(mCard));
                    startActivity(browserIntent);
                }
            });
        }

        if (tvSend != null) {
            tvSend.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (!mCard.hasBalanceInfo()) {
                        return;
                    } else if (!engine.IsBalanceNotZero(mCard)) {
                        Toast.makeText(getContext(), "The wallet is empty", Toast.LENGTH_LONG).show();
                        return;
                    } else if (!engine.IsBalanceAlterNotZero(mCard)) {
                        Toast.makeText(getContext(), "Not enough funds for transaction fee (gas)!", Toast.LENGTH_LONG).show();
                        return;
                    } else if (engine.AwaitingConfirmation(mCard)) {
                        Toast.makeText(getContext(), "Please wait while previous transaction is confirmed in blockchain", Toast.LENGTH_LONG).show();
                        return;
                    } else if (!engine.CheckUnspentTransaction(mCard)) {
                        Toast.makeText(getContext(), "Please wait for confirmation of incoming transaction!", Toast.LENGTH_LONG).show();
                        return;
                    } else if (mCard.getRemainingSignatures() == 0) {
                        Toast.makeText(getContext(), "Card hasn't remaining signature!", Toast.LENGTH_LONG).show();
                        return;
                    }

                    Intent intent = new Intent(getContext(), PreparePaymentActivity.class);
                    intent.putExtra("UID", mCard.getUID());
                    intent.putExtra("Card", mCard.getAsBundle());
                    startActivityForResult(intent, REQUEST_CODE_SEND_PAYMENT);
                }
            });
        }

        tvPurge = v.findViewById(R.id.tvPurge);
        if (tvPurge != null) {
            tvPurge.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View v) {
                                               showMenu(tvPurge);
                                           }
                                       }
            );
        }
        UpdateViews();

        if (!mCard.hasBalanceInfo()) {
            mSwipeRefreshLayout.setRefreshing(true);
            mSwipeRefreshLayout.postDelayed(new Runnable() {
                @Override
                public void run() {
                    onRefresh();
                }
            }, 1000);
        }
        return v;
    }

    public void showMenu(View v) {
        final PopupMenu popup = new PopupMenu(getActivity(), v);
        MenuInflater inflater = popup.getMenuInflater();
        inflater.inflate(R.menu.menu_loaded_wallet, popup.getMenu());

        popup.getMenu().findItem(R.id.action_set_PIN1).setVisible(mCard.allowSwapPIN());
        popup.getMenu().findItem(R.id.action_reset_PIN1).setVisible(mCard.allowSwapPIN() && !mCard.useDefaultPIN1());
        popup.getMenu().findItem(R.id.action_set_PIN2).setVisible(mCard.allowSwapPIN2());
        popup.getMenu().findItem(R.id.action_reset_PIN2).setVisible(mCard.allowSwapPIN2() && !mCard.useDefaultPIN2());
        popup.getMenu().findItem(R.id.action_reset_PINs).setVisible(mCard.allowSwapPIN() && mCard.allowSwapPIN2() && !mCard.useDefaultPIN1() && !mCard.useDefaultPIN2());

        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                int id = item.getItemId();
                switch (id) {
                    case R.id.action_set_PIN1:
                        doSetPin();
                        return true;
                    case R.id.action_reset_PIN1:
                        doResetPin();
                        return true;
                    case R.id.action_set_PIN2:
                        doSetPin2();
                        return true;
                    case R.id.action_reset_PIN2:
                        doResetPin2();
                        return true;
                    case R.id.action_reset_PINs:
                        doResetPins();
                        return true;
                    case R.id.action_purge:
                        doPurge();
                        return true;
                    default:
                        return false;
                }
            }
        });
        popup.show();
    }

    String newPIN = "", newPIN2 = "";

    void doSetPin() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestNewPIN.toString());
        newPIN = "";
        newPIN2 = "";
        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN);
    }

    void doResetPin() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        newPIN = PINStorage.getDefaultPIN();
        newPIN2 = "";
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
    }

    void doResetPin2() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        newPIN = "";
        newPIN2 = PINStorage.getDefaultPIN2();
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
    }

    void doResetPins() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        newPIN = PINStorage.getDefaultPIN();
        newPIN2 = PINStorage.getDefaultPIN2();
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
    }


    void doSetPin2() {
        requestPIN2Count = 0;
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestNewPIN2.toString());
        newPIN = "";
        newPIN2 = "";
        startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2);
    }

    void doPurge() {
        requestPIN2Count = 0;
        final CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        if (!mCard.hasBalanceInfo()) {
            return;
        } else if (engine.IsBalanceNotZero(mCard)) {
            Toast.makeText(getContext(), "Cannot erase wallet with non-zero balance", Toast.LENGTH_LONG).show();
            return;
        }

        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE);
    }

    void UpdateViews() {
        try {
            if (timerHideErrorAndMessage != null) {
                timerHideErrorAndMessage.cancel();
                timerHideErrorAndMessage = null;
            }
            tvCardID.setText(mCard.getCIDDescription());

            if ((mCard.getError() == null || mCard.getError().isEmpty())) {
                tvError.setVisibility(View.GONE);
                tvError.setText("");
            } else {
                tvError.setVisibility(View.VISIBLE);
                tvError.setText(mCard.getError());
            }

            boolean needResendTX = LastSignStorage.getNeedTxSend(mCard.getWallet());

            if ((mCard.getMessage() == null || mCard.getMessage().isEmpty()) && !needResendTX) {
                tvMessage.setText("");
                tvMessage.setVisibility(View.GONE);
            } else {
                if (needResendTX) {
                    tvMessage.setText("Sending cached transaction...");
                } else {
                    tvMessage.setText(mCard.getMessage());
                }
                tvMessage.setVisibility(View.VISIBLE);

            }


            CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

            if (engine.HasBalanceInfo(mCard) || mCard.getOfflineBalance() == null) {
                if (mCard.getBlockchain() == Blockchain.Token) {
                    Spanned html = Html.fromHtml(engine.GetBalanceWithAlter(mCard));
                    tvBalance.setText(html);
                } else {
                    tvBalance.setText(engine.GetBalanceWithAlter(mCard));
                }

                tvBalanceEquivalent.setText(engine.GetBalanceEquivalent(mCard));
                tvOffline.setVisibility(View.INVISIBLE);
            } else {
                String offlineAmount = engine.ConvertByteArrayToAmount(mCard, mCard.getOfflineBalance());
                if (mCard.getBlockchain() == Blockchain.Token) {
                    tvBalance.setText("NOT IMPLEMENTED");
                } else {
                    tvBalance.setText(engine.GetAmountDescription(mCard, offlineAmount));
                }

                tvBalanceEquivalent.setText(engine.GetAmountEqualentDescriptor(mCard, offlineAmount));
                tvOffline.setVisibility(View.VISIBLE);
            }

            if (!mCard.getAmountEquivalentDescriptionAvailable()) {
                //tvBalanceEquivalent.setError("Service unavailable");
            } else {
                tvBalanceEquivalent.setError(null);
            }

            tvWallet.setText(mCard.getWallet());

            tvInputs.setText(mCard.getInputsDescription());
            if (mCard.getLastInputDescription().contains("awaiting")) {
                tvInputs.setTextColor(getContext().getResources().getColor(R.color.not_confirmed, getContext().getTheme()));
            } else if (mCard.getLastInputDescription().contains("None")) {
                tvInputs.setTextColor(getContext().getResources().getColor(R.color.primary_dark, getContext().getTheme()));
            } else {
                tvInputs.setTextColor(getContext().getResources().getColor(R.color.confirmed, getContext().getTheme()));
            }

            if (tvOutputs != null) {
                tvOutputs.setText(mCard.getOutputsDescription());
            }

            if (tvLastInput != null) {
                tvLastInput.setText(mCard.getLastInputDescription());
            }
            if (tvLastOutput != null) {
                tvLastOutput.setText(mCard.getLastOutputDescription());
            }

            tvBlockchain.setText(mCard.getBlockchainName());
            ivBlockchain.setImageResource(mCard.getBlockchain().getImageResource(this.getContext(), mCard.getTokenSymbol()));

            if (tvValidationNode != null) {
                tvValidationNode.setText(mCard.getValidationNodeDescription());
            }

            if (mCard.useDefaultPIN1()) {
                ivPIN.setImageResource(R.drawable.unlock_pin1);
                ivPIN.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by default PIN1 code", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                ivPIN.setImageResource(R.drawable.lock_pin1);
                ivPIN.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by user's PIN1 code", Toast.LENGTH_LONG).show();
                    }
                });
            }

            if (mCard.getPauseBeforePIN2() > 0 && (mCard.useDefaultPIN2() || !mCard.useSmartSecurityDelay())) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.timer);
                ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), String.format("This banknote will enforce %.0f seconds security delay for all operations requiring PIN2 code", mCard.getPauseBeforePIN2() / 1000.0), Toast.LENGTH_LONG).show();
                    }
                });

            } else if (mCard.useDefaultPIN2()) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by default PIN2 code", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "This banknote is protected by user's PIN2 code", Toast.LENGTH_LONG).show();
                    }
                });
            }


            if (mCard.useDevelopersFirmware()) {
                ivDeveloperVersion.setImageResource(R.drawable.ic_developer_version);
                ivDeveloperVersion.setVisibility(View.VISIBLE);
                ivDeveloperVersion.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(getContext(), "Unlocked banknote, only for development use", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                ivDeveloperVersion.setVisibility(View.INVISIBLE);
            }

            if (tvSend != null) {
                if (mCard.hasBalanceInfo()) {
                    tvSend.setEnabled(true);
                } else {
                    tvSend.setEnabled(false);
                }
            }

            if (tvPurge != null) {
                if (mCard.hasBalanceInfo()) {
                    tvPurge.setEnabled(true);
                } else {
                    tvPurge.setEnabled(false);
                }
            }

            tvIssuer.setText(mCard.getIssuerDescription());

            timerHideErrorAndMessage = new Timer();


            timerHideErrorAndMessage.schedule(new TimerTask() {
                @Override
                public void run() {
                    tvError.post(new Runnable() {
                        @Override
                        public void run() {
                            tvMessage.setVisibility(View.GONE);
                            tvError.setVisibility(View.GONE);
                            mCard.setError(null);
                            mCard.setMessage(null);
                        }
                    });
                }
            }, 5000);

            if (mCard.isReusable()) {
                tvHeader.setText("REUSABLE WALLET");
                tvCaution.setVisibility(View.GONE);
            } else {
                if (mCard.getMaxSignatures() == mCard.getRemainingSignatures()) {
                    tvHeader.setText("BANKNOTE");
                    tvCaution.setVisibility(View.GONE);
                } else {
                    tvHeader.setText("NON-TRANSFERABLE BANKNOTE");
                    tvCaution.setVisibility(View.VISIBLE);
                }
            }

            if (mCard.useDevelopersFirmware()) {
                tvHeader.setText("DEVELOPER KIT");
                tvCaution.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    Timer timerHideErrorAndMessage = null;

    public Intent prepareResultIntent() {
        Intent data = new Intent();
        data.putExtra("UID", mCard.getUID());
        data.putExtra("Card", mCard.getAsBundle());
        return data;
    }

    private void SendTransaction(String tx) {
        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask task = new ETHRequestTask(mCard.getBlockchain());
            Infura_Request req = Infura_Request.SendTransaction(mCard.getWallet(), tx);
            req.setID(67);
            req.setBlockchain(mCard.getBlockchain());
            task.execute(req);
        } else if (mCard.getBlockchain() == Blockchain.Bitcoin ||mCard.getBlockchain() == Blockchain.BitcoinTestNet) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);

            UpdateWalletInfoTask connectTask = new UpdateWalletInfoTask(nodeAddress, nodePort);
            connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
        }
        else if (mCard.getBlockchain() == Blockchain.BitcoinCash ||mCard.getBlockchain() == Blockchain.BitcoinCashTestNet) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);

            UpdateWalletInfoTask connectTask = new UpdateWalletInfoTask(nodeAddress, nodePort);
            connectTask.execute(ElectrumRequest.Broadcast(mCard.getWallet(), tx));
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_ENTER_NEW_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (data.getExtras().containsKey("confirmPIN")) {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                            intent.putExtra("UID", mCard.getUID());
                            intent.putExtra("Card", mCard.getAsBundle());
                            newPIN = data.getStringExtra("newPIN");
                            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
                        } else {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("newPIN", data.getStringExtra("newPIN"));
                            intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN.toString());
                            startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN);
                        }
                    }
                }
                break;
            case REQUEST_CODE_ENTER_NEW_PIN2:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (data.getExtras().containsKey("confirmPIN2")) {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                            intent.putExtra("UID", mCard.getUID());
                            intent.putExtra("Card", mCard.getAsBundle());
                            newPIN2 = data.getStringExtra("newPIN2");
                            startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
                        } else {
                            Intent intent = new Intent(getContext(), RequestPINActivity.class);
                            intent.putExtra("newPIN2", data.getStringExtra("newPIN2"));
                            intent.putExtra("mode", RequestPINActivity.Mode.ConfirmNewPIN2.toString());
                            startActivityForResult(intent, REQUEST_CODE_ENTER_NEW_PIN2);
                        }
                    }
                }
                break;
            case REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (newPIN.equals("")) {
                        newPIN = mCard.getPIN();
                    }
                    if (newPIN2.equals("")) {
                        newPIN2 = PINStorage.getPIN2();
                    }

                    PINSwapWarningDialog dialog = (new PINSwapWarningDialog());
                    dialog.activityFragment = this;
                    if (!PINStorage.isDefaultPIN(newPIN) || !PINStorage.isDefaultPIN2(newPIN2)) {
                        dialog.message = "If you forget your new PIN you will lose your money forever!";
                    } else {
                        dialog.message = "If you use default PIN someone can steal your money!";
                    }
                    dialog.show(getActivity().getFragmentManager(), "PINSwapWarningDialog");
                }
                break;

            case REQUEST_CODE_SWAP_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        data = new Intent();

                        data.putExtra("UID", mCard.getUID());
                        data.putExtra("Card", mCard.getAsBundle());
                        data.putExtra("modification", "delete");
                    } else {
                        data.putExtra("modification", "update");
                    }
                    getActivity().setResult(Activity.RESULT_OK, data);
                    getActivity().finish();
                } else {
                    if (data != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                        TangemCard updatedCard = new TangemCard(data.getStringExtra("UID"));
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                        mCard = updatedCard;
                    }
                    if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                        requestPIN2Count++;
                        Intent intent = new Intent(getContext(), RequestPINActivity.class);
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                        intent.putExtra("UID", mCard.getUID());
                        intent.putExtra("Card", mCard.getAsBundle());
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN);
                        return;
                    } else {
                        if (data != null && data.getExtras().containsKey("message")) {
                            mCard.setError(data.getStringExtra("message"));
                        }
                    }
                }
                break;
            case REQUEST_CODE_REQUEST_PIN2_FOR_PURGE:
                if (resultCode == Activity.RESULT_OK) {
                    Intent intent = new Intent(getContext(), PurgeActivity.class);
                    intent.putExtra("UID", mCard.getUID());
                    intent.putExtra("Card", mCard.getAsBundle());
                    startActivityForResult(intent, REQUEST_CODE_PURGE);
                }
                break;
            case REQUEST_CODE_PURGE:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        data = new Intent();

                        data.putExtra("UID", mCard.getUID());
                        data.putExtra("Card", mCard.getAsBundle());
                        data.putExtra("modification", "delete");
                    } else {
                        data.putExtra("modification", "update");
                    }
                    getActivity().setResult(Activity.RESULT_OK, data);
                    getActivity().finish();
                } else {
                    if (data != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                        TangemCard updatedCard = new TangemCard(data.getStringExtra("UID"));
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                        mCard = updatedCard;
                    }
                    if (resultCode == CreateNewWalletActivity.RESULT_INVALID_PIN && requestPIN2Count < 2) {
                        requestPIN2Count++;
                        Intent intent = new Intent(getContext(), RequestPINActivity.class);
                        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN2.toString());
                        intent.putExtra("UID", mCard.getUID());
                        intent.putExtra("Card", mCard.getAsBundle());
                        startActivityForResult(intent, REQUEST_CODE_REQUEST_PIN2_FOR_PURGE);
                        return;
                    } else {
                        if (data != null && data.getExtras().containsKey("message")) {
                            mCard.setError(data.getStringExtra("message"));
                        }
                    }
                    UpdateViews();
                }
                break;
            case REQUEST_CODE_SEND_PAYMENT:
                if (resultCode == Activity.RESULT_OK) {
                    mSwipeRefreshLayout.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            onRefresh();
                        }
                    }, 10000);
                    mSwipeRefreshLayout.setRefreshing(true);
                    mCard.clearInfo();
                    UpdateViews();
                }

                if (data != null) {
                    if (data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
                        TangemCard updatedCard = new TangemCard(data.getStringExtra("UID"));
                        updatedCard.LoadFromBundle(data.getBundleExtra("Card"));
                        mCard = updatedCard;
                    }
                    if (data.getExtras().containsKey("message")) {
                        if (resultCode == Activity.RESULT_OK) {
                            mCard.setMessage(data.getStringExtra("message"));
                        } else {
                            mCard.setError(data.getStringExtra("message"));
                        }
                    }
                    UpdateViews();
                }

                break;
        }

    }

    private void startSwapPINActivity() {
        Intent intent = new Intent(getContext(), SwapPINActivity.class);
        intent.putExtra("UID", mCard.getUID());
        intent.putExtra("Card", mCard.getAsBundle());
        intent.putExtra("newPIN", newPIN);
        intent.putExtra("newPIN2", newPIN2);
        startActivityForResult(intent, REQUEST_CODE_SWAP_PIN);
    }

    public static Bitmap generateQrCode(String myCodeText) throws WriterException {
        Hashtable<EncodeHintType, ErrorCorrectionLevel> hintMap = new Hashtable<EncodeHintType, ErrorCorrectionLevel>();
        hintMap.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H); // H = 30% damage

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        int size = 256;

        BitMatrix bitMatrix = qrCodeWriter.encode(myCodeText, BarcodeFormat.QR_CODE, size, size, hintMap);
        int width = bitMatrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, width, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < width; y++) {
                bmp.setPixel(y, x, bitMatrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }


    @Override
    public void onResume() {
        super.onResume();
        mNfcManager.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mNfcManager.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        for (UpdateWalletInfoTask ut : updateTasks) {
            ut.cancel(true);
        }
        mNfcManager.onStop();
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            final IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                throw new CardProtocol.TangemException(getString(R.string.wrong_tag_err));
            }
            byte UID[] = tag.getId();
            String sUID = Util.byteArrayToHexString(UID);
            if (!mCard.getUID().equals(sUID)) {
                Log.d(logTag, "Invalid UID: " + sUID);
                mNfcManager.IgnoreTag(isoDep.getTag());
                return;
            } else {
                Log.v(logTag, "UID: " + sUID);
            }

            if (lastReadSuccess) {
                isoDep.setTimeout(1000);
            } else {
                isoDep.setTimeout(65000);
            }
            //lastTag = tag;
            verifyCardTask = new VerifyCardTask(getContext(), mCard, mNfcManager, isoDep, this);
            verifyCardTask.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ErrorOnUpdate(String message) {
        mCard.setError("Cannot obtain data from blockchain");
        UpdateViews();
    }

    private class ETHRequestTask extends InfuraTask {
        ETHRequestTask(Blockchain blockchain) {
            super(blockchain);
        }

        @Override
        protected void onPostExecute(List<Infura_Request> requests) {
            super.onPostExecute(requests);
            for (Infura_Request request : requests) {
                try {
                    if (request.error == null) {

                        if (request.isMethod(Infura_Request.METHOD_ETH_GetBalance)) {
                            try {
                                String balanceCap = request.getResultString();
                                balanceCap = balanceCap.substring(2);
                                BigInteger l = new BigInteger(balanceCap, 16);
                                BigInteger d = l.divide(new BigInteger("1000000000000000000", 10));
                                Long balance = d.longValue();

                                mCard.setBalanceConfirmed(balance);
                                mCard.setBalanceUnconfirmed(0L);
                                if (mCard.getBlockchain() != Blockchain.Token)
                                    mCard.setDecimalBalance(l.toString(10));
                                mCard.setDecimalBalanceAlter(l.toString(10));

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ErrorOnUpdate(e.toString());
                            }
                        } else if (request.isMethod(Infura_Request.METHOD_ETH_Call)) {
                            try {
                                String balanceCap = request.getResultString();
                                balanceCap = balanceCap.substring(2);
                                BigInteger l = new BigInteger(balanceCap, 16);
                                Long balance = l.longValue();

                                if (l.compareTo(BigInteger.ZERO) == 0) {
                                    mCard.setBlockchainID(Blockchain.Ethereum.getID());
                                    mCard.addTokenToBlockchainName();
                                    mSwipeRefreshLayout.setRefreshing(false);
                                    onRefresh();
                                    return;
                                }

                                mCard.setBalanceConfirmed(balance);
                                mCard.setBalanceUnconfirmed(0L);
                                mCard.setDecimalBalance(l.toString(10));

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ErrorOnUpdate(e.toString());
                            }
                        } else if (request.isMethod(Infura_Request.METHOD_ETH_GetOutTransactionCount)) {
                            try {
                                String nonce = request.getResultString();
                                nonce = nonce.substring(2);
                                BigInteger count = new BigInteger(nonce, 16);

                                mCard.SetConfirmTXCount(count);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        } else if (request.isMethod(Infura_Request.METHOD_ETH_SendRawTransaction)) {
                            try {
                                String hashTX = "";

                                try {
                                    String tmp = request.getResultString();
                                    hashTX = tmp;
                                } catch (JSONException e) {
                                    JSONObject msg = request.getAnswer();
                                    JSONObject err = msg.getJSONObject("error");
                                    hashTX = err.getString("message");
                                    LastSignStorage.setLastMessage(mCard.getWallet(), hashTX);
                                    ErrorOnUpdate("Failed to send transaction. Try again");
                                    return;
                                }

                                if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                    hashTX = hashTX.substring(2);
                                }
                                BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                LastSignStorage.setTxWasSend(mCard.getWallet());
                                LastSignStorage.setLastMessage(mCard.getWallet(), "");
                                Log.e("TX_RESULT", hashTX);


                                BigInteger nonce = mCard.GetConfirmTXCount();
                                nonce.add(BigInteger.valueOf(1));
                                mCard.SetConfirmTXCount(nonce);
                                Log.e("TX_RESULT", hashTX);

                            } catch (Exception e) {
                                e.printStackTrace();
                                ErrorOnUpdate("Failed to send transaction. Try again");
                            }
                        }
                        UpdateViews();
                    } else {
                        ErrorOnUpdate(request.error);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    ErrorOnUpdate(e.toString());
                }
            }

            if (updateTasks.size() == 0) mSwipeRefreshLayout.setRefreshing(false);
        }
    }

    public void OnReadStart(CardProtocol cardProtocol) {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(5);
            }
        });
    }

    public void OnReadFinish(final CardProtocol cardProtocol) {

        verifyCardTask = null;

        if (cardProtocol != null) {
            if (cardProtocol.getError() == null) {
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                        Intent intent = new Intent(getContext(), VerifyCardActivity.class);
                        // TODO обновить карту mCard
                        intent.putExtra("UID", cardProtocol.getCard().getUID());
                        intent.putExtra("Card", cardProtocol.getCard().getAsBundle());
                        startActivityForResult(intent, REQUEST_CODE_VERIFY_CARD);
                        //addCard(cardProtocol.getCard());
                    }
                });
            } else {
                // remove last UIDs because of error and no card read
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        lastReadSuccess = false;
                        if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allreadyShowed) {
                                new NoExtendedLengthSupportDialog().show(getActivity().getFragmentManager(), "NoExtendedLengthSupportDialog");
                            }
                        } else {
                            Toast.makeText(getContext(), "Try to scan again", Toast.LENGTH_LONG).show();
                        }
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                    }
                });
            }
        }

        progressBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    progressBar.setProgress(0);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                    progressBar.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 500);
    }

    public void OnReadProgress(CardProtocol protocol, final int progress) {
        progressBar.post(new Runnable() {
            @Override
            public void run() {
                progressBar.setProgress(progress);
            }
        });
    }

    public void OnReadCancel() {

        verifyCardTask = null;

        progressBar.postDelayed(new Runnable() {
            @Override
            public void run() {
                try {
                    progressBar.setProgress(0);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                    progressBar.setVisibility(View.INVISIBLE);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }, 500);
    }

    public void OnReadWait(int msec) {
        WaitSecurityDelayDialog.OnReadWait(getActivity(), msec);
    }

    @Override
    public void OnReadBeforeRequest(int timeout) {
        WaitSecurityDelayDialog.onReadBeforeRequest(getActivity(), timeout);
    }

    @Override
    public void OnReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(getActivity());
    }


    private class RateInfoTask extends ExchangeTask {
        protected void onPostExecute(List<ExchangeRequest> requests) {
            super.onPostExecute(requests);
            for (ExchangeRequest request : requests) {
                if (request.error == null) {
                    try {

                        JSONArray arr = request.getAnswerList();
                        for (int i = 0; i < arr.length(); ++i) {
                            JSONObject obj = arr.getJSONObject(i);
                            String currency = obj.getString("id");

                            boolean stop = false;
                            boolean stopAlter = false;
                            if (currency.equals(request.currency)) {
                                String usd = obj.getString("price_usd");

                                Float rate = Float.valueOf(usd);
                                mCard.setRate(rate);
                                UpdateViews();
                                stop = true;
                            }

                            if (currency.equals(request.currencyAlter)) {
                                String usd = obj.getString("price_usd");

                                Float rate = Float.valueOf(usd);
                                mCard.setRateAlter(rate);
                                UpdateViews();
                                stopAlter = true;
                            }

                            if (stop && stopAlter) {
                                break;
                            }

                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private class UpdateWalletInfoTask extends ElectrumTask {
        public UpdateWalletInfoTask(String host, int port) {
            super(host, port);
        }


        public UpdateWalletInfoTask(String host, int port, SharedData sharedData) {
            super(host, port, sharedData);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            updateTasks.remove(this);
            if (updateTasks.size() == 0) mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        protected void onPostExecute(List<ElectrumRequest> requests) {
            super.onPostExecute(requests);
            Log.i("RequestWalletInfoTask", "onPostExecute[" + String.valueOf(updateTasks.size()) + "]");
            updateTasks.remove(this);

            CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

            for (ElectrumRequest request : requests) {
                try {
                    if (request.error == null) {
                        if (request.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);
                                Long confBalance = request.getResult().getLong("confirmed");
                                Long unconf = request.getResult().getLong("unconfirmed");
                                if (sharedCounter != null) {
                                    int counter = sharedCounter.requestCounter.incrementAndGet();
                                    if (counter != 1) {
                                        continue;
                                    }
                                }

                                mCard.setBalanceConfirmed(confBalance);
                                mCard.setBalanceUnconfirmed(unconf);
                                mCard.setDecimalBalance(String.valueOf(confBalance));
                                mCard.setValidationNodeDescription(getValidationNodeDescription());
                            } catch (JSONException e) {
                                if (sharedCounter != null) {
                                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                    if (errCounter >= sharedCounter.allRequest) {
                                        e.printStackTrace();
                                        ErrorOnUpdate(e.toString());
                                        engine.SwitchNode(mCard);
                                        }
                                } else {
                                    e.printStackTrace();
                                    ErrorOnUpdate(e.toString());
                                    engine.SwitchNode(mCard);
                                }
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_SendTransaction)) {
                            try {
                                String hashTX = request.getResultString();

                                try {
                                    LastSignStorage.setLastMessage(mCard.getWallet(), hashTX);
                                    if (hashTX.startsWith("0x") || hashTX.startsWith("0X")) {
                                        hashTX = hashTX.substring(2);
                                    }
                                    BigInteger bigInt = new BigInteger(hashTX, 16); //TODO: очень плохой способ
                                    LastSignStorage.setTxWasSend(mCard.getWallet());
                                    LastSignStorage.setLastMessage(mCard.getWallet(), "");
                                    Log.e("TX_RESULT", hashTX);

                                } catch (Exception e) {
                                    engine.SwitchNode(mCard);
                                    ErrorOnUpdate("Failed to send transaction. Try again.");
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ErrorOnUpdate("Failed to send transaction. Try again.");
                                engine.SwitchNode(mCard);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);

                                JSONArray jsUnspentArray = request.getResultArray();
                                try {
                                    mCard.getUnspentTransactions().clear();
                                    for (int i = 0; i < jsUnspentArray.length(); i++) {
                                        JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                        TangemCard.UnspentTransaction trUnspent = new TangemCard.UnspentTransaction();
                                        trUnspent.txID = jsUnspent.getString("tx_hash");
                                        trUnspent.Amount = jsUnspent.getInt("value");
                                        trUnspent.Height = jsUnspent.getInt("height");
                                        mCard.getUnspentTransactions().add(trUnspent);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    ErrorOnUpdate(e.toString());
                                    engine.SwitchNode(mCard);
                                    }

                                for (int i = 0; i < jsUnspentArray.length(); i++) {
                                    JSONObject jsUnspent = jsUnspentArray.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {
                                        String nodeAddress = engine.GetNextNode(mCard);
                                        int nodePort = engine.GetNextNodePort(mCard);
                                        UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(nodeAddress, nodePort);

                                        updateTasks.add(updateWalletInfoTask);

                                        updateWalletInfoTask.execute(ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                                ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                ErrorOnUpdate(e.toString());
                                engine.SwitchNode(mCard);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetHistory)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);

                                JSONArray jsHistoryArray = request.getResultArray();
                                try {
                                    mCard.getHistoryTransactions().clear();
                                    for (int i = 0; i < jsHistoryArray.length(); i++) {
                                        JSONObject jsUnspent = jsHistoryArray.getJSONObject(i);
                                        TangemCard.HistoryTransaction trHistory = new TangemCard.HistoryTransaction();
                                        trHistory.txID = jsUnspent.getString("tx_hash");
                                        trHistory.Height = jsUnspent.getInt("height");
                                        mCard.getHistoryTransactions().add(trHistory);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    ErrorOnUpdate(e.toString());
                                    engine.SwitchNode(mCard);
                                }

                                for (int i = 0; i < jsHistoryArray.length(); i++) {
                                    JSONObject jsUnspent = jsHistoryArray.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {

                                        String nodeAddress = engine.GetNode(mCard);
                                        int nodePort = engine.GetNodePort(mCard);
                                        UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(nodeAddress, nodePort);
                                        updateTasks.add(updateWalletInfoTask);

                                        updateWalletInfoTask.execute(ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                                ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                    }

                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                ErrorOnUpdate(e.toString());
                                engine.SwitchNode(mCard);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetHeader)) {
                            try {
                                JSONObject jsHeader = request.getResult();
                                try {
                                    mCard.getHaedersInfo();
                                    mCard.UpdateHeaderInfo(new TangemCard.HeaderInfo(
                                            jsHeader.getInt("block_height"),
                                            jsHeader.getInt("timestamp")));
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                    ErrorOnUpdate(e.toString());
                                    engine.SwitchNode(mCard);

                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ErrorOnUpdate(e.toString());
                                engine.SwitchNode(mCard);

                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                            try {

                                String txHash = request.TxHash;
                                String raw = request.getResultString();

                                List<TangemCard.UnspentTransaction> listTx = mCard.getUnspentTransactions();
                                for (TangemCard.UnspentTransaction tx : listTx) {
                                    if (tx.txID.equals(txHash)) {
                                        tx.Raw = raw;
                                    }
                                }

                                List<TangemCard.HistoryTransaction> listHTx = mCard.getHistoryTransactions();
                                for (TangemCard.HistoryTransaction tx : listHTx) {
                                    if (tx.txID.equals(txHash)) {
                                        tx.Raw = raw;
                                        try {
                                            ArrayList<byte[]> prevHashes = BTCUtils.getPrevTX(raw);

                                            boolean isOur = false;
                                            for (byte[] hash : prevHashes) {
                                                String checkID = BTCUtils.toHex(hash);
                                                for (TangemCard.HistoryTransaction txForCheck : listHTx) {
                                                    if (txForCheck.txID == checkID) {
                                                        isOur = true;
                                                    }
                                                }
                                            }

                                            tx.isInput = !isOur;
                                        } catch (BitcoinException e) {
                                            e.printStackTrace();
                                            ErrorOnUpdate(e.toString());
                                        }
                                        Log.e("TX", raw);
                                    }
                                }

                            } catch (JSONException e) {
                                e.printStackTrace();
                                ErrorOnUpdate(e.toString());
                                engine.SwitchNode(mCard);
                            }
                        }
                        UpdateViews();
                    } else {
                        if (sharedCounter != null) {
                            int errCounter = sharedCounter.errorRequest.incrementAndGet();
                            if (errCounter >= sharedCounter.allRequest) {
                                ErrorOnUpdate(request.error);
                                engine.SwitchNode(mCard);

                            }
                        } else {
                            ErrorOnUpdate(request.error);
                            engine.SwitchNode(mCard);

                        }

                    }
                } catch (JSONException e) {
                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        if (errCounter >= sharedCounter.allRequest) {
                            e.printStackTrace();
                            ErrorOnUpdate(e.toString());
                        }
                    } else {
                        e.printStackTrace();
                        ErrorOnUpdate(e.toString());
                    }
                }
            }
            if (updateTasks.size() == 0) mSwipeRefreshLayout.setRefreshing(false);

        }
    }

    public static class PINSwapWarningDialog extends DialogFragment {

        LoadedWalletFragment activityFragment = null;
        String message;

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {

            return new AlertDialog.Builder(getActivity())
                    .setIcon(R.drawable.tangem_logo_small_new)
                    .setTitle("Your money is at risk!")
                    .setMessage(message)
                    .setCancelable(true)
                    .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            PINSwapWarningDialog.this.dismiss();
                        }
                    })
                    .setPositiveButton("Continue",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int whichButton) {
                                    if (activityFragment != null)
                                        activityFragment.startSwapPINActivity();
                                }
                            }
                    )
                    .create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
        }
    }

}

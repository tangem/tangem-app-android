package com.tangem.presentation.fragment;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.AppCompatButton;
import android.text.Html;
import android.text.Spanned;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.WriterException;
import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.request.ExchangeRequest;
import com.tangem.data.network.request.InfuraRequest;
import com.tangem.data.network.request.VerificationServerProtocol;
import com.tangem.data.network.task.VerificationServerTask;
import com.tangem.data.network.task.loaded_wallet.ETHRequestTask;
import com.tangem.data.network.task.loaded_wallet.RateInfoTask;
import com.tangem.data.network.task.loaded_wallet.UpdateWalletInfoTask;
import com.tangem.data.nfc.VerifyCardTask;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.BalanceValidator;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.LastSignStorage;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.domain.wallet.SharedData;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.activity.CreateNewWalletActivity;
import com.tangem.presentation.activity.MainActivity;
import com.tangem.presentation.activity.PreparePaymentActivity;
import com.tangem.presentation.activity.PurgeActivity;
import com.tangem.presentation.activity.RequestPINActivity;
import com.tangem.presentation.activity.SwapPINActivity;
import com.tangem.presentation.activity.VerifyCardActivity;
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog;
import com.tangem.presentation.dialog.PINSwapWarningDialog;
import com.tangem.presentation.dialog.WaitSecurityDelayDialog;
import com.tangem.util.Util;
import com.tangem.util.UtilHelper;
import com.tangem.wallet.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Timer;
import java.util.TimerTask;

import static android.content.Context.CLIPBOARD_SERVICE;

public class LoadedWallet extends Fragment implements SwipeRefreshLayout.OnRefreshListener, NfcAdapter.ReaderCallback, CardProtocol.Notifications {
    public static final String TAG = LoadedWallet.class.getSimpleName();

    private static final int REQUEST_CODE_SEND_PAYMENT = 1;
    private static final int REQUEST_CODE_VERIFY_CARD = 4;
    private static final int REQUEST_CODE_PURGE = 2;
    private static final int REQUEST_CODE_REQUEST_PIN2_FOR_PURGE = 3;
    private static final int REQUEST_CODE_ENTER_NEW_PIN = 5;
    private static final int REQUEST_CODE_ENTER_NEW_PIN2 = 6;
    private static final int REQUEST_CODE_REQUEST_PIN2_FOR_SWAP_PIN = 7;
    private static final int REQUEST_CODE_SWAP_PIN = 8;

    private NfcManager mNfcManager;
    public TangemCard mCard;
    private Tag lastTag;

    public SwipeRefreshLayout mSwipeRefreshLayout;
    private RelativeLayout rlProgressBar;
    private TextView tvCardID, tvBalance, tvBalanceLine1, tvBalanceLine2,tvOffline, tvBalanceEquivalent, tvWallet, tvInputs, tvError, tvMessage, tvIssuer, tvBlockchain, tvValidationNode, tvHeader, tvCaution;
    private ProgressBar progressBar;
    private ImageView ivBlockchain, ivPIN, ivPIN2orSecurityDelay, ivDeveloperVersion;
    private AppCompatButton btnExtract;

    public List<AsyncTask> updateTasks = new ArrayList<>();
    private boolean lastReadSuccess = true;
    private VerifyCardTask verifyCardTask = null;
    private int requestPIN2Count = 0;
    private Timer timerHideErrorAndMessage = null;
    private String newPIN = "", newPIN2 = "";
    private CardProtocol mCardProtocol;
    private int scanTimes = 0;
    OnlineVerifyTask onlineVerifyTask;

    public LoadedWallet() {

    }

    private class OnlineVerifyTask extends VerificationServerTask {

        @Override
        protected void onCancelled() {
            super.onCancelled();
            updateTasks.remove(this);
            onlineVerifyTask=null;
            if (updateTasks.size() == 0) mSwipeRefreshLayout.setRefreshing(false);
        }

        @Override
        protected void onPostExecute(List<VerificationServerProtocol.Request> requests) {
            super.onPostExecute(requests);
            Log.i("OnlineVerifyTask", "onPostExecute[" + String.valueOf(updateTasks.size()) + "]");
            updateTasks.remove(this);
            onlineVerifyTask=null;

            for (VerificationServerProtocol.Request request : requests) {
                if (request.error == null) {
                    VerificationServerProtocol.Verify.Answer answer = (VerificationServerProtocol.Verify.Answer) request.answer;
                    if (answer.error == null) {
                        mCard.setOnlineVerified(answer.results[0].passed);
                    } else {
                        mCard.setOnlineVerified(null);
                        errorOnUpdate(request.error);
                    }
                } else {
                    mCard.setOnlineVerified(null);
                    errorOnUpdate(request.error);
                }
            }
            if (updateTasks.size() == 0) mSwipeRefreshLayout.setRefreshing(false);
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mNfcManager = new NfcManager(getActivity(), this);

        mCard = new TangemCard(Objects.requireNonNull(getActivity()).getIntent().getStringExtra(TangemCard.EXTRA_CARD));
        mCard.LoadFromBundle(Objects.requireNonNull(getActivity().getIntent().getExtras()).getBundle(TangemCard.EXTRA_CARD));

        lastTag = getActivity().getIntent().getParcelableExtra(Main.EXTRA_LAST_DISCOVERED_TAG);
    }

    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fr_loaded_wallet, container, false);

        mSwipeRefreshLayout = v.findViewById(R.id.swipe_container);
        progressBar = v.findViewById(R.id.progressBar);
        rlProgressBar = v.findViewById(R.id.rlProgressBar);
        ImageView ivTangemCard = v.findViewById(R.id.ivTangemCard);
        tvBalance = v.findViewById(R.id.tvBalance);
        tvBalanceLine1 = v.findViewById(R.id.tvBalanceLine1);
        tvBalanceLine2 = v.findViewById(R.id.tvBalanceLine2);
        tvOffline = v.findViewById(R.id.tvOffline);
        tvCardID = v.findViewById(R.id.tvCardID);
        tvWallet = v.findViewById(R.id.tvWallet);
        tvInputs = v.findViewById(R.id.tvInputs);
        tvError = v.findViewById(R.id.tvError);
        tvMessage = v.findViewById(R.id.tvMessage);
        tvIssuer = v.findViewById(R.id.tvIssuer);
        tvHeader = v.findViewById(R.id.tvHeader);
        tvCaution = v.findViewById(R.id.tvCaution);
        ImageView imgLookup = v.findViewById(R.id.imgLookup);
        ImageView ivCopy = v.findViewById(R.id.ivCopy);
        tvValidationNode = v.findViewById(R.id.tvValidationNode);
        tvBlockchain = v.findViewById(R.id.tvBlockchain);
        ivBlockchain = v.findViewById(R.id.imgBlockchain);
        ivPIN = v.findViewById(R.id.imgPIN);
        ivPIN2orSecurityDelay = v.findViewById(R.id.imgPIN2orSecurityDelay);
        ivDeveloperVersion = v.findViewById(R.id.imgDeveloperVersion);
        ImageView ivQR = v.findViewById(R.id.qrWallet);
        FloatingActionButton fabInfo = v.findViewById(R.id.fabInfo);
        FloatingActionButton fabNFC = v.findViewById(R.id.fabNFC);
        AppCompatButton btnLoad = v.findViewById(R.id.btnLoad);
        btnExtract = v.findViewById(R.id.btnExtract);
        tvBalanceEquivalent = v.findViewById(R.id.tvBalanceEquivalent);

        mSwipeRefreshLayout.setOnRefreshListener(this);

        if (mCard.getBlockchain() == Blockchain.Token)
            tvBalance.setSingleLine(false);

        ivTangemCard.setImageResource(mCard.getCardImageResource());

        final CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        boolean visibleFlag = engine != null ? engine.InOutPutVisible() : true;
        int visibleIOPuts = visibleFlag ? View.VISIBLE : View.GONE;

        tvInputs.setVisibility(visibleIOPuts);

        try {
            ivQR.setImageBitmap(UtilHelper.INSTANCE.generateQrCode(Objects.requireNonNull(engine).getShareWalletURI(mCard).toString()));
        } catch (WriterException e) {
            e.printStackTrace();
        }

        updateViews();

        if (!mCard.hasBalanceInfo()) {
            mSwipeRefreshLayout.setRefreshing(true);
            mSwipeRefreshLayout.postDelayed(this::onRefresh, 1000);
        }

        if ( (mCard.isOnlineVerified()==null || !mCard.isOnlineVerified()) && onlineVerifyTask ==null) {
            onlineVerifyTask = new OnlineVerifyTask();
            updateTasks.add(onlineVerifyTask);
            onlineVerifyTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, VerificationServerProtocol.Verify.prepare(mCard));
        }

        startVerify(lastTag);

        tvWallet.setText(mCard.getWallet());

        // set listeners
        imgLookup.setOnClickListener(v15 -> {
            if (!mCard.hasBalanceInfo()) return;
            CoinEngine engineClick = CoinEngineFactory.Create(mCard.getBlockchain());
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Objects.requireNonNull(engineClick).getShareWalletURIExplorer(mCard));
            startActivity(browserIntent);
        });

        ivCopy.setOnClickListener(v14 -> doShareWallet(false));

        tvWallet.setOnClickListener(v12 -> doShareWallet(false));

        ivQR.setOnClickListener(v1 -> doShareWallet(true));

        btnLoad.setOnClickListener(v1 -> {
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Objects.requireNonNull(CoinEngineFactory.Create(mCard.getBlockchain())).getShareWalletURI(mCard));
                        intent.addCategory(Intent.CATEGORY_DEFAULT);
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        e.printStackTrace();
                    }
                }
        );

        fabInfo.setOnClickListener(v16 -> {
            if (mCardProtocol != null)
                openVerifyCard(mCardProtocol);
            else {
                scanTimes++;
                Toast.makeText(getContext(), R.string.need_attach_card_again, Toast.LENGTH_LONG).show();
            }
        });


        fabNFC.setOnClickListener(view -> {
            Intent intent = new Intent(getContext(), MainActivity.class);
            startActivity(intent);
        });

        btnExtract.setOnClickListener(v13 -> {
            if (!mCard.hasBalanceInfo()) {
                return;
            } else if (!Objects.requireNonNull(engine).IsBalanceNotZero(mCard)) {
                Toast.makeText(getContext(), R.string.wallet_empty, Toast.LENGTH_LONG).show();
                return;
            } else if (!engine.IsBalanceAlterNotZero(mCard)) {
                Toast.makeText(getContext(), R.string.not_enough_funds, Toast.LENGTH_LONG).show();
                return;
            } else if (engine.AwaitingConfirmation(mCard)) {
                Toast.makeText(getContext(), R.string.please_wait_while_previous, Toast.LENGTH_LONG).show();
                return;

            } else if (!engine.CheckUnspentTransaction(mCard)) {
                Toast.makeText(getContext(), R.string.please_wait_for_confirmation, Toast.LENGTH_LONG).show();
                return;
            } else if (mCard.getRemainingSignatures() == 0) {
                Toast.makeText(getContext(), R.string.card_hasn_t_remaining_signature, Toast.LENGTH_LONG).show();
                return;
            }

            Intent intent = new Intent(getContext(), PreparePaymentActivity.class);
            intent.putExtra("UID", mCard.getUID());
            intent.putExtra("Card", mCard.getAsBundle());
            startActivityForResult(intent, REQUEST_CODE_SEND_PAYMENT);
        });

        return v;
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
        for (AsyncTask ut : updateTasks) {
            ut.cancel(true);
        }
        mNfcManager.onStop();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_CODE_VERIFY_CARD:
                // action when erase wallet
                if (resultCode == Activity.RESULT_OK) {
                    if (getActivity() != null)
                        getActivity().finish();
                }
                break;

            case REQUEST_CODE_ENTER_NEW_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (data != null) {
                        if (data.getExtras() != null && data.getExtras().containsKey("confirmPIN")) {
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
                        if (data.getExtras() != null && data.getExtras().containsKey("confirmPIN2")) {
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
                    if (newPIN.equals(""))
                        newPIN = mCard.getPIN();

                    if (newPIN2.equals(""))
                        newPIN2 = PINStorage.getPIN2();

                    PINSwapWarningDialog pinSwapWarningDialog = new PINSwapWarningDialog();
                    pinSwapWarningDialog.setOnRefreshPage(this::startSwapPINActivity);
                    Bundle bundle = new Bundle();
                    if (!PINStorage.isDefaultPIN(newPIN) || !PINStorage.isDefaultPIN2(newPIN2))
                        bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_forget));
                    else
                        bundle.putString(PINSwapWarningDialog.EXTRA_MESSAGE, getString(R.string.if_you_use_default));
                    pinSwapWarningDialog.setArguments(bundle);
                    pinSwapWarningDialog.show(Objects.requireNonNull(getActivity()).getFragmentManager(), PINSwapWarningDialog.TAG);
                }
                break;

            case REQUEST_CODE_SWAP_PIN:
                if (resultCode == Activity.RESULT_OK) {
                    if (data == null) {
                        data = new Intent();
                        data.putExtra("UID", mCard.getUID());
                        data.putExtra("Card", mCard.getAsBundle());
                        data.putExtra("modification", "delete");
                    } else
                        data.putExtra("modification", "update");

                    if (getActivity() != null) {
                        getActivity().setResult(Activity.RESULT_OK, data);
                        getActivity().finish();
                    }
                } else {
                    if (data != null && data.getExtras() != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
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
                    if (getActivity() != null) {
                        getActivity().setResult(Activity.RESULT_OK, data);
                        getActivity().finish();
                    }
                } else {
                    if (data != null && data.getExtras() != null && data.getExtras().containsKey("UID") && data.getExtras().containsKey("Card")) {
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
                    updateViews();
                }
                break;
            case REQUEST_CODE_SEND_PAYMENT:
                if (resultCode == Activity.RESULT_OK) {
                    mSwipeRefreshLayout.postDelayed(this::onRefresh, 10000);
                    mSwipeRefreshLayout.setRefreshing(true);
                    mCard.clearInfo();
                    updateViews();
                }

                if (data != null && data.getExtras() != null) {
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
                    updateViews();
                }
                break;
        }
    }

    @Override
    public void onRefresh() {
        // update, showing refresh animation before making http call
        if (updateTasks.size() > 0) return;

        mSwipeRefreshLayout.setRefreshing(true);
        mCard.clearInfo();
        mCard.setError(null);
        mCard.setMessage(null);

        boolean needResendTX = LastSignStorage.getNeedTxSend(mCard.getWallet());

        updateViews();

        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

        if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet) {
            mCard.resetFailedBalanceRequestCounter();
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            mCard.resetFailedBalanceRequestCounter();
            for (int i = 0; i < data.allRequest; ++i) {
                String nodeAddress = Objects.requireNonNull(engine).GetNextNode(mCard);
                int nodePort = engine.GetNextNodePort(mCard);
                UpdateWalletInfoTask connectTaskEx = new UpdateWalletInfoTask(LoadedWallet.this, nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(mCard.getWallet()));

                UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(LoadedWallet.this, nodeAddress, nodePort, data);
                updateTasks.add(updateWalletInfoTask);
                updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(mCard.getWallet()));
            }

            RateInfoTask taskRate = new RateInfoTask(LoadedWallet.this);
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "bitcoin", "bitcoin");
            taskRate.execute(rate);


        } else if (mCard.getBlockchain() == Blockchain.BitcoinCash || mCard.getBlockchain() == Blockchain.BitcoinCashTestNet) {
            mCard.resetFailedBalanceRequestCounter();
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            for (int i = 0; i < data.allRequest; ++i) {
                String nodeAddress = Objects.requireNonNull(engine).GetNextNode(mCard);
                int nodePort = engine.GetNextNodePort(mCard);
                UpdateWalletInfoTask connectTaskEx = new UpdateWalletInfoTask(LoadedWallet.this, nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(mCard.getWallet()));
            }

            String nodeAddress = Objects.requireNonNull(engine).GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);
            UpdateWalletInfoTask updateWalletInfoTask = new UpdateWalletInfoTask(LoadedWallet.this,nodeAddress, nodePort, data);

            updateTasks.add(updateWalletInfoTask);
            updateWalletInfoTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(mCard.getWallet())
//                    ElectrumRequest.ListHistory(mCard.getWallet())
            );

            RateInfoTask taskRate = new RateInfoTask(LoadedWallet.this);
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "bitcoin-cash", "bitcoin-cash");
            taskRate.execute(rate);


        } else if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet) {
            ETHRequestTask updateETH = new ETHRequestTask(LoadedWallet.this, mCard.getBlockchain());
            InfuraRequest reqETH = InfuraRequest.GetBalance(mCard.getWallet());
            reqETH.setID(67);
            reqETH.setBlockchain(mCard.getBlockchain());

            InfuraRequest reqNonce = InfuraRequest.GetOutTransactionCount(mCard.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(mCard.getBlockchain());

            updateETH.execute(reqETH, reqNonce);


            RateInfoTask taskRate = new RateInfoTask(LoadedWallet.this);
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "ic_logo_ethereum", "ic_logo_ethereum");
            taskRate.execute(rate);

        } else if (mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask updateETH = new ETHRequestTask(LoadedWallet.this, mCard.getBlockchain());
            InfuraRequest reqETH = InfuraRequest.GetTokenBalance(mCard.getWallet(), Objects.requireNonNull(engine).GetContractAddress(mCard), engine.GetTokenDecimals(mCard));
            reqETH.setID(67);
            reqETH.setBlockchain(mCard.getBlockchain());

            InfuraRequest reqBalance = InfuraRequest.GetBalance(mCard.getWallet());
            reqBalance.setID(67);
            reqBalance.setBlockchain(mCard.getBlockchain());

            InfuraRequest reqNonce = InfuraRequest.GetOutTransactionCount(mCard.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(mCard.getBlockchain());
            updateETH.execute(reqETH, reqNonce, reqBalance);


            RateInfoTask taskRate = new RateInfoTask(LoadedWallet.this);
            ExchangeRequest rate = ExchangeRequest.GetRate(mCard.getWallet(), "basic-attention-token", "ic_logo_ethereum");
            taskRate.execute(rate);
        }

        if (needResendTX) {
            sendTransaction(LastSignStorage.getTxForSend(mCard.getWallet()));
        }
    }

    @Override
    public void onTagDiscovered(Tag tag) {
        startVerify(tag);
    }



    public Intent prepareResultIntent() {
        Intent data = new Intent();
        data.putExtra("UID", mCard.getUID());
        data.putExtra("Card", mCard.getAsBundle());
        return data;
    }

    @Override
    public void OnReadStart(CardProtocol cardProtocol) {
        progressBar.post(() -> {
            rlProgressBar.setVisibility(View.VISIBLE);
            //progressBar.setVisibility(View.VISIBLE);
            progressBar.setProgress(5);
        });
    }

    @Override
    public void OnReadFinish(final CardProtocol cardProtocol) {
        verifyCardTask = null;

        if (cardProtocol != null) {
            if (cardProtocol.getError() == null) {
                progressBar.post(() -> {
                    rlProgressBar.setVisibility(View.GONE);
                    progressBar.setProgress(100);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

                    mCardProtocol = cardProtocol;

//                    Log.i(TAG, "scanTimes " + scanTimes);
                    if (scanTimes > 0)
                        openVerifyCard(mCardProtocol);

                    scanTimes++;


                    //addCard(cardProtocol.getCard());
                });
            } else {
                // remove last UIDs because of error and no card read
                progressBar.post(() -> {
                    lastReadSuccess = false;
                    if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {
                        if (!NoExtendedLengthSupportDialog.allreadyShowed) {
                            new NoExtendedLengthSupportDialog().show(Objects.requireNonNull(getActivity()).getFragmentManager(), NoExtendedLengthSupportDialog.TAG);
                        }
                    } else {
                        Toast.makeText(getContext(), R.string.try_to_scan_again, Toast.LENGTH_SHORT).show();
                    }
                    progressBar.setProgress(100);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                });
            }
        }

        progressBar.postDelayed(() -> {
            try {
                rlProgressBar.setVisibility(View.GONE);
                progressBar.setProgress(0);
                progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                progressBar.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500);
    }

    @Override
    public void OnReadProgress(CardProtocol protocol, final int progress) {
        progressBar.post(() -> progressBar.setProgress(progress));
    }

    @Override
    public void OnReadCancel() {
        verifyCardTask = null;
        progressBar.postDelayed(() -> {
            try {
                rlProgressBar.setVisibility(View.GONE);
                progressBar.setProgress(0);
                progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
                progressBar.setVisibility(View.INVISIBLE);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 500);
    }

    @Override
    public void OnReadWait(int msec) {
        WaitSecurityDelayDialog.OnReadWait(Objects.requireNonNull(getActivity()), msec);
    }

    @Override
    public void OnReadBeforeRequest(int timeout) {
        WaitSecurityDelayDialog.onReadBeforeRequest(Objects.requireNonNull(getActivity()), timeout);
    }

    @Override
    public void OnReadAfterRequest() {
        WaitSecurityDelayDialog.onReadAfterRequest(Objects.requireNonNull(getActivity()));
    }

    public void updateViews() {
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
                    //tvMessage.setText(R.string.sending_cached_transaction);
                } else {
                    tvMessage.setText(mCard.getMessage());
                }
                tvMessage.setVisibility(View.VISIBLE);
            }

            CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());

            if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet) {

                BalanceValidator validator = new BalanceValidator();
                validator.Check(mCard);
                tvBalanceLine1.setText(validator.GetFirstLine());
                tvBalanceLine2.setText(validator.GetSecondLine());
            }

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
                    tvBalance.setText(R.string.not_implemented);
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
            if (mCard.getLastInputDescription().contains("awaiting"))
                tvInputs.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.not_confirmed));
            else if (mCard.getLastInputDescription().contains("None"))
                tvInputs.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.primary_dark));
            else
                tvInputs.setTextColor(ContextCompat.getColor(Objects.requireNonNull(getContext()), R.color.confirmed));

            tvBlockchain.setText(mCard.getBlockchainName());
            ivBlockchain.setImageResource(mCard.getBlockchain().getImageResource(getContext(), mCard.getTokenSymbol()));

            if (tvValidationNode != null) {
                tvValidationNode.setText(mCard.getValidationNodeDescription());
            }

            if (mCard.useDefaultPIN1()) {
                ivPIN.setImageResource(R.drawable.unlock_pin1);
                ivPIN.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_default_PIN1_code, Toast.LENGTH_LONG).show());
            } else {
                ivPIN.setImageResource(R.drawable.lock_pin1);
                ivPIN.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_user_PIN1_code, Toast.LENGTH_LONG).show());
            }

            if (mCard.getPauseBeforePIN2() > 0 && (mCard.useDefaultPIN2() || !mCard.useSmartSecurityDelay())) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.timer);
                ivPIN2orSecurityDelay.setOnClickListener(v -> Toast.makeText(getContext(), String.format("This banknote will enforce %.0f seconds security delay for all operations requiring PIN2 code", mCard.getPauseBeforePIN2() / 1000.0), Toast.LENGTH_LONG).show());

            } else if (mCard.useDefaultPIN2()) {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.unlock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_default_PIN2_code, Toast.LENGTH_LONG).show());
            } else {
                ivPIN2orSecurityDelay.setImageResource(R.drawable.lock_pin2);
                ivPIN2orSecurityDelay.setOnClickListener(v -> Toast.makeText(getContext(), R.string.this_banknote_protected_user_PIN2_code, Toast.LENGTH_LONG).show());
            }

            if (mCard.useDevelopersFirmware()) {
                ivDeveloperVersion.setImageResource(R.drawable.ic_developer_version);
                ivDeveloperVersion.setVisibility(View.VISIBLE);
                ivDeveloperVersion.setOnClickListener(v -> Toast.makeText(getContext(), R.string.unlocked_banknote_only_development_use, Toast.LENGTH_LONG).show());
            } else {
                ivDeveloperVersion.setVisibility(View.INVISIBLE);
            }


            if (mCard.hasBalanceInfo())
                btnExtract.setEnabled(true);
            else
                btnExtract.setEnabled(false);

            tvIssuer.setText(mCard.getIssuerDescription());

            timerHideErrorAndMessage = new Timer();

            timerHideErrorAndMessage.schedule(new TimerTask() {
                @Override
                public void run() {
                    tvError.post(() -> {
                        tvMessage.setVisibility(View.GONE);
                        tvError.setVisibility(View.GONE);
                        mCard.setError(null);
                        mCard.setMessage(null);
                    });
                }
            }, 5000);

            if (mCard.isReusable()) {
                tvHeader.setText(R.string.reusable_wallet);
                tvCaution.setVisibility(View.GONE);
            } else {
                if (mCard.getMaxSignatures() == mCard.getRemainingSignatures()) {
                    tvHeader.setText(R.string.banknote);
                    tvCaution.setVisibility(View.GONE);
                } else {
                    tvHeader.setText(R.string.not_transferable_banknote);
                    tvCaution.setVisibility(View.VISIBLE);
                }
            }

            if (mCard.useDevelopersFirmware()) {
                tvHeader.setText(R.string.developer_kit);
                tvCaution.setVisibility(View.VISIBLE);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void errorOnUpdate(String message) {
//        mCard.setError(getString(R.string.cannot_obtain_data_from_blockchain));
//        updateViews();
    }

    private void openVerifyCard(CardProtocol cardProtocol) {
        Intent intent = new Intent(getContext(), VerifyCardActivity.class);
        intent.putExtra("UID", cardProtocol.getCard().getUID());
        intent.putExtra("Card", cardProtocol.getCard().getAsBundle());
        startActivityForResult(intent, REQUEST_CODE_VERIFY_CARD);
    }

    private void startVerify(Tag tag) {
        try {
            final IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                throw new CardProtocol.TangemException(getString(R.string.wrong_tag_err));
            }
            byte UID[] = tag.getId();
            String sUID = Util.byteArrayToHexString(UID);
            if (!mCard.getUID().equals(sUID)) {
                Log.d(TAG, "Invalid UID: " + sUID);
                mNfcManager.ignoreTag(isoDep.getTag());
                return;
            } else {
                Log.v(TAG, "UID: " + sUID);
            }

            if (lastReadSuccess) {
                isoDep.setTimeout(1000);
            } else {
                isoDep.setTimeout(65000);
            }

            verifyCardTask = new VerifyCardTask(getContext(), mCard, mNfcManager, isoDep, this);
            verifyCardTask.start();

//            Log.i(TAG, "onTagDiscovered " + Arrays.toString(tag.getId()));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void doShareWallet(boolean useURI) {
        if (useURI) {
            String txtShare = CoinEngineFactory.Create(mCard.getBlockchain()).getShareWalletURI(mCard).toString();
            //String txtShare = Blockchain.getShareWalletURI(mCard).toString();
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Wallet address");
            intent.putExtra(Intent.EXTRA_TEXT, txtShare);

            PackageManager packageManager = Objects.requireNonNull(getActivity()).getPackageManager();
            List activities = packageManager.queryIntentActivities(intent, PackageManager.MATCH_ALL);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) {
                String title = getString(R.string.share_wallet_address_with);

                // create intent to show chooser
                Intent chooser = Intent.createChooser(intent, title);

                // verify the intent will resolve to at least one activity
                if (intent.resolveActivity(getActivity().getPackageManager()) != null) {
                    startActivity(chooser);
                }
            } else {
                ClipboardManager clipboard = (ClipboardManager) getActivity().getSystemService(CLIPBOARD_SERVICE);
                Objects.requireNonNull(clipboard).setPrimaryClip(ClipData.newPlainText(txtShare, txtShare));
                Toast.makeText(getContext(), R.string.copied_clipboard, Toast.LENGTH_LONG).show();
            }
        } else {
            String txtShare = mCard.getWallet();
            ClipboardManager clipboard = (ClipboardManager) Objects.requireNonNull(getActivity()).getSystemService(CLIPBOARD_SERVICE);
            Objects.requireNonNull(clipboard).setPrimaryClip(ClipData.newPlainText(txtShare, txtShare));
            Toast.makeText(getContext(), R.string.copied_clipboard, Toast.LENGTH_LONG).show();
        }
    }

    private void sendTransaction(String tx) {
        CoinEngine engine = CoinEngineFactory.Create(mCard.getBlockchain());
        if (mCard.getBlockchain() == Blockchain.Ethereum || mCard.getBlockchain() == Blockchain.EthereumTestNet || mCard.getBlockchain() == Blockchain.Token) {
            ETHRequestTask task = new ETHRequestTask(LoadedWallet.this, mCard.getBlockchain());
            InfuraRequest req = InfuraRequest.SendTransaction(mCard.getWallet(), tx);
            req.setID(67);
            req.setBlockchain(mCard.getBlockchain());
            task.execute(req);
        } else if (mCard.getBlockchain() == Blockchain.Bitcoin || mCard.getBlockchain() == Blockchain.BitcoinTestNet) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);

            UpdateWalletInfoTask connectTask = new UpdateWalletInfoTask(LoadedWallet.this, nodeAddress, nodePort);
            connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.Broadcast(mCard.getWallet(), tx));
        } else if (mCard.getBlockchain() == Blockchain.BitcoinCash || mCard.getBlockchain() == Blockchain.BitcoinCashTestNet) {
            String nodeAddress = engine.GetNode(mCard);
            int nodePort = engine.GetNodePort(mCard);

            UpdateWalletInfoTask connectTask = new UpdateWalletInfoTask(LoadedWallet.this, nodeAddress, nodePort);
            connectTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.Broadcast(mCard.getWallet(), tx));
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

}
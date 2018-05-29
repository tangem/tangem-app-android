package com.tangem.presentation.fragment;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.util.Util;
import com.tangem.presentation.activity.EmptyWalletActivity;
import com.tangem.presentation.activity.LoadedWalletActivity;
import com.tangem.presentation.activity.MainActivity;
import com.tangem.presentation.activity.RequestPINActivity;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.presentation.adapter.CardListAdapter;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.data.network.task.ElectrumTask;
import com.tangem.data.network.request.ExchangeRequest;
import com.tangem.data.network.task.ExchangeTask;
import com.tangem.domain.wallet.Infura_Request;
import com.tangem.data.network.task.InfuraTask;
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog;
import com.tangem.domain.wallet.PINStorage;
import com.tangem.wallet.R;
import com.tangem.domain.wallet.SharedData;
import com.tangem.presentation.dialog.WaitSecurityDelayDialog;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;


/**
 * A placeholder fragment containing a simple view.
 */
public class MainFragment extends Fragment implements NfcAdapter.ReaderCallback, CardListAdapter.UiCallbacks, CardProtocol.Notifications, MainActivity.OnCardsClean {

    private static final int REQUEST_CODE_SHOW_CARD_ACTIVITY = 1;
    private static final int REQUEST_CODE_ENTER_PIN_ACTIVITY = 2;
    private static final int REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS = 3;
    private NfcManager mNfcManager;
    private ArrayList<String> slCardUIDs = new ArrayList<>();
    private static final String logTag = "MainFragment";
    private ProgressBar progressBar;

    private CardListAdapter mCardListAdapter;

    public CardListAdapter getCardListAdapter() {
        return mCardListAdapter;
    }

    private ReadCardInfoTask readCardInfoTask;
    private SwipeRefreshLayout mSwipeRefreshLayout; //TODO: tmp
    List<RequestWalletInfoTask> requestTasks = new ArrayList<>();

    public MainFragment() {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mCardListAdapter.onSaveInstanceState(outState);
        outState.putStringArrayList("slCardUIDs", slCardUIDs);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View result = inflater.inflate(R.layout.fr_main, container, false);

        mNfcManager = new NfcManager(this.getActivity(), this);
        verifyPermissions();

        progressBar = result.findViewById(R.id.progressBar);
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
        RecyclerView rvCards = result.findViewById(R.id.lvCards);
        rvCards.setLayoutManager(new LinearLayoutManager(getContext()));
        mCardListAdapter = new CardListAdapter(getActivity().getLayoutInflater(), savedInstanceState, this);
        rvCards.setAdapter(mCardListAdapter);
        if (savedInstanceState != null && savedInstanceState.containsKey("slCardUIDs")) {
            slCardUIDs = savedInstanceState.getStringArrayList("slCardUIDs");
        }

        // SwipeRefreshLayout
        mSwipeRefreshLayout = result.findViewById(R.id.swipe_container); //TODO: tmp

        if( mSwipeRefreshLayout!=null ) {
            SwipeRefreshLayout.OnRefreshListener onRefreshListener = new SwipeRefreshLayout.OnRefreshListener() {

                @Override
                public void onRefresh() {
                    //Update
                    // Showing refresh animation before making http call

                    if (requestTasks.size() > 0) return;

                    if (requestTasks.size() > 0) { //TODO: tmp
                        mSwipeRefreshLayout.setRefreshing(true);
                    } else {
                        mSwipeRefreshLayout.setRefreshing(false);
                    }
                }
            };
        mSwipeRefreshLayout.setOnRefreshListener(onRefreshListener); //TODO: tmp
        }

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                //Remove swiped item from list and notify the RecyclerView
                int cardIndex = viewHolder.getAdapterPosition();
                if (cardIndex < 0 || cardIndex >= mCardListAdapter.getItemCount()) return;
                slCardUIDs.remove(mCardListAdapter.getCard(cardIndex).getUID());
                if (mCardListAdapter.getCard(cardIndex).getUID() == lastRead_UID) {
                    lastRead_UID = "";
                }
                mCardListAdapter.removeCard(cardIndex);
                if (mCardListAdapter.getItemCount() == 0 && getActivity().getClass() == MainActivity.class) {
                    ((MainActivity) getActivity()).hideCleanButton();
                }
            }
        });

        itemTouchHelper.attachToRecyclerView(rvCards);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setOnCardsClean(this);
//            ((MainActivity) getActivity()).setOnCreateNFCDialog(this);
            ((MainActivity) getActivity()).setNfcAdapterReaderCallback(this);
        }

        return result;
    }

    private void verifyPermissions() {
        NfcManager.verifyPermissions(getActivity());
        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Log.e("QRScanActivity", "User hasn't granted permission to use camera");
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS);
        }
    }

    int unsuccessReadCount = 0;
    Tag lastTag = null;

    @Override
    public void onTagDiscovered(Tag tag) {
        try {
            // get IsoDep handle and run cardReader thread
            final IsoDep isoDep = IsoDep.get(tag);
            if (isoDep == null) {
                throw new CardProtocol.TangemException(getString(R.string.wrong_tag_err));
            }

            byte UID[] = tag.getId();
            String sUID = Util.byteArrayToHexString(UID);
            if (slCardUIDs.indexOf(sUID) != -1) {
                Log.d(logTag, "Repeat UID: " + sUID);
                mNfcManager.IgnoreTag(isoDep.getTag());
                return;
            } else {
                Log.v(logTag, "UID: " + sUID);
            }

//            Log.e(logTag,"setTimeout("+String.valueOf(1000 + 3000 * unsuccessReadCount)+")");
            if (unsuccessReadCount < 2) {
                isoDep.setTimeout(2000 + 5000 * unsuccessReadCount);
            } else {
                isoDep.setTimeout(90000);
            }
            lastTag = tag;

            readCardInfoTask = new ReadCardInfoTask(isoDep, this);
            readCardInfoTask.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        mNfcManager.onResume();
    }

    @Override
    public void onPause() {
        mNfcManager.onPause();
        if (readCardInfoTask != null) {
            readCardInfoTask.cancel(true);
        }
        super.onPause();
    }

    @Override
    public void onStop() {
        // dismiss enable NFC dialog
        mNfcManager.onStop();
        if (readCardInfoTask != null) {
            readCardInfoTask.cancel(true);
        }
        for (RequestWalletInfoTask rt : requestTasks) {
            rt.cancel(true);
        }
        super.onStop();
    }

    public void refreshCard(TangemCard card) {
        CoinEngine engine = CoinEngineFactory.Create(card.getBlockchain());
        if (card.getBlockchain() == Blockchain.Ethereum || card.getBlockchain() == Blockchain.EthereumTestNet) {
            ETHRequestTask task = new ETHRequestTask(card.getBlockchain());
            Infura_Request req = Infura_Request.GetBalance(card.getWallet());
            req.setID(67);
            req.setBlockchain(card.getBlockchain());
            Infura_Request reqNonce = Infura_Request.GetOutTransactionCount(card.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(card.getBlockchain());

            task.execute(req, reqNonce);

            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "ethereum", "ethereum");
            taskRate.execute(rate);

        } else if (card.getBlockchain() == Blockchain.BitcoinTestNet || card.getBlockchain() == Blockchain.Bitcoin) {
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            for (int i = 0; i < data.allRequest; ++i) {

                String nodeAddress = engine.GetNextNode(card);
                int nodePort = engine.GetNextNodePort(card);
                RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));
            }

            String nodeAddress = engine.GetNode(card);
            int nodePort = engine.GetNodePort(card);
            RequestWalletInfoTask task = new RequestWalletInfoTask(nodeAddress, nodePort);
            if( mSwipeRefreshLayout!=null ) mSwipeRefreshLayout.setRefreshing(true); //TODO: tmp
            requestTasks.add(task);
            task.execute(ElectrumRequest.ListUnspent(card.getWallet()), ElectrumRequest.ListHistory(card.getWallet()));
            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "bitcoin", "bitcoin");
            taskRate.execute(rate);

        }
        else if (card.getBlockchain() == Blockchain.BitcoinCashTestNet || card.getBlockchain() == Blockchain.BitcoinCash) {
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            for (int i = 0; i < data.allRequest; ++i) {

                String nodeAddress = engine.GetNextNode(card);
                int nodePort = engine.GetNextNodePort(card);
                RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));
            }

            String nodeAddress = engine.GetNode(card);
            int nodePort = engine.GetNodePort(card);
            RequestWalletInfoTask task = new RequestWalletInfoTask(nodeAddress, nodePort);
            if( mSwipeRefreshLayout!=null ) mSwipeRefreshLayout.setRefreshing(true); //TODO: tmp
            requestTasks.add(task);
            task.execute(ElectrumRequest.ListUnspent(card.getWallet()), ElectrumRequest.ListHistory(card.getWallet()));
            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "bitcoin-cash", "bitcoin-cash");
            taskRate.execute(rate);

        }else if (card.getBlockchain() == Blockchain.Token) {
            ETHRequestTask updateETH = new ETHRequestTask(card.getBlockchain());
            Infura_Request reqETH = Infura_Request.GetTokenBalance(card.getWallet(), engine.GetContractAddress(card), engine.GetTokenDecimals(card));
            reqETH.setID(67);
            reqETH.setBlockchain(card.getBlockchain());


            Infura_Request reqBalance = Infura_Request.GetBalance(card.getWallet());
            reqBalance.setID(67);
            reqBalance.setBlockchain(card.getBlockchain());


            RateInfoTask taskRate = new RateInfoTask();
            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "basic-attention-token", "ethereum");
            taskRate.execute(rate);

            Infura_Request reqNonce = Infura_Request.GetOutTransactionCount(card.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(card.getBlockchain());

            updateETH.execute(reqETH, reqNonce, reqBalance);
        }
    }

    public void addCard(final TangemCard card) {
        if (mCardListAdapter != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    unsuccessReadCount = 0;
                    slCardUIDs.add(0, card.getUID());
                    mCardListAdapter.addCard(card);

                    CoinEngine engine = CoinEngineFactory.Create(card.getBlockchain());

                    if (card.getStatus() == TangemCard.Status.Loaded) {

                        if (card.getBlockchain() == Blockchain.Ethereum || card.getBlockchain() == Blockchain.EthereumTestNet) {
                            ETHRequestTask task = new ETHRequestTask(card.getBlockchain());
                            Infura_Request req = Infura_Request.GetBalance(card.getWallet());
                            req.setID(67);
                            req.setBlockchain(card.getBlockchain());
                            Infura_Request reqNonce = Infura_Request.GetOutTransactionCount(card.getWallet());
                            reqNonce.setID(67);
                            reqNonce.setBlockchain(card.getBlockchain());

                            task.execute(req, reqNonce);

                            RateInfoTask taskRate = new RateInfoTask();
                            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "ethereum", "ethereum");
                            taskRate.execute(rate);

                        } else if (card.getBlockchain() == Blockchain.BitcoinTestNet || card.getBlockchain() == Blockchain.Bitcoin) {
                            SharedData data = new SharedData(SharedData.COUNT_REQUEST);

                            for (int i = 0; i < data.allRequest; ++i) {
                                String nodeAddress = engine.GetNextNode(card);
                                int nodePort = engine.GetNextNodePort(card);

                                RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(nodeAddress, nodePort, data);
                                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));
                            }

                            String nodeAddress = engine.GetNode(card);
                            int nodePort = engine.GetNodePort(card);

                            RequestWalletInfoTask task = new RequestWalletInfoTask(nodeAddress, nodePort);
                            if( mSwipeRefreshLayout!=null ) mSwipeRefreshLayout.setRefreshing(true); //TODO: tmp

                            requestTasks.add(task);
                            task.execute(ElectrumRequest.ListUnspent(card.getWallet()), ElectrumRequest.ListHistory(card.getWallet()));
                            RateInfoTask taskRate = new RateInfoTask();
                            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "bitcoin", "bitcoin");
                            taskRate.execute(rate);

                        }else if (card.getBlockchain() == Blockchain.BitcoinCashTestNet || card.getBlockchain() == Blockchain.BitcoinCash) {
                            SharedData data = new SharedData(SharedData.COUNT_REQUEST);

                            for (int i = 0; i < data.allRequest; ++i) {
                                String nodeAddress = engine.GetNextNode(card);
                                int nodePort = engine.GetNextNodePort(card);

                                RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(nodeAddress, nodePort, data);
                                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));
                            }

                            String nodeAddress = engine.GetNode(card);
                            int nodePort = engine.GetNodePort(card);

                            RequestWalletInfoTask task = new RequestWalletInfoTask(nodeAddress, nodePort);
                            if( mSwipeRefreshLayout!=null ) mSwipeRefreshLayout.setRefreshing(true); //TODO: tmp

                            requestTasks.add(task);
                            task.execute(ElectrumRequest.ListUnspent(card.getWallet()), ElectrumRequest.ListHistory(card.getWallet()));
                            RateInfoTask taskRate = new RateInfoTask();
                            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "bitcoin-cash", "bitcoin-cash");
                            taskRate.execute(rate);

                        }
                        else if (card.getBlockchain() == Blockchain.Token) {
                            ETHRequestTask updateETH = new ETHRequestTask(card.getBlockchain());
                            Infura_Request reqETH = Infura_Request.GetTokenBalance(card.getWallet(), engine.GetContractAddress(card), engine.GetTokenDecimals(card));
                            reqETH.setID(67);
                            reqETH.setBlockchain(card.getBlockchain());


                            Infura_Request reqBalance = Infura_Request.GetBalance(card.getWallet());
                            reqBalance.setID(67);
                            reqBalance.setBlockchain(card.getBlockchain());


                            RateInfoTask taskRate = new RateInfoTask();
                            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "basic-attention-token", "ethereum");
                            taskRate.execute(rate);

                            Infura_Request reqNonce = Infura_Request.GetOutTransactionCount(card.getWallet());
                            reqNonce.setID(67);
                            reqNonce.setBlockchain(card.getBlockchain());

                            updateETH.execute(reqETH, reqNonce, reqBalance);
                        }
                    }
                    if (getActivity().getClass() == MainActivity.class) {
                        ((MainActivity) getActivity()).showCleanButton();
                    }
                }
            });
        }
    }

    @Override
    public void onViewCard(Bundle cardInfo) {

        if( mSwipeRefreshLayout!=null ) mSwipeRefreshLayout.setRefreshing(false); //TODO: tmp

        String UID = cardInfo.getString("UID");
        TangemCard card = new TangemCard(UID);
        card.LoadFromBundle(cardInfo.getBundle("Card"));


        Intent intent;

        if (card.getStatus() == TangemCard.Status.Empty) {
            intent = new Intent(getActivity(), EmptyWalletActivity.class);
        } else if (card.getStatus() == TangemCard.Status.Loaded) {
            intent = new Intent(getActivity(), LoadedWalletActivity.class);
        } else if (card.getStatus() == TangemCard.Status.NotPersonalized || card.getStatus() == TangemCard.Status.Purged) {
            return;
        } else {
            intent = new Intent(getActivity(), LoadedWalletActivity.class);
        }

        intent.putExtras(cardInfo);
        startActivityForResult(intent, REQUEST_CODE_SHOW_CARD_ACTIVITY);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(logTag, "ActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);
        // если пришло ОК
        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SHOW_CARD_ACTIVITY) {
            if (data != null && data.getExtras().containsKey("UID")) {
                final TangemCard card = new TangemCard(data.getStringExtra("UID"));
                card.LoadFromBundle(data.getBundleExtra("Card"));
                if (data.getStringExtra("modification").equals("delete")) {
                    mCardListAdapter.removeCard(card);
                    for (int i = 0; i < slCardUIDs.size(); i++) {
                        if (slCardUIDs.get(i).equals(card.getUID())) {
                            slCardUIDs.remove(i);
                            break;
                        }
                    }
                    if (mCardListAdapter.getItemCount() == 0 && getActivity().getClass() == MainActivity.class) {
                        ((MainActivity) getActivity()).hideCleanButton();
                    }
                } else if (data.getStringExtra("modification").equals("update")) {
                    mCardListAdapter.updateCard(card);
                } else if (data.getStringExtra("modification").equals("updateAndViewCard")) {
                    mCardListAdapter.updateCard(card);
                    onViewCard(data.getExtras());
                }

            }
        } else if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_ENTER_PIN_ACTIVITY) {
            if (lastTag != null) onTagDiscovered(lastTag);
        }
    }

    @Override
    public void doClean() {
        mCardListAdapter.clearCards();
        slCardUIDs.clear();
        for (RequestWalletInfoTask rt : requestTasks) {
            rt.cancel(true);
        }
        lastRead_UID = "";
    }

    public void doEnterPIN() {
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN.toString());
        startActivityForResult(intent, REQUEST_CODE_ENTER_PIN_ACTIVITY);
    }

    private static String lastRead_UID = "";
    private static ArrayList<String> lastRead_UnsuccessfullPINs = new ArrayList<>();
    private static TangemCard.EncryptionMode lastRead_Encryption = null;

    private class ReadCardInfoTask extends Thread {


        IsoDep mIsoDep;
        CardProtocol.Notifications mNotifications;
        private boolean isCancelled = false;

        ReadCardInfoTask(IsoDep isoDep, CardProtocol.Notifications notifications) {
            mIsoDep = isoDep;
            mNotifications = notifications;
        }

        @Override
        public void run() {
            if (mIsoDep == null) {
                return;
            }
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
                    CardProtocol protocol = new CardProtocol(getContext(), mIsoDep, mNotifications);
                    mNotifications.OnReadStart(protocol);
                    try {
                        mNotifications.OnReadProgress(protocol, 5);

                        byte[] UID = mIsoDep.getTag().getId();
                        String sUID = Util.byteArrayToHexString(UID);
                        if (!lastRead_UID.equals(sUID)) {
                            lastRead_UID = sUID;
                            lastRead_UnsuccessfullPINs.clear();
                            lastRead_Encryption = null;
                        }

                        Log.i("ReadCardInfoTask", "[-- Start read card info --]");

                        if (isCancelled) return;

                        protocol.setPIN(PINStorage.getDefaultPIN());
                        protocol.clearReadResult();

                        if (lastRead_Encryption == null) {
                            Log.i("ReadCardInfoTask", "Try get supported encryption mode");
                            protocol.run_GetSupportedEncryption();
                        } else {
                            Log.i("ReadCardInfoTask", "Use already defined encryption mode: " + lastRead_Encryption.name());
                            protocol.getCard().encryptionMode = lastRead_Encryption;
                        }

                        if (protocol.haveReadResult()) {
                            //already have read result (obtained while get supported encryption), only read issuer data and define offline balance
                            protocol.parseReadResult();
                            protocol.run_ReadOrWriteIssuerDataAndDefineOfflineBalance();
                            mNotifications.OnReadProgress(protocol, 60);
                            PINStorage.setLastUsedPIN(protocol.getCard().getPIN());
                        } else {
                            //don't have read result - may be don't get supported encryption on this try, need encryption or need another PIN
                            if (lastRead_Encryption == null) {
                                // we try get supported encryption on this time
                                lastRead_Encryption = protocol.getCard().encryptionMode;
                                if (protocol.getCard().encryptionMode == TangemCard.EncryptionMode.None) {
                                    // default pin not accepted
                                    lastRead_UnsuccessfullPINs.add(PINStorage.getDefaultPIN());
                                }
                            }

                            boolean pinFound = false;
                            for (String PIN : PINStorage.getPINs()) {
                                Log.e("ReadCardInfoTask", "PIN: " + PIN);

                                boolean skipPin = false;
                                for (int i = 0; i < lastRead_UnsuccessfullPINs.size(); i++) {
                                    if (lastRead_UnsuccessfullPINs.get(i).equals(PIN)) {
                                        skipPin = true;
                                        break;
                                    }
                                }

                                if (skipPin) {
                                    Log.e("ReadCardInfoTask", "Skip PIN - already checked before");
                                    continue;
                                }

                                try {
                                    protocol.setPIN(PIN);
                                    if (protocol.getCard().encryptionMode != TangemCard.EncryptionMode.None) {
                                        protocol.CreateProtocolKey();
                                    }
                                    protocol.run_Read();
                                    mNotifications.OnReadProgress(protocol, 60);
                                    PINStorage.setLastUsedPIN(PIN);
                                    pinFound = true;
                                    protocol.getCard().setPIN(PIN);
                                    break;
                                } catch (CardProtocol.TangemException_InvalidPIN e) {
                                    Log.e(logTag, e.getMessage());
                                    lastRead_UnsuccessfullPINs.add(PIN);
                                }
                            }
                            if (!pinFound) {
                                throw new CardProtocol.TangemException_InvalidPIN("No valid PIN found!");
                            }
                        }

                        protocol.run_CheckPIN2isDefault();

                    } catch (Exception e) {
                        e.printStackTrace();
                        protocol.setError(e);

                    } finally {
                        Log.i("ReadCardInfoTask", "[-- Finish read card info --]");
                        mNotifications.OnReadFinish(protocol);
                    }
                } finally {
                    mNfcManager.IgnoreTag(mIsoDep.getTag());
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        void cancel(Boolean AllowInterrupt) {
            try {
                if (this.isAlive()) {
                    isCancelled = true;
                    join(500);
                }
                if (this.isAlive() && AllowInterrupt) {
                    interrupt();
                    mNotifications.OnReadCancel();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
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

        readCardInfoTask = null;

        if (cardProtocol != null) {
            if (cardProtocol.getError() == null) {
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));
                        addCard(cardProtocol.getCard());
                    }
                });
            } else {
                // remove last UIDs because of error and no card read
                progressBar.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getContext(), "Try to scan again", Toast.LENGTH_LONG).show();
                        unsuccessReadCount++;
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                        slCardUIDs.remove(cardProtocol.getCard().getUID());
                        if (cardProtocol.getError() instanceof CardProtocol.TangemException_InvalidPIN) {
                            doEnterPIN();
                        } else if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {
                            if (!NoExtendedLengthSupportDialog.allreadyShowed) {
                                new NoExtendedLengthSupportDialog().show(getActivity().getFragmentManager(), "NoExtendedLengthSupportDialog");
                            }
                        } else {
                            lastTag = null;
                        }
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

        readCardInfoTask = null;

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


    public void OnReadWait(final int msec) {
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

    private class RequestWalletInfoTask extends ElectrumTask {
        public RequestWalletInfoTask(String host, int port) {
            super(host, port);
        }


        public RequestWalletInfoTask(String host, int port, SharedData sharedData) {
            super(host, port, sharedData);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
            requestTasks.remove(this);
            if ( mSwipeRefreshLayout!=null && requestTasks.size() == 0) mSwipeRefreshLayout.setRefreshing(false); //TODO: tmp

        }

        void FinishWithError(String wallet, String message) {
            mCardListAdapter.UpdateWalletError(wallet, "Cannot obtain data from blockchain");
        }

        @Override
        protected void onPostExecute(List<ElectrumRequest> requests) {
            super.onPostExecute(requests);
            requestTasks.remove(this);
            Log.i("RequestWalletInfoTask", "onPostExecute[" + String.valueOf(requests.size()) + "]");

            CoinEngine engine = CoinEngineFactory.Create(Blockchain.Bitcoin);

            for (ElectrumRequest request : requests) {
                try {

                    if (request.error == null) {
                        if (request.isMethod(ElectrumRequest.METHOD_GetBalance)) {
                            try {
                                Long conf = request.getResult().getLong("confirmed");
                                Long unconf = request.getResult().getLong("unconfirmed");
                                if (sharedCounter != null) {
                                    int counter = sharedCounter.requestCounter.incrementAndGet();
                                    if (counter != 1) {
                                        continue;
                                    }
                                }
                                String mWalletAddress = request.getParams().getString(0);
                                mCardListAdapter.UpdateWalletBalance(mWalletAddress, conf, unconf, getValidationNodeDescription());
                            } catch (JSONException e) {
                                if (sharedCounter != null) {
                                    int errCounter = sharedCounter.errorRequest.incrementAndGet();
                                    if (errCounter == sharedCounter.allRequest) {
                                        e.printStackTrace();
                                        FinishWithError(request.WalletAddress, e.toString());
                                        engine.SwitchNode(null);
                                    }
                                } else {
                                    e.printStackTrace();
                                    FinishWithError(request.WalletAddress, e.toString());
                                    engine.SwitchNode(null);
                                }
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_ListUnspent)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);
                                mCardListAdapter.UpdateWalletUnspent(mWalletAddress, request.getResultArray());

                                JSONArray unspentList = request.getResultArray();

                                for (int i = 0; i < unspentList.length(); i++) {
                                    JSONObject jsUnspent = unspentList.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {
                                        RequestWalletInfoTask task = new RequestWalletInfoTask(request.Host, request.Port);
                                        requestTasks.add(task);
                                        task.execute(ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                                ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                                engine.SwitchNode(null);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetHistory)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);
                                mCardListAdapter.UpdateWalletHistory(mWalletAddress, request.getResultArray());

                                JSONArray historyList = request.getResultArray();

                                for (int i = 0; i < historyList.length(); i++) {
                                    JSONObject jsUnspent = historyList.getJSONObject(i);
                                    Integer height = jsUnspent.getInt("height");
                                    String hash = jsUnspent.getString("tx_hash");
                                    if (height != -1) {
                                        RequestWalletInfoTask task = new RequestWalletInfoTask(request.Host, request.Port);
                                        requestTasks.add(task);

                                        task.execute(ElectrumRequest.GetHeader(mWalletAddress, String.valueOf(height)),
                                                ElectrumRequest.GetTransaction(mWalletAddress, hash));
                                    }
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                                engine.SwitchNode(null);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetHeader)) {
                            try {
                                String mWalletAddress = request.WalletAddress;

                                mCardListAdapter.UpdateWalletHeader(mWalletAddress, request.getResult());

                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                                engine.SwitchNode(null);
                            }
                        } else if (request.isMethod(ElectrumRequest.METHOD_GetTransaction)) {
                            try {
                                Log.e("MainActivityFragment_TX", request.TxHash);
                                String mWalletAddress = request.WalletAddress;
                                String tx = request.TxHash;
                                String raw = request.getResultString();
                                Log.e("MainActivityFragment_R", raw);
                                mCardListAdapter.UpdateTransaction(mWalletAddress, tx, raw);

                            } catch (JSONException e) {
                                e.printStackTrace();
                                mCardListAdapter.UpdateWalletError(request.WalletAddress, "Cannot obtain data from blockchain");
                                FinishWithError(request.WalletAddress, e.toString());
                                engine.SwitchNode(null);
                            }
                        }
                    } else {
                        if (sharedCounter != null) {
                            int errCounter = sharedCounter.errorRequest.incrementAndGet();
                            if (errCounter >= sharedCounter.allRequest) {
                                FinishWithError(request.WalletAddress, request.error);
                                engine.SwitchNode(null);
                            }
                        } else {
                            FinishWithError(request.WalletAddress, request.error);
                            engine.SwitchNode(null);
                        }

                    }
                } catch (JSONException e) {
                    if (sharedCounter != null) {
                        int errCounter = sharedCounter.errorRequest.incrementAndGet();
                        if (errCounter >= sharedCounter.allRequest) {
                            e.printStackTrace();
                            mCardListAdapter.UpdateWalletError(request.WalletAddress, "Cannot obtain data from blockchain");
                            engine.SwitchNode(null);
                        }
                    } else {
                        e.printStackTrace();
                        mCardListAdapter.UpdateWalletError(request.WalletAddress, "Cannot obtain data from blockchain");
                        engine.SwitchNode(null);
                    }

                }
            }

            if ( mSwipeRefreshLayout!=null && requestTasks.size() == 0) mSwipeRefreshLayout.setRefreshing(false); //TODO: tmp
        }

    }

    private class ETHRequestTask extends InfuraTask {
        ETHRequestTask(Blockchain blockchain) {
            super(blockchain);
        }

        void FinishWithError(String wallet, String message) {
            mCardListAdapter.UpdateWalletError(wallet, "Cannot obtain data from blockchain");
        }

        @Override
        protected void onPostExecute(List<Infura_Request> requests) {
            super.onPostExecute(requests);
            for (Infura_Request request : requests) {
                try {
                    if (request.error == null) {

                        if (request.isMethod(Infura_Request.METHOD_ETH_GetBalance)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);

                                String balanceCap = request.getResultString();
                                balanceCap = balanceCap.substring(2);
                                BigInteger l = new BigInteger(balanceCap, 16);
                                BigInteger d = l.divide(new BigInteger("1000000000000000000", 10));
                                Long balance = d.longValue();
                                if (request.getBlockchain() != Blockchain.Token)
                                    mCardListAdapter.UpdateWalletBalance(mWalletAddress, balance, l.toString(10), getValidationNodeDescription());
                                mCardListAdapter.UpdateWalletBalanceOnlyAlter(mWalletAddress, l.toString(10));
                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                            }
                        } else if (request.isMethod(Infura_Request.METHOD_ETH_Call)) {
                            try {
                                String mWalletAddress = request.WalletAddress;

                                String balanceCap = request.getResultString();
                                balanceCap = balanceCap.substring(2);
                                BigInteger l = new BigInteger(balanceCap, 16);
                                Long balance = l.longValue();
                                if (l.compareTo(BigInteger.ZERO) == 0) {
                                    mCardListAdapter.UpdateWalletBlockchain(mWalletAddress, Blockchain.Ethereum);
                                    mCardListAdapter.AddWalletBlockchainNameToken(mWalletAddress);
                                    TangemCard card = mCardListAdapter.getCardByWallet(request.WalletAddress);
                                    if (card != null) {
                                        refreshCard(card);
                                    }
                                    return;
                                }
                                mCardListAdapter.UpdateWalletBalance(mWalletAddress, balance, l.toString(10), getValidationNodeDescription());
                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                            } catch (Exception e) {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                            }
                        } else if (request.isMethod(Infura_Request.METHOD_ETH_GetOutTransactionCount)) {
                            try {
                                String mWalletAddress = request.getParams().getString(0);

                                String nonce = request.getResultString();
                                nonce = nonce.substring(2);
                                BigInteger count = new BigInteger(nonce, 16);

                                mCardListAdapter.UpdateWalletCoutConfirmTx(mWalletAddress, count);
                            } catch (JSONException e) {
                                e.printStackTrace();
                                FinishWithError(request.WalletAddress, e.toString());
                            }
                        }
                    } else {
                        FinishWithError(request.WalletAddress, request.error);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    FinishWithError(request.WalletAddress, e.toString());
                }
            }
        }

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
                                mCardListAdapter.UpdateRate(request.WalletAddress, rate);
                                stop = true;
                            }

                            if (currency.equals(request.currencyAlter)) {
                                String usd = obj.getString("price_usd");

                                Float rate = Float.valueOf(usd);
                                mCardListAdapter.UpdateRateAlter(request.WalletAddress, rate);
                                stopAlter = true;
                            }

                            if (stop && stopAlter) {
                                break;
                            }
                        }


                        //mCardListAdapter.UpdateWalletBalance(mWalletAddress, balance, l.toString(10));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }


}

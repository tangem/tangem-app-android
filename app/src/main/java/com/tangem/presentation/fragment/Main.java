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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.tangem.data.network.request.ElectrumRequest;
import com.tangem.data.network.request.ExchangeRequest;
import com.tangem.data.network.request.InfuraRequest;
import com.tangem.data.network.task.main.ETHRequestTask;
import com.tangem.data.network.task.main.RateInfoTask;
import com.tangem.data.network.task.main.RequestWalletInfoTask;
import com.tangem.data.nfc.ReadCardInfoTask;
import com.tangem.domain.cardReader.CardProtocol;
import com.tangem.domain.cardReader.NfcManager;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.CoinEngineFactory;
import com.tangem.domain.wallet.SharedData;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.activity.EmptyWalletActivity;
import com.tangem.presentation.activity.LoadedWalletActivity;
import com.tangem.presentation.activity.MainActivity;
import com.tangem.presentation.activity.RequestPINActivity;
import com.tangem.presentation.adapter.CardListAdapter;
import com.tangem.presentation.dialog.NoExtendedLengthSupportDialog;
import com.tangem.presentation.dialog.WaitSecurityDelayDialog;
import com.tangem.util.Util;
import com.tangem.wallet.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A placeholder fragment containing a simple view.
 */
public class Main extends Fragment implements NfcAdapter.ReaderCallback, CardListAdapter.UiCallbacks, CardProtocol.Notifications, MainActivity.OnCardsClean {
    public static final String TAG = Main.class.getSimpleName();

    public static final String EXTRA_LAST_DISCOVERED_TAG = "extra_last_tag";

    private static final int REQUEST_CODE_SHOW_CARD_ACTIVITY = 1;
    private static final int REQUEST_CODE_ENTER_PIN_ACTIVITY = 2;
    private static final int REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS = 3;

    private NfcManager mNfcManager;
    private ArrayList<String> slCardUIDs = new ArrayList<>();
    private RelativeLayout rlProgressBar;
    private ProgressBar progressBar;
    public CardListAdapter mCardListAdapter;
    private ReadCardInfoTask readCardInfoTask;
    public List<RequestWalletInfoTask> requestTasks = new ArrayList<>();

    private int unsuccessReadCount = 0;
    private Tag lastTag = null;
//    private String lastRead_UID = "";

    public Main() {
    }

    public CardListAdapter getCardListAdapter() {
        return mCardListAdapter;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fr_main, container, false);

        mNfcManager = new NfcManager(getActivity(), this);

        verifyPermissions();

        rlProgressBar = v.findViewById(R.id.rlProgressBar);
        progressBar = v.findViewById(R.id.progressBar);
        progressBar.setProgressTintList(ColorStateList.valueOf(Color.DKGRAY));
        RecyclerView rvCards = v.findViewById(R.id.lvCards);
        rvCards.setVisibility(View.GONE);

        rvCards.setLayoutManager(new LinearLayoutManager(getContext()));
        mCardListAdapter = new CardListAdapter(inflater, savedInstanceState, this);
        rvCards.setAdapter(mCardListAdapter);

        if (savedInstanceState != null && savedInstanceState.containsKey("slCardUIDs")) {
            slCardUIDs = savedInstanceState.getStringArrayList("slCardUIDs");
        }

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
                // remove swiped item from list and notify the RecyclerView
                int cardIndex = viewHolder.getAdapterPosition();
                if (cardIndex < 0 || cardIndex >= mCardListAdapter.getItemCount()) return;
                slCardUIDs.remove(mCardListAdapter.getCard(cardIndex).getUID());
//                if (mCardListAdapter.getCard(cardIndex).getUID() == lastRead_UID) {
//                    lastRead_UID = "";
//                }
                mCardListAdapter.removeCard(cardIndex);
                if (mCardListAdapter.getItemCount() == 0 && getActivity().getClass() == MainActivity.class) {
                    ((MainActivity) getActivity()).hideCleanButton();
                }
            }
        });

        itemTouchHelper.attachToRecyclerView(rvCards);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setOnCardsClean(this);
            ((MainActivity) getActivity()).setNfcAdapterReaderCallback(this);
        }

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
        ReadCardInfoTask.resetLastReadInfo();
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

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d(TAG, "ActivityResult: requestCode = " + requestCode + ", resultCode = " + resultCode);

        if (resultCode == Activity.RESULT_OK && requestCode == REQUEST_CODE_SHOW_CARD_ACTIVITY) {
            if (data != null && Objects.requireNonNull(data.getExtras()).containsKey("UID")) {
                final TangemCard card = new TangemCard(data.getStringExtra("UID"));
                card.LoadFromBundle(data.getBundleExtra("Card"));

                switch (data.getStringExtra("modification")) {
                    case "delete":
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
                        break;

                    case "update":
                        mCardListAdapter.updateCard(card);
                        break;

                    case "updateAndViewCard":
                        mCardListAdapter.updateCard(card);
                        onViewCard(data.getExtras());
                        break;
                }

            }
        } else if (requestCode == REQUEST_CODE_ENTER_PIN_ACTIVITY) {
            if( resultCode == Activity.RESULT_OK && lastTag != null)
            {
                onTagDiscovered(lastTag);
            }else{
                ReadCardInfoTask.resetLastReadInfo();
            }
        }
    }

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
                Log.d(TAG, "Repeat UID: " + sUID);
                mNfcManager.ignoreTag(isoDep.getTag());
                return;
            } else {
                Log.v(TAG, "UID: " + sUID);
            }

            Log.e(TAG, "setTimeout(" + String.valueOf(1000 + 3000 * unsuccessReadCount) + ")");
            if (unsuccessReadCount < 2) {
                isoDep.setTimeout(2000 + 5000 * unsuccessReadCount);
            } else {
                isoDep.setTimeout(90000);
            }
            lastTag = tag;

            readCardInfoTask = new ReadCardInfoTask(getActivity(), mNfcManager, isoDep, this);
            readCardInfoTask.start();

            Log.i(TAG, "onTagDiscovered " + Arrays.toString(tag.getId()));

        } catch (Exception e) {
            e.printStackTrace();
            mNfcManager.notifyReadResult(false);
        }

    }

    @Override
    public void onViewCard(Bundle cardInfo) {
        String UID = cardInfo.getString("UID");
        TangemCard card = new TangemCard(UID);
        card.LoadFromBundle(cardInfo.getBundle("Card"));

        Intent intent;
        if (card.getStatus() == TangemCard.Status.Empty) {
            intent = new Intent(getActivity(), EmptyWalletActivity.class);
            Log.i(TAG, "onViewCard " + "Empty");

        } else if (card.getStatus() == TangemCard.Status.Loaded) {
            intent = new Intent(getActivity(), LoadedWalletActivity.class);
            Log.i(TAG, "onViewCard " + "Loaded");

        } else if (card.getStatus() == TangemCard.Status.NotPersonalized || card.getStatus() == TangemCard.Status.Purged) {
            Log.i(TAG, "onViewCard " + "NotPersonalized");
            return;
        } else {
            intent = new Intent(getActivity(), LoadedWalletActivity.class);
            Log.i(TAG, "onViewCard " + "else");
        }

        intent.putExtras(cardInfo);
        startActivityForResult(intent, REQUEST_CODE_SHOW_CARD_ACTIVITY);
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
    public void OnReadProgress(CardProtocol protocol, final int progress) {
        progressBar.post(() -> progressBar.setProgress(progress));
    }

    @Override
    public void OnReadFinish(final CardProtocol cardProtocol) {
        readCardInfoTask = null;
        if (cardProtocol != null) {
            if (cardProtocol.getError() == null) {
                mNfcManager.notifyReadResult(true);
                progressBar.post(() -> {
                    rlProgressBar.setVisibility(View.GONE);
                    progressBar.setProgress(100);
                    progressBar.setProgressTintList(ColorStateList.valueOf(Color.GREEN));

//                    addCard(cardProtocol.getCard());

                    Bundle cardInfo = new Bundle();
                    cardInfo.putString("UID", cardProtocol.getCard().getUID());
                    Bundle bCard = new Bundle();
                    cardProtocol.getCard().SaveToBundle(bCard);
                    cardInfo.putBundle("Card", bCard);


                    String UID = cardInfo.getString("UID");
                    TangemCard card = new TangemCard(UID);
                    card.LoadFromBundle(cardInfo.getBundle("Card"));

                    Intent intent;
                    if (card.getStatus() == TangemCard.Status.Empty) {
                        intent = new Intent(getActivity(), EmptyWalletActivity.class);
                        Log.i(TAG, "onViewCard " + "Empty");

                    } else if (card.getStatus() == TangemCard.Status.Loaded) {
                        intent = new Intent(getActivity(), LoadedWalletActivity.class);
                        Log.i(TAG, "onViewCard " + "Loaded");

                    } else if (card.getStatus() == TangemCard.Status.NotPersonalized || card.getStatus() == TangemCard.Status.Purged) {
                        Log.i(TAG, "onViewCard " + "NotPersonalized");
                        return;
                    } else {
                        intent = new Intent(getActivity(), LoadedWalletActivity.class);
                        Log.i(TAG, "onViewCard " + "else");
                    }

                    intent.putExtra(EXTRA_LAST_DISCOVERED_TAG, lastTag);
                    intent.putExtras(cardInfo);
                    startActivityForResult(intent, REQUEST_CODE_SHOW_CARD_ACTIVITY);

                    mCardListAdapter.clearCards();
                    slCardUIDs.clear();
                    for (RequestWalletInfoTask rt : requestTasks) {
                        rt.cancel(true);
                    }


                });
            } else {
                // remove last UIDs because of error and no card read
                progressBar.post(() -> {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), R.string.try_to_scan_again, Toast.LENGTH_SHORT).show();
                        unsuccessReadCount++;
                        progressBar.setProgress(100);
                        progressBar.setProgressTintList(ColorStateList.valueOf(Color.RED));
                        slCardUIDs.remove(cardProtocol.getCard().getUID());
                        if (cardProtocol.getError() instanceof CardProtocol.TangemException_InvalidPIN) {
                            doEnterPIN();
                        } else {
                            if (cardProtocol.getError() instanceof CardProtocol.TangemException_ExtendedLengthNotSupported) {
                                if (!NoExtendedLengthSupportDialog.allreadyShowed) {
                                    new NoExtendedLengthSupportDialog().show(Objects.requireNonNull(getActivity()).getFragmentManager(), NoExtendedLengthSupportDialog.TAG);
                                }
                            }
                            lastTag = null;
                            ReadCardInfoTask.resetLastReadInfo();
                            mNfcManager.notifyReadResult(false);
                        }
                    }
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


    public void OnReadCancel() {
        readCardInfoTask = null;
        ReadCardInfoTask.resetLastReadInfo();
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


    public void OnReadWait(final int msec) {
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

    @Override
    public void doClean() {
        mCardListAdapter.clearCards();
        slCardUIDs.clear();
        for (RequestWalletInfoTask rt : requestTasks) {
            rt.cancel(true);
        }
        ReadCardInfoTask.resetLastReadInfo();
    }

    public void refreshCard(TangemCard card) {
        CoinEngine engine = CoinEngineFactory.Create(card.getBlockchain());
        if (card.getBlockchain() == Blockchain.Ethereum || card.getBlockchain() == Blockchain.EthereumTestNet) {
            ETHRequestTask task = new ETHRequestTask(Main.this, card.getBlockchain());
            InfuraRequest req = InfuraRequest.GetBalance(card.getWallet());
            req.setID(67);
            req.setBlockchain(card.getBlockchain());
            InfuraRequest reqNonce = InfuraRequest.GetOutTransactionCount(card.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(card.getBlockchain());

            task.execute(req, reqNonce);

            RateInfoTask taskRate = new RateInfoTask(Main.this);
            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "ethereum", "ethereum");
            taskRate.execute(rate);

        } else if (card.getBlockchain() == Blockchain.BitcoinTestNet || card.getBlockchain() == Blockchain.Bitcoin) {
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            for (int i = 0; i < data.allRequest; ++i) {

                String nodeAddress = engine.GetNextNode(card);
                int nodePort = engine.GetNextNodePort(card);
                RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));

                RequestWalletInfoTask task = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort);
                requestTasks.add(task);
                task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(card.getWallet()));//, ElectrumRequest.ListHistory(card.getWallet()));
            }

        } else if (card.getBlockchain() == Blockchain.BitcoinCashTestNet || card.getBlockchain() == Blockchain.BitcoinCash) {
            SharedData data = new SharedData(SharedData.COUNT_REQUEST);
            for (int i = 0; i < data.allRequest; ++i) {

                String nodeAddress = engine.GetNextNode(card);
                int nodePort = engine.GetNextNodePort(card);
                RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort, data);
                connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));
            }

            String nodeAddress = engine.GetNode(card);
            int nodePort = engine.GetNodePort(card);
            RequestWalletInfoTask task = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort);

            requestTasks.add(task);
            task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(card.getWallet()));//, ElectrumRequest.ListHistory(card.getWallet()));
            RateInfoTask taskRate = new RateInfoTask(Main.this);
            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "bitcoin-cash", "bitcoin-cash");
            taskRate.execute(rate);

        } else if (card.getBlockchain() == Blockchain.Token) {
            ETHRequestTask updateETH = new ETHRequestTask(Main.this, card.getBlockchain());
            InfuraRequest reqETH = InfuraRequest.GetTokenBalance(card.getWallet(), engine.GetContractAddress(card), engine.GetTokenDecimals(card));
            reqETH.setID(67);
            reqETH.setBlockchain(card.getBlockchain());


            InfuraRequest reqBalance = InfuraRequest.GetBalance(card.getWallet());
            reqBalance.setID(67);
            reqBalance.setBlockchain(card.getBlockchain());


            RateInfoTask taskRate = new RateInfoTask(Main.this);
            ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "basic-attention-token", "ethereum");
            taskRate.execute(rate);

            InfuraRequest reqNonce = InfuraRequest.GetOutTransactionCount(card.getWallet());
            reqNonce.setID(67);
            reqNonce.setBlockchain(card.getBlockchain());

            updateETH.execute(reqETH, reqNonce, reqBalance);
        }
    }

    private void addCard(final TangemCard card) {
        if (mCardListAdapter != null) {
            Objects.requireNonNull(getActivity()).runOnUiThread(() -> {
                unsuccessReadCount = 0;
                slCardUIDs.add(0, card.getUID());
                mCardListAdapter.addCard(card);

                CoinEngine engine = CoinEngineFactory.Create(card.getBlockchain());

                if (card.getStatus() == TangemCard.Status.Loaded) {

                    if (card.getBlockchain() == Blockchain.Ethereum || card.getBlockchain() == Blockchain.EthereumTestNet) {
                        ETHRequestTask task = new ETHRequestTask(Main.this, card.getBlockchain());
                        InfuraRequest req = InfuraRequest.GetBalance(card.getWallet());
                        req.setID(67);
                        req.setBlockchain(card.getBlockchain());
                        InfuraRequest reqNonce = InfuraRequest.GetOutTransactionCount(card.getWallet());
                        reqNonce.setID(67);
                        reqNonce.setBlockchain(card.getBlockchain());

                        task.execute(req, reqNonce);

                        RateInfoTask taskRate = new RateInfoTask(Main.this);
                        ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "ethereum", "ethereum");
                        taskRate.execute(rate);

                    } else if (card.getBlockchain() == Blockchain.BitcoinTestNet || card.getBlockchain() == Blockchain.Bitcoin) {
                        SharedData data = new SharedData(SharedData.COUNT_REQUEST);

                        for (int i = 0; i < data.allRequest; ++i) {
                            String nodeAddress = engine.GetNextNode(card);
                            int nodePort = engine.GetNextNodePort(card);

                            RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort, data);
                            connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));
                        }

                        String nodeAddress = engine.GetNode(card);
                        int nodePort = engine.GetNodePort(card);

                        RequestWalletInfoTask task = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort);

                        requestTasks.add(task);
//                        task.execute(ElectrumRequest.ListUnspent(card.getWallet()));//, ElectrumRequest.ListHistory(card.getWallet()));
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(card.getWallet()));//, ElectrumRequest.ListHistory(card.getWallet()));
                        RateInfoTask taskRate = new RateInfoTask(Main.this);
                        ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "bitcoin", "bitcoin");
                        taskRate.execute(rate);

                    } else if (card.getBlockchain() == Blockchain.BitcoinCashTestNet || card.getBlockchain() == Blockchain.BitcoinCash) {
                        SharedData data = new SharedData(SharedData.COUNT_REQUEST);

                        for (int i = 0; i < data.allRequest; ++i) {
                            String nodeAddress = engine.GetNextNode(card);
                            int nodePort = engine.GetNextNodePort(card);

                            RequestWalletInfoTask connectTaskEx = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort, data);
                            connectTaskEx.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.CheckBalance(card.getWallet()));
                        }

                        String nodeAddress = engine.GetNode(card);
                        int nodePort = engine.GetNodePort(card);

                        RequestWalletInfoTask task = new RequestWalletInfoTask(Main.this, nodeAddress, nodePort);

                        requestTasks.add(task);
                        task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, ElectrumRequest.ListUnspent(card.getWallet()));//, ElectrumRequest.ListHistory(card.getWallet()));
                        RateInfoTask taskRate = new RateInfoTask(Main.this);
                        ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "bitcoin-cash", "bitcoin-cash");
                        taskRate.execute(rate);

                    } else if (card.getBlockchain() == Blockchain.Token) {
                        ETHRequestTask updateETH = new ETHRequestTask(Main.this, card.getBlockchain());
                        InfuraRequest reqETH = InfuraRequest.GetTokenBalance(card.getWallet(), engine.GetContractAddress(card), engine.GetTokenDecimals(card));
                        reqETH.setID(67);
                        reqETH.setBlockchain(card.getBlockchain());


                        InfuraRequest reqBalance = InfuraRequest.GetBalance(card.getWallet());
                        reqBalance.setID(67);
                        reqBalance.setBlockchain(card.getBlockchain());


                        RateInfoTask taskRate = new RateInfoTask(Main.this);
                        ExchangeRequest rate = ExchangeRequest.GetRate(card.getWallet(), "basic-attention-token", "ethereum");
                        taskRate.execute(rate);

                        InfuraRequest reqNonce = InfuraRequest.GetOutTransactionCount(card.getWallet());
                        reqNonce.setID(67);
                        reqNonce.setBlockchain(card.getBlockchain());

                        updateETH.execute(reqETH, reqNonce, reqBalance);
                    }
                }
                if (getActivity().getClass() == MainActivity.class) {
                    ((MainActivity) getActivity()).showCleanButton();
                }
            });
        }
    }

    private void doEnterPIN() {
        Intent intent = new Intent(getContext(), RequestPINActivity.class);
        intent.putExtra("mode", RequestPINActivity.Mode.RequestPIN.toString());
        startActivityForResult(intent, REQUEST_CODE_ENTER_PIN_ACTIVITY);
    }

    private void verifyPermissions() {
        NfcManager.verifyPermissions(getActivity());
        if (ActivityCompat.checkSelfPermission(Objects.requireNonNull(getActivity()), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
//            Log.e("QRScanActivity", "User hasn't granted permission to use camera");
            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.CAMERA}, REQUEST_CODE_REQUEST_CAMERA_PERMISSIONS);
        }
    }

}
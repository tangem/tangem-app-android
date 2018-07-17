package com.tangem.data.network.task.main;

import com.tangem.data.network.request.InfuraRequest;
import com.tangem.data.network.task.InfuraTask;
import com.tangem.domain.wallet.Blockchain;
import com.tangem.domain.wallet.TangemCard;
import com.tangem.presentation.fragment.Main;

import org.json.JSONException;

import java.lang.ref.WeakReference;
import java.math.BigInteger;
import java.util.List;

public class ETHRequestTask extends InfuraTask {
    private WeakReference<Main> reference;

    public ETHRequestTask(Main context, Blockchain blockchain) {
        super(blockchain);
        reference = new WeakReference<>(context);
    }

    private void FinishWithError(String wallet, String message) {
        Main main = reference.get();
        main.mCardListAdapter.UpdateWalletError(wallet, "Cannot obtain data from blockchain");
    }

    @Override
    protected void onPostExecute(List<InfuraRequest> requests) {
        super.onPostExecute(requests);
        Main main = reference.get();

        for (InfuraRequest request : requests) {
            try {
                if (request.error == null) {

                    if (request.isMethod(InfuraRequest.METHOD_ETH_GetBalance)) {
                        try {
                            String mWalletAddress = request.getParams().getString(0);

                            String balanceCap = request.getResultString();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            BigInteger d = l.divide(new BigInteger("1000000000000000000", 10));
                            Long balance = d.longValue();
                            if (request.getBlockchain() != Blockchain.Token)
                                main.mCardListAdapter.UpdateWalletBalance(mWalletAddress, balance, l.toString(10), getValidationNodeDescription());
                            main.mCardListAdapter.UpdateWalletBalanceOnlyAlter(mWalletAddress, l.toString(10));
                        } catch (JSONException e) {
                            e.printStackTrace();
                            FinishWithError(request.WalletAddress, e.toString());
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_Call)) {
                        try {
                            String mWalletAddress = request.WalletAddress;

                            String balanceCap = request.getResultString();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            Long balance = l.longValue();
                            if (l.compareTo(BigInteger.ZERO) == 0) {
                                main.mCardListAdapter.UpdateWalletBlockchain(mWalletAddress, Blockchain.Ethereum);
                                main.mCardListAdapter.AddWalletBlockchainNameToken(mWalletAddress);
                                TangemCard card = main.mCardListAdapter.getCardByWallet(request.WalletAddress);
//                                if (card != null) {
//                                    main.refreshCard(card);
//                                }
                                return;
                            }
                            main.mCardListAdapter.UpdateWalletBalance(mWalletAddress, balance, l.toString(10), getValidationNodeDescription());
                        } catch (JSONException e) {
                            e.printStackTrace();
                            FinishWithError(request.WalletAddress, e.toString());
                        } catch (Exception e) {
                            e.printStackTrace();
                            FinishWithError(request.WalletAddress, e.toString());
                        }
                    } else if (request.isMethod(InfuraRequest.METHOD_ETH_GetOutTransactionCount)) {
                        try {
                            String mWalletAddress = request.getParams().getString(0);

                            String nonce = request.getResultString();
                            nonce = nonce.substring(2);
                            BigInteger count = new BigInteger(nonce, 16);

                            main.mCardListAdapter.UpdateWalletCoutConfirmTx(mWalletAddress, count);
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
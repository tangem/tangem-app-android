package com.tangem.domain.wallet.rsk;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.google.common.base.Strings;
import com.tangem.data.Blockchain;
import com.tangem.data.network.ServerApiRootstock;
import com.tangem.data.network.model.InfuraResponse;
import com.tangem.domain.wallet.BTCUtils;
import com.tangem.domain.wallet.CoinEngine;
import com.tangem.domain.wallet.ECDSASignatureETH;
import com.tangem.domain.wallet.EthTransaction;
import com.tangem.domain.wallet.TangemContext;
import com.tangem.domain.wallet.eth.EthData;
import com.tangem.domain.wallet.token.TokenData;
import com.tangem.domain.wallet.token.TokenEngine;
import com.tangem.tangemcard.data.TangemCard;
import com.tangem.tangemcard.tasks.SignTask;
import com.tangem.util.CryptoUtil;
import com.tangem.wallet.R;

import org.bitcoinj.core.ECKey;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Arrays;

public class RskTokenEngine extends TokenEngine {

    private static final String TAG = RskTokenEngine.class.getSimpleName();

    public RskTokenEngine(TangemContext ctx) throws Exception {
        super(ctx);
    }

    public RskTokenEngine() {
        super();
    }

    @Override
    public String getBalanceCurrency() {
        String currency = ctx.getCard().getTokenSymbol();
        if (Strings.isNullOrEmpty(currency))
            return "NoN";
        if (hasBalanceInfo()) {
            if (coinData.getBalanceInInternalUnits().notZero()) {
                return currency;
            } else {
                return Blockchain.Rootstock.getCurrency();
            }
        } else {
            return currency;
        }
    }

    @Override
    public String getFeeCurrency() {
        return Blockchain.Rootstock.getCurrency();
    }

    @Override
    public Amount convertToAmount(InternalAmount internalAmount) throws Exception {
        if (internalAmount.getCurrency().equals("wei")) {
            BigDecimal d = internalAmount.divide(new BigDecimal("1000000000000000000"), getEthDecimals(), RoundingMode.DOWN);
            return new Amount(d, Blockchain.Rootstock.getCurrency());
        } else if (internalAmount.getCurrency().equals(ctx.getCard().getTokenSymbol())) {
            BigDecimal p = new BigDecimal(10);
            p = p.pow(getTokenDecimals());
            BigDecimal d = internalAmount.divide(p);
            return new Amount(d, ctx.getCard().getTokenSymbol());
        }
        throw new Exception(String.format("Can't convert '%s' to '%s'", internalAmount.getCurrency(), ctx.getCard().getTokenSymbol()));
    }

    @Override
    public InternalAmount convertToInternalAmount(Amount amount) throws Exception {
        if (amount.getCurrency().equals(Blockchain.Rootstock.getCurrency())) {
            BigDecimal d = amount.multiply(new BigDecimal("1000000000000000000"));
            return new InternalAmount(d, "wei");
        } else if (amount.getCurrency().equals(ctx.getCard().getTokenSymbol())) {
            BigDecimal p = new BigDecimal(10);
            p = p.pow(getTokenDecimals());
            BigDecimal d = amount.multiply(p);
            return new InternalAmount(d, ctx.getCard().getTokenSymbol());
        }
        throw new Exception(String.format("Can't convert '%s' to '%s'", amount.getCurrency(), ctx.getCard().getTokenSymbol()));
    }

    @Override
    public Uri getShareWalletUriExplorer() { return Uri.parse("https://explorer.rsk.co/address/" + ctx.getCoinData().getWallet()); } // Only RSK explorer for now

    @Override
    public Uri getShareWalletUri() { return Uri.parse(ctx.getCoinData().getWallet()); }

    @Override
    public boolean isExtractPossible() {
        if (!hasBalanceInfo()) {
            ctx.setMessage(R.string.cannot_obtain_data_from_blockchain);
        } else if (!isBalanceNotZero()) {
            ctx.setMessage(R.string.wallet_empty);
        } else if (awaitingConfirmation()) {
            ctx.setMessage(R.string.please_wait_while_previous);
        } else if (!isBalanceAlterNotZero()) {
            ctx.setMessage(ctx.getString(R.string.not_enough_rbtc_for_fee));
        } else {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkNewTransactionAmount(Amount amount) {
        if (!hasBalanceInfo()) return false;
        Amount balance;
        try {
            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                balance = convertToAmount(coinData.getBalanceInInternalUnits());
            } else if (amount.getCurrency().equals(Blockchain.Rootstock.getCurrency()) && coinData.getBalanceInInternalUnits().isZero()) {
                balance = convertToAmount(coinData.getBalanceAlterInInternalUnits());
            } else {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return amount.compareTo(balance) <= 0;
    }

    @Override
    public boolean checkNewTransactionAmountAndFee(Amount amount, Amount fee, Boolean isFeeIncluded) {
        if (!hasBalanceInfo()) return false;

        try {
            Amount balanceRBTC = convertToAmount(coinData.getBalanceAlterInInternalUnits());

            if (fee == null || amount == null || fee.isZero() || amount.isZero())
                return false;


            if (amount.getCurrency().equals(ctx.getCard().tokenSymbol)) {
                // token transaction
                if (fee.compareTo(balanceRBTC) > 0)
                    return false;
            } else if (amount.getCurrency().equals(Blockchain.Rootstock.getCurrency()) && coinData.getBalanceInInternalUnits().isZero()) {
                // standard RBTC transaction
//                try {
                if (isFeeIncluded && (amount.compareTo(balanceRBTC) > 0 || fee.compareTo(balanceRBTC) > 0))
                    return false;

                if (!isFeeIncluded && amount.add(fee).compareTo(balanceRBTC) > 0)
                    return false;

//                } catch (NumberFormatException e) {
//                    e.printStackTrace();
//                }
            } else

            {
                return false;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    @Override
    public SignTask.PaymentToSign constructPayment(Amount amountValue, Amount feeValue, boolean IncFee, String targetAddress) throws Exception {
        if (amountValue.getCurrency().equals(Blockchain.Rootstock.getCurrency())) {
            return constructPaymentRBTC(feeValue, amountValue, IncFee, targetAddress);
        } else {
            return constructPaymentToken(feeValue, amountValue, IncFee, targetAddress);
        }
    }

    private SignTask.PaymentToSign constructPaymentRBTC(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress) throws Exception {
        Log.e(TAG, "Construct RBTC payment "+amountValue.toString()+" with fee "+feeValue.toString()+(IncFee?" including":" excluding"));

        BigInteger nonceValue = coinData.getConfirmedTXCount();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();

        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();
        BigInteger weiAmount = convertToInternalAmount(amountValue).toBigIntegerExact();

        if (IncFee) {
            weiAmount = weiAmount.subtract(weiFee);
        }

        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(21000));
        BigInteger gasLimit = BigInteger.valueOf(21000);
        Integer chainId = EthTransaction.ChainEnum.Rootstock_mainnet.getValue();

        String to = targetAddress;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        final EthTransaction tx = EthTransaction.create(to, weiAmount, nonceValue, gasPrice, gasLimit, chainId);

        return new SignTask.PaymentToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] hashesForSign = new byte[1][];
                hashesForSign[0] = tx.getRawHash();
                return hashesForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for RSK");
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for RSK");
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                byte[] for_hash = tx.getRawHash();
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
                s = CryptoUtil.toCanonicalised(s);

                boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

                if (!f) {
                    Log.e("RSK-CHECK", "sign Failed.");
                }

                tx.signature = new ECDSASignatureETH(r, s);
                int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
                if (v != 27 && v != 28) {
                    Log.e(TAG, "invalid v");
                    throw new Exception("Error in RskTokenEngine - invalid v");
                }
                tx.signature.v = (byte) v;
                Log.e(TAG,"RSK_v "+ String.valueOf(v));

                byte[] txForSend = tx.getEncoded();
                notifyOnNeedSendPayment(txForSend);
                return txForSend;
            }
        };
    }

    private SignTask.PaymentToSign constructPaymentToken(Amount feeValue, Amount amountValue, boolean IncFee, String targetAddress) throws Exception {
        Log.e(TAG, "Construct TOKEN payment "+amountValue.toString()+" with fee "+feeValue.toString()+(IncFee?" including":" excluding"));

        BigInteger nonceValue = coinData.getConfirmedTXCount();
        byte[] pbKey = ctx.getCard().getWalletPublicKey();
//        boolean flag = (ctx.getCard().getSigningMethod() == TangemCard.SigningMethod.Sign_Hash_Validated_By_Issuer);
//        Issuer issuer = ctx.getCard().getIssuer();


//        BigInteger gigaK = BigInteger.valueOf(1000000000L);

        BigInteger weiFee = convertToInternalAmount(feeValue).toBigIntegerExact();

        InternalAmount amountDec = convertToInternalAmount(amountValue);
        BigInteger amount = amountDec.toBigInteger(); //new BigInteger(amountValue, 10);


        //amount = amount.subtract(fee);

        BigInteger gasPrice = weiFee.divide(BigInteger.valueOf(60000));
        BigInteger gasLimit = BigInteger.valueOf(60000);
        Integer chainId = EthTransaction.ChainEnum.Rootstock_mainnet.getValue();
        BigInteger amountZero = BigInteger.ZERO;

        String to = targetAddress;

        if (to.startsWith("0x") || to.startsWith("0X")) {
            to = to.substring(2);
        }

        String contractAddress = getContractAddress(ctx.getCard());

        if (contractAddress.startsWith("0x") || contractAddress.startsWith("0X")) {
            contractAddress = contractAddress.substring(2);
        }

        String amountLeadZero = amount.toString(16);
        if (amountLeadZero.startsWith("0x") || amountLeadZero.startsWith("0X")) {
            amountLeadZero = amountLeadZero.substring(2);
        }

        while (amountLeadZero.length() < 64) {
            amountLeadZero = "0" + amountLeadZero;
        }

        String cmd = "a9059cbb000000000000000000000000" + to + amountLeadZero; //TODO only for BAT


        byte[] data = BTCUtils.fromHex(cmd);
        EthTransaction tx = EthTransaction.create(contractAddress, amountZero, nonceValue, gasPrice, gasLimit, chainId, data);

        return new SignTask.PaymentToSign() {
            @Override
            public boolean isSigningMethodSupported(TangemCard.SigningMethod signingMethod) {
                return signingMethod == TangemCard.SigningMethod.Sign_Hash;
            }

            @Override
            public byte[][] getHashesToSign() {
                byte[][] hashesForSign = new byte[1][];
                hashesForSign[0] = tx.getRawHash();
                return hashesForSign;
            }

            @Override
            public byte[] getRawDataToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for RSK");
            }

            @Override
            public String getHashAlgToSign() throws Exception {
                throw new Exception("Signing of raw transaction not supported for RSK");
            }

            @Override
            public byte[] getIssuerTransactionSignature(byte[] dataToSignByIssuer) throws Exception {
                throw new Exception("Transaction validation by issuer not supported in this version");
            }

            @Override
            public byte[] onSignCompleted(byte[] signFromCard) throws Exception {
                byte[] for_hash = tx.getRawHash();
                BigInteger r = new BigInteger(1, Arrays.copyOfRange(signFromCard, 0, 32));
                BigInteger s = new BigInteger(1, Arrays.copyOfRange(signFromCard, 32, 64));
                s = CryptoUtil.toCanonicalised(s);

                boolean f = ECKey.verify(for_hash, new ECKey.ECDSASignature(r, s), pbKey);

                if (!f) {
                    Log.e("RSK-CHECK", "sign Failed.");
                }

                tx.signature = new ECDSASignatureETH(r, s);
                int v = tx.BruteRecoveryID2(tx.signature, for_hash, pbKey);
                if (v != 27 && v != 28) {
                    Log.e(TAG, "invalid v");
                    throw new Exception("Error in RskTokenEngine - invalid v");
                }
                tx.signature.v = (byte) v;
                Log.e(TAG,"RSK_v: "+ String.valueOf(v));

                byte[] txForSend = tx.getEncoded();
                notifyOnNeedSendPayment(txForSend);
                return txForSend;

            }
        };

    }

    @Override
    public void requestBalanceAndUnspentTransactions(BlockchainRequestsCallbacks blockchainRequestsCallbacks) {
        final ServerApiRootstock serverApiRootstock = new ServerApiRootstock();
        // request rootstock listener
        ServerApiRootstock.RootstockBodyListener rootstockBodyListener = new ServerApiRootstock.RootstockBodyListener() {
            @Override
            public void onSuccess(String method, InfuraResponse rootstockResponse) {
                switch (method) {
                    case ServerApiRootstock.ROOTSTOCK_ETH_GET_BALANCE: {
                        String balanceCap = rootstockResponse.getResult();
                        balanceCap = balanceCap.substring(2);
                        BigInteger l = new BigInteger(balanceCap, 16);
                        coinData.setBalanceReceived(true);
                        coinData.setBalanceAlterInInternalUnits(new CoinEngine.InternalAmount(l, "wei"));

//                        Log.i("$TAG eth_get_balance", balanceCap)
                    }
                    break;

                    case ServerApiRootstock.ROOTSTOCK_ETH_GET_TRANSACTION_COUNT: {
                        String nonce = rootstockResponse.getResult();
                        nonce = nonce.substring(2);
                        BigInteger count = new BigInteger(nonce, 16);
                        coinData.setConfirmedTXCount(count);


//                        Log.i("$TAG eth_getTransCount", nonce)
                    }
                    break;

                    case ServerApiRootstock.ROOTSTOCK_ETH_GET_PENDING_COUNT: {
                        String pending = rootstockResponse.getResult();
                        pending = pending.substring(2);
                        BigInteger count = new BigInteger(pending, 16);
                        coinData.setUnconfirmedTXCount(count);

//                        Log.i("$TAG eth_getPendingTxCount", pending)
                    }
                    break;
//
                    case ServerApiRootstock.ROOTSTOCK_ETH_CALL: {
                        try {
                            String balanceCap = rootstockResponse.getResult();
                            balanceCap = balanceCap.substring(2);
                            BigInteger l = new BigInteger(balanceCap, 16);
                            coinData.setBalanceInInternalUnits(new CoinEngine.InternalAmount(l, ctx.getCard().tokenSymbol));

//                            Log.i("$TAG eth_call", balanceCap)

                            if (blockchainRequestsCallbacks.allowAdvance()) {
                                serverApiRootstock.rootstock(ServerApiRootstock.ROOTSTOCK_ETH_GET_BALANCE, 67, coinData.getWallet(), "", "");
                                serverApiRootstock.rootstock(ServerApiRootstock.ROOTSTOCK_ETH_GET_TRANSACTION_COUNT, 67, coinData.getWallet(), "", "");
                                serverApiRootstock.rootstock(ServerApiRootstock.ROOTSTOCK_ETH_GET_PENDING_COUNT, 67, coinData.getWallet(), "", "");
                            } else {
                                ctx.setError("Terminated by user");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    break;

                }
                if (serverApiRootstock.isRequestsSequenceCompleted()) {
                    blockchainRequestsCallbacks.onComplete(!ctx.hasError());
                } else {
                    blockchainRequestsCallbacks.onProgress();
                }
            }

            @Override
            public void onFail(String method, String message) {
                if (!serverApiRootstock.isRequestsSequenceCompleted()) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };
        serverApiRootstock.setRootstockResponse(rootstockBodyListener);

        serverApiRootstock.rootstock(ServerApiRootstock.ROOTSTOCK_ETH_CALL, 67, coinData.getWallet(), getContractAddress(ctx.getCard()), "");
    }

    @Override
    public void requestFee(BlockchainRequestsCallbacks blockchainRequestsCallbacks, String targetAddress, Amount amount) {
        ServerApiRootstock serverApiRootstock = new ServerApiRootstock();
        // request rootstock gasPrice listener
        ServerApiRootstock.RootstockBodyListener rootstockBodyListener = new ServerApiRootstock.RootstockBodyListener() {
            @Override
            public void onSuccess(String method, InfuraResponse rootstockResponse) {
                String gasPrice = rootstockResponse.getResult();
                gasPrice = gasPrice.substring(2);

                // rounding gas price to integer gwei
                BigInteger l = new BigInteger(gasPrice, 16);

                Log.i(TAG, "Rootstock gas price: "+gasPrice+" ("+l.toString()+")");
                BigInteger m;
                if (!amount.getCurrency().equals(Blockchain.Rootstock.getCurrency())) m = BigInteger.valueOf(60000);
                else m = BigInteger.valueOf(21000);

                Log.i(TAG, "fee multiplier: "+m.toString());

                CoinEngine.InternalAmount weiMinFee = new CoinEngine.InternalAmount(l.multiply(m), "wei");
                CoinEngine.InternalAmount weiNormalFee = new CoinEngine.InternalAmount(l.multiply(BigInteger.valueOf(12)).divide(BigInteger.valueOf(10)).multiply(m), "wei");
                CoinEngine.InternalAmount weiMaxFee = new CoinEngine.InternalAmount(l.multiply(BigInteger.valueOf(15)).divide(BigInteger.valueOf(10)).multiply(m), "wei");
                Log.i(TAG, "min fee   : "+weiMinFee.toValueString()+" wei");
                Log.i(TAG, "normal fee: "+weiNormalFee.toValueString()+" wei");
                Log.i(TAG, "max fee   : "+weiMaxFee.toValueString()+" wei");

                try {
                    coinData.minFee = convertToAmount(weiMinFee);
                    coinData.normalFee = convertToAmount(weiNormalFee);
                    coinData.maxFee = convertToAmount(weiMaxFee);
                    Log.i(TAG, "min fee   : "+coinData.minFee.toString());
                    Log.i(TAG, "normal fee: "+coinData.normalFee.toString());
                    Log.i(TAG, "max fee   : "+coinData.maxFee.toString());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                blockchainRequestsCallbacks.onComplete(true);
            }

            @Override
            public void onFail(String method, String message) {
                ctx.setError(message);
                blockchainRequestsCallbacks.onComplete(false);
            }
        };
        serverApiRootstock.setRootstockResponse(rootstockBodyListener);

        serverApiRootstock.rootstock(ServerApiRootstock.ROOTSTOCK_ETH_GAS_PRICE, 67, coinData.getWallet(), "", "");
    }

    @Override
    public void requestSendTransaction(BlockchainRequestsCallbacks blockchainRequestsCallbacks, byte[] txForSend) {

        String txStr = String.format("0x%s", BTCUtils.toHex(txForSend));

        ServerApiRootstock serverApiRootstock = new ServerApiRootstock();
        // request rootstock listener
        ServerApiRootstock.RootstockBodyListener rootstockBodyListener = new ServerApiRootstock.RootstockBodyListener() {
            @Override
            public void onSuccess(String method, InfuraResponse infuraResponse) {
                if (method.equals(ServerApiRootstock.ROOTSTOCK_ETH_SEND_RAW_TRANSACTION)) {
                    if (infuraResponse.getResult().isEmpty()) {
                        ctx.setError("Rejected by node: " + infuraResponse.getError());
                        blockchainRequestsCallbacks.onComplete(false);
                    } else {
                        BigInteger nonce = coinData.getConfirmedTXCount();
                        nonce=nonce.add(BigInteger.valueOf(1));
                        coinData.setConfirmedTXCount(nonce);
                        ctx.setError(null);
                        blockchainRequestsCallbacks.onComplete(true);
                    }
                }
            }

            @Override
            public void onFail(String method, String message) {
                if (method.equals(ServerApiRootstock.ROOTSTOCK_ETH_SEND_RAW_TRANSACTION)) {
                    ctx.setError(message);
                    blockchainRequestsCallbacks.onComplete(false);
                }
            }
        };

        serverApiRootstock.setRootstockResponse(rootstockBodyListener);

        serverApiRootstock.rootstock(ServerApiRootstock.ROOTSTOCK_ETH_SEND_RAW_TRANSACTION, 67, coinData.getWallet(), "", txStr);

    }

}

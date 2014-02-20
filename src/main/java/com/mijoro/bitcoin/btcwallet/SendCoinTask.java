package com.mijoro.bitcoin.btcwallet;

import java.util.concurrent.ExecutionException;

import android.os.AsyncTask;

import com.google.bitcoin.core.Address;
import com.google.bitcoin.core.AddressFormatException;
import com.google.bitcoin.core.InsufficientMoneyException;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Utils;
import com.google.bitcoin.core.Wallet.SendResult;
import com.google.bitcoin.kits.WalletAppKit;

public class SendCoinTask extends AsyncTask<Void, Integer, Long> {
	private String mAddress;
	private String mAmount;
	private NetworkParameters mNetworkParams;
	private WalletAppKit mKit;

	public SendCoinTask(String address, String amount, NetworkParameters params, WalletAppKit kit) {
		mAddress = address;
		mAmount = amount;
		mKit = kit;
		mNetworkParams = params;
	}

	@Override
	protected Long doInBackground(Void... params) {
		Logger.log("Sending " + mAmount + " to " + mAddress + " Nano: "
				+ Utils.toNanoCoins(mAmount));
		try {
			Address target = new Address(mNetworkParams, mAddress);
			SendResult result = mKit.wallet().sendCoins(mKit.peerGroup(),
					target, Utils.toNanoCoins(mAmount));
			result.broadcastComplete.get();
		} catch (AddressFormatException e) {
			e.printStackTrace();
			Logger.log("Address Format Exception: " + mAddress);
		} catch (InsufficientMoneyException e) {
			e.printStackTrace();
			Logger.log("Insufficient Money");
		} catch (InterruptedException e) {
			e.printStackTrace();
			Logger.log("Interrupted Exception" + e);
		} catch (ExecutionException e) {
			e.printStackTrace();
			Logger.log("ExecutionException " + e);
		}
		return null;
	}

	protected void onPostExecute(Long result) {
		Logger.log("Executed Spend");
	}

}
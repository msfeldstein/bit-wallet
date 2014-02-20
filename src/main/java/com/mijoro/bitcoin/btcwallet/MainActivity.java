package com.mijoro.bitcoin.btcwallet;

import java.io.File;
import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.Executor;

import com.google.bitcoin.core.AbstractWalletEventListener;
import com.google.bitcoin.core.ECKey;
import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Transaction;
import com.google.bitcoin.core.Wallet;
import com.google.bitcoin.core.Wallet.BalanceType;
import com.google.bitcoin.kits.WalletAppKit;
import com.google.bitcoin.params.MainNetParams;
import com.google.bitcoin.params.TestNet3Params;
import com.google.bitcoin.utils.Threading;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.EditText;
import android.widget.TextView;

public class MainActivity extends Activity {
	private Handler mHandler;
	private TextView mBalanceField, mVerifiedBalanceField;
	private NetworkParameters mNetworkParams;
	private WalletAppKit mKit;
	private BTCWalletEventListener mWalletListener;

	private static boolean PRODUCTION = false;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		mBalanceField = (TextView) findViewById(R.id.balance_field);
		mVerifiedBalanceField = (TextView) findViewById(R.id.verified_balance_field);
		String filePrefix;
		if (PRODUCTION) {
			mNetworkParams = MainNetParams.get();
			filePrefix = "production";
		} else {
			mNetworkParams = TestNet3Params.get();
			filePrefix = "testnet";
		}

		final File f = new File(getDir("blockstore", Context.MODE_PRIVATE),
				"blockchain-" + filePrefix);
		initializeThreads();

		mKit = new WalletAppKit(mNetworkParams, f, filePrefix) {
			@Override
			protected void onSetupCompleted() {
				// File walletFile = new File(f, filePrefix + ".wallet");
				ECKey key = new ECKey();
				List<ECKey> keys = wallet().getKeys();
				if (keys.size() < 1) {
					System.out
							.println("KEY CREATED - " + key.toAddress(params));
					wallet().addKey(key);
				}
				Logger.log("First key is " + keys.get(0).toAddress(params));
				mWalletListener = new BTCWalletEventListener();
				wallet().addEventListener(mWalletListener);
			}
		};
		mKit.start();
		findViewById(R.id.return_to_faucet).setOnClickListener(
				new OnClickListener() {
					public void onClick(View arg0) {
						EditText amountField = (EditText) findViewById(R.id.faucet_amount);
						sendBG("mn6A139WczczyDmwT3MVFQwcLzXdQK6EfM",
								amountField.getText().toString());
					}
				});
	}

	private void sendBG(String address, String amount) {
		new SendCoinTask(address, amount, mNetworkParams, mKit).execute();
	}

	

	private void initializeThreads() {
		mHandler = new Handler();
		Threading.USER_THREAD = new Executor() {
			public void execute(Runnable command) {
				mHandler.post(command);
			}
		};
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(com.mijoro.bitcoin.btcwallet.R.menu.main,
				menu);
		return true;
	}

	private class BTCWalletEventListener extends AbstractWalletEventListener {
		public void onCoinsReceived(Wallet wallet, Transaction tx,
				BigInteger prevBalance, BigInteger newBalance) {
			BigInteger amt = newBalance.subtract(prevBalance);
			Logger.log("onCoinsReceived: Amt: " + amt + " Prev:" + prevBalance
					+ " new:" + newBalance);
		}

		public void onCoinsSent(Wallet wallet, Transaction tx,
				BigInteger prevBalance, BigInteger newBalance) {
			BigInteger amt = newBalance.subtract(prevBalance);
			Logger.log("onCoinsSent: Amt: " + amt + " Prev:" + prevBalance + " new:"
					+ newBalance);
		}

		public void onWalletChanged(Wallet wallet) {
			mBalanceField.setText(BTCUtils.formatBTC(wallet
					.getBalance(BalanceType.ESTIMATED)));
			mVerifiedBalanceField.setText(BTCUtils.formatBTC(wallet
					.getBalance(BalanceType.AVAILABLE)));
		}

		public void onKeysAdded(Wallet wallet, List<ECKey> keys) {
			for (ECKey key : keys) {
				Logger.log("keyAdded" + key.toAddress(mNetworkParams));
			}
		}
	}
}

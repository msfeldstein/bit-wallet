package com.mijoro.bitcoin.btcwallet;

import java.math.BigInteger;
import java.util.Locale;

import javax.annotation.Nonnull;

public class BTCUtils {
	public static final BigInteger ONE_BTC = new BigInteger("100000000", 10);
	public static final BigInteger ONE_MBTC = new BigInteger("100000", 10);

	private static final int ONE_BTC_INT = ONE_BTC.intValue();
	private static final int ONE_MBTC_INT = ONE_MBTC.intValue();
	
	public static String formatBTC(BigInteger amount) {
		return formatValue(amount, 4, 0);
	}
	
	public static String formatValue(@Nonnull final BigInteger value, final int precision, final int shift)
	{
		return formatValue(value, "", "-", precision, shift) + "BTC";
	}

	public static String formatValue(@Nonnull final BigInteger value, @Nonnull final String plusSign, @Nonnull final String minusSign,
			final int precision, final int shift)
	{
		long longValue = value.longValue();

		final String sign = longValue < 0 ? minusSign : plusSign;

		if (shift == 0)
		{
			if (precision == 2)
				longValue = longValue - longValue % 1000000 + longValue % 1000000 / 500000 * 1000000;
			else if (precision == 4)
				longValue = longValue - longValue % 10000 + longValue % 10000 / 5000 * 10000;
			else if (precision == 6)
				longValue = longValue - longValue % 100 + longValue % 100 / 50 * 100;
			else if (precision == 8)
				;
			else
				throw new IllegalArgumentException("cannot handle precision/shift: " + precision + "/" + shift);

			final long absValue = Math.abs(longValue);
			final long coins = absValue / ONE_BTC_INT;
			final int satoshis = (int) (absValue % ONE_BTC_INT);

			if (satoshis % 1000000 == 0)
				return String.format(Locale.US, "%s%d.%02d", sign, coins, satoshis / 1000000);
			else if (satoshis % 10000 == 0)
				return String.format(Locale.US, "%s%d.%04d", sign, coins, satoshis / 10000);
			else if (satoshis % 100 == 0)
				return String.format(Locale.US, "%s%d.%06d", sign, coins, satoshis / 100);
			else
				return String.format(Locale.US, "%s%d.%08d", sign, coins, satoshis);
		}
		else if (shift == 3)
		{
			if (precision == 2)
				longValue = longValue - longValue % 1000 + longValue % 1000 / 500 * 1000;
			else if (precision == 4)
				longValue = longValue - longValue % 10 + longValue % 10 / 5 * 10;
			else if (precision == 5)
				;
			else
				throw new IllegalArgumentException("cannot handle precision/shift: " + precision + "/" + shift);

			final long absValue = Math.abs(longValue);
			final long coins = absValue / ONE_MBTC_INT;
			final int satoshis = (int) (absValue % ONE_MBTC_INT);

			if (satoshis % 1000 == 0)
				return String.format(Locale.US, "%s%d.%02d", sign, coins, satoshis / 1000);
			else if (satoshis % 10 == 0)
				return String.format(Locale.US, "%s%d.%04d", sign, coins, satoshis / 10);
			else
				return String.format(Locale.US, "%s%d.%05d", sign, coins, satoshis);
		}
		else
		{
			throw new IllegalArgumentException("cannot handle shift: " + shift);
		}
	}
}

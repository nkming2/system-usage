package com.nkming.sysusage

import android.util.Pair

object NetUtils
{
	fun getReadableSpeedNoUnit(bps: Long): String
	{
		if (bps >= 1000000000L)
		{
			return "%.1f".format(bps / 1000000000.0)
		}
		else if (bps >= 1000000L)
		{
			return "%.1f".format(bps / 1000000.0)
		}
		else if (bps >= 1000L)
		{
			return "%.1f".format(bps / 1000.0)
		}
		else
		{
			return "$bps"
		}
	}

	fun getReadableBitSpeed(bps: Long): String
	{
		if (bps >= 1000000000L)
		{
			return "%.1f Gbit/s".format(bps / 1000000000.0)
		}
		else if (bps >= 1000000L)
		{
			return "%.1f Mbit/s".format(bps / 1000000.0)
		}
		else if (bps >= 1000L)
		{
			return "%.1f kbit/s".format(bps / 1000.0)
		}
		else
		{
			return "$bps bit/s"
		}
	}

	fun getReadableByteSpeed(bps: Long): String
	{
		if (bps >= 1000000000L)
		{
			return "%.1f GByte/s".format(bps / 1000000000.0)
		}
		else if (bps >= 1000000L)
		{
			return "%.1f MByte/s".format(bps / 1000000.0)
		}
		else if (bps >= 1000L)
		{
			return "%.1f kByte/s".format(bps / 1000.0)
		}
		else
		{
			return "$bps Byte/s"
		}
	}

	/**
	 * Return throughput for various mobile network type. These figures are
	 * pretty random BTW :)
	 *
	 * @param type
	 * @return Downlink and uplink throughput of said network, in bit/s. null if
	 * network unknown
	 */
	fun getMobileNetThroughput(type: String): Pair<Long, Long>
	{
		return when (type.toUpperCase())
		{
			"LTE" -> Pair(15L * 1000000L, 5L * 1000000L)
			"WIMAX" -> Pair(10L * 1000000L, 2L * 1000000L)
			"HSPA+" -> Pair(10L * 1000000L, 2L * 1000000L)
			"HSPA" -> Pair(7200L * 1000L, 1500L * 1000L)
			"3G" -> Pair(500L * 1000L, 500L * 1000L)
			"EDGE" -> Pair(384L * 1000L, 384L * 1000L)
			"2G" -> Pair(172L * 1000L, 172L * 1000L)
			else -> throw IllegalArgumentException("Unknown network type: $type")
		}
	}
}

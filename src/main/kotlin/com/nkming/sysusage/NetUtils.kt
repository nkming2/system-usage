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
	 * Return throughput for various mobile network type
	 *
	 * @param type
	 * @return Downlink and uplink throughput of said network, in bit/s. null if
	 * network unknown
	 */
	fun getMobileNetThroughput(type: String): Pair<Long, Long>
	{
		return when (type.toUpperCase())
		{
			"LTE" -> Pair(100L * 1000000L, 50L * 1000000L)
			"WIMAX" -> Pair(128L * 1000000L, 56L * 1000000L)
			"HSPA+" -> Pair(168L * 1000000L, 22L * 1000000L)
			"HSPA" -> Pair(14400L * 1000L, 5760L * 1000L)
			"3G" -> Pair(4900L * 1000L, 1800L * 1000L)
			"EDGE" -> Pair(473600L, 473600L)
			"2G" -> Pair(64200L, 42800L)
			else -> throw IllegalArgumentException("Unknown network type: $type")
		}
	}
}

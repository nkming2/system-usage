package com.nkming.sysusage

object DiskUtils
{
	fun getReadableSpeedNoUnit(bps: Long): String
	{
		if (bps >= 1073741824L)
		{
			return "%.1f".format(bps / 1073741824.0)
		}
		else if (bps >= 1048576L)
		{
			return "%.1f".format(bps / 1048576.0)
		}
		else if (bps >= 1024L)
		{
			return "%.1f".format(bps / 1024.0)
		}
		else
		{
			return "$bps"
		}
	}

	fun getReadableByteSpeed(bps: Long): String
	{
		if (bps >= 1073741824L)
		{
			return "%.1f GByte/s".format(bps / 1073741824.0)
		}
		else if (bps >= 1048576L)
		{
			return "%.1f MByte/s".format(bps / 1048576.0)
		}
		else if (bps >= 1024L)
		{
			return "%.1f KByte/s".format(bps / 1024.0)
		}
		else
		{
			return "$bps Byte/s"
		}
	}
}

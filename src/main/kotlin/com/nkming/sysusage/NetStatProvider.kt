package com.nkming.sysusage

import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Handler
import android.os.Parcel
import android.os.Parcelable

data class NetStat(
	val isOnline: Boolean,
	// B for Byte
	val txBps: Long,
	val txUsage: Double,
	val rxBps: Long,
	val rxUsage: Double)
	: Parcelable
{
	companion object
	{
		@JvmField
		val CREATOR = object: Parcelable.Creator<NetStat>
		{
			override fun createFromParcel(source: Parcel): NetStat
			{
				return NetStat((source.readInt() != 0),
						source.readLong(),
						source.readDouble(),
						source.readLong(),
						source.readDouble())
			}

			override fun newArray(size: Int): Array<NetStat?>
			{
				return arrayOfNulls(size)
			}
		}
	}

	override fun describeContents() = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeInt(if (isOnline) 1 else 0)
		dest.writeLong(txBps)
		dest.writeDouble(txUsage)
		dest.writeLong(rxBps)
		dest.writeDouble(rxUsage)
	}
}

class NetStatProvider(context: Context,
		onStatUpdate: ((stat: NetStat) -> Unit)? = null,
		onFailure: (() -> Unit)? = null)
{
	fun init(interval: Long)
	{
		stop()
		this.interval = interval
		_onUpdate = object: Runnable
		{
			override fun run()
			{
				onUpdate()
				_handler.postDelayed(this, this@NetStatProvider.interval)
			}
		}
	}

	fun start()
	{
		_onUpdate ?: return
		if (!_isStarted)
		{
			_handler.post(_onUpdate)
			_isStarted = true
		}
	}

	fun stop()
	{
		_onUpdate ?: return
		_handler.removeCallbacks(_onUpdate)
		_isStarted = false
	}

	var onStatUpdate = onStatUpdate
	var onFailure = onFailure
	var interval: Long = 2000

	// Throughput is in bit/s
	var mobileRxThroughput = 0L
	var mobileTxThroughput = 0L
	var wifiRxThroughput = 0L
	var wifiTxThroughput = 0L

	private fun onUpdate()
	{
		val now = System.currentTimeMillis()
		val tx = TrafficStats.getTotalTxBytes()
		val rx = TrafficStats.getTotalRxBytes()
		try
		{
			if (!_isFirstRun)
			{
				val divisor = (now - _prevTime) / 1000.0
				val rxBps = (rx - _prevRx) / divisor
				val txBps = (tx - _prevTx) / divisor
				val throughputs = _activeThroughputs
				val stat = if (throughputs == null)
				{
					NetStat(false, 0, .0, 0, .0)
				}
				else
				{
					val rxUsage = if (throughputs.first > 0)
									(rxBps * 8.0) / throughputs.first
							else 0.0
					val txUsage = if (throughputs.second > 0)
									(txBps * 8.0) / throughputs.second
							else 0.0
					NetStat(true, Math.round(txBps).toLong(), txUsage,
							Math.round(rxBps).toLong(), rxUsage)
				}
				onStatUpdate?.invoke(stat)
			}
			else
			{
				onStatUpdate?.invoke(NetStat(false, 0, .0, 0, .0))
			}
		}
		finally
		{
			_prevTime = now
			_prevTx = tx
			_prevRx = rx
			_isFirstRun = false
		}
	}

	private val _activeThroughputs: Pair<Long, Long>?
		get()
		{
			val netInfo = _connectivityManager.activeNetworkInfo ?: return null
			if (!netInfo.isConnected)
			{
				return null
			}
			return when (netInfo.type)
			{
				ConnectivityManager.TYPE_ETHERNET,
				ConnectivityManager.TYPE_WIFI ->
						Pair(wifiRxThroughput, wifiTxThroughput)

				else -> Pair(mobileRxThroughput, mobileTxThroughput)
			}
		}

	private val _handler = Handler()
	private val _context = context
	private val _connectivityManager by lazy{_context.getSystemService(
			Context.CONNECTIVITY_SERVICE) as ConnectivityManager}
	private var _onUpdate: Runnable? = null
	private var _isStarted = false

	private var _isFirstRun = true
	private var _prevTime = 0L
	private var _prevTx = 0L
	private var _prevRx = 0L
}

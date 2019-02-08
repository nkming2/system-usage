package com.nkming.sysusage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nkming.utils.Log
import java.util.*

class CpuStatService : BaseStatService()
{
	companion object
	{
		@JvmStatic
		fun plug(context: Context)
		{
			context.startService(Intent(context, CpuStatService::class.java))
		}

		@JvmStatic
		fun unplug(context: Context)
		{
			val i = Intent(context, CpuStatService::class.java)
			i.action = Res.ACTION_UNPLUG
			context.startService(i)
		}

		private val LOG_TAG = CpuStatService::class.java.canonicalName

		// Update interval == INTERVAL * AVG_WINDOW
		private const val INTERVAL_BASE = 100L
		private const val AVG_WINDOW = 5
		private const val FAILURE_THRESHOLD = 5
	}

	override fun onCreate()
	{
		super.onCreate()
		Log.d(LOG_TAG, "onCreate()")
		_pref.onSharedPreferenceChangeListener =
				SharedPreferences.OnSharedPreferenceChangeListener{pref, key ->
					if (key == getString(R.string.pref_interval_mul_key))
					{
						_statProvider.interval =
								_pref.intervalMul * INTERVAL_BASE
					}
				}
		_statProvider.init(_pref.intervalMul * INTERVAL_BASE)
	}

	private fun createCpuStatProvider(): CpuStatProvider
	{
		val buffer = ArrayList<CpuStat>(AVG_WINDOW)
		return CpuStatProvider(this, {
			buffer += it
			if (buffer.size >= AVG_WINDOW)
			{
				val avg = getAvgStatus(buffer)
				buffer.clear()

				val i = Intent(Res.ACTION_CPU_STAT_AVAILABLE)
				i.putExtra(Res.EXTRA_STAT, avg)
				_broadcastManager.sendBroadcast(i)
			}
			_failureCount = 0
		},
		{
			if (++_failureCount > FAILURE_THRESHOLD)
			{
				throw RuntimeException(it)
			}
		})
	}

	private fun getAvgStatus(buffer: List<CpuStat>): CpuStat
	{
		val cores = ArrayList<MutableCpuCoreStat>(buffer[0].cores.size)
		for (i in 0 until buffer[0].cores.size)
		{
			cores += MutableCpuCoreStat(false, .0)
		}
		for (b in buffer)
		{
			for ((c, bc) in cores.zip(b.cores))
			{
				c.isOnline = c.isOnline || bc.isOnline
				c.usage += bc.usage / buffer.size
			}
		}
		return CpuStat(cores.map{it.immutable()})
	}

	protected override val _statProvider by lazy{createCpuStatProvider()}

	private var _failureCount = 0
	private val _broadcastManager by lazy{LocalBroadcastManager.getInstance(this)}
	private val _pref by lazy{Preference.from(this)}
}

package com.nkming.sysusage

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.support.v4.content.LocalBroadcastManager
import com.nkming.utils.Log

class MemStatService : Service()
{
	companion object
	{
		@JvmStatic
		fun plug(context: Context)
		{
			context.startService(Intent(context, MemStatService::class.java))
		}

		@JvmStatic
		fun unplug(context: Context)
		{
			val i = Intent(context, MemStatService::class.java)
			i.action = Res.ACTION_UNPLUG
			context.startService(i)
		}

		private val LOG_TAG = MemStatService::class.java.canonicalName

		private const val INTERVAL_BASE = 500L
	}

	override fun onBind(intent: Intent?) = null

	override fun onCreate()
	{
		super.onCreate()
		Log.d(LOG_TAG, "onCreate()")
		_pref.onSharedPreferenceChangeListener =
				SharedPreferences.OnSharedPreferenceChangeListener{pref, key ->
				run{
					if (key == getString(R.string.pref_interval_mul_key))
					{
						_statProvider.interval =
								_pref.intervalMul * INTERVAL_BASE
					}
				}}
		_statProvider.init(_pref.intervalMul * INTERVAL_BASE)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d(LOG_TAG, "onStartCommand()")
		if (intent == null)
		{
			// We are not supposed to receive null here
			stopSelf()
			return START_NOT_STICKY
		}

		if (intent.action == Res.ACTION_UNPLUG)
		{
			if (--_plugCount <= 0)
			{
				stopSelf()
			}
		}
		else
		{
			_statProvider.start()
			++_plugCount
		}
		return START_NOT_STICKY
	}

	override fun onDestroy()
	{
		super.onDestroy()
		Log.d(LOG_TAG, "onDestroy()")
		_statProvider.stop()
	}

	private fun createMemoryStatProvider(): MemoryStatProvider
	{
		return MemoryStatProvider(this,
		{
			val i = Intent(Res.ACTION_MEM_STAT_AVAILABLE)
			i.putExtra(Res.EXTRA_STAT, it)
			_broadcastManager.sendBroadcast(i)
		})
	}

	private var _plugCount = 0
	private val _statProvider by lazy{createMemoryStatProvider()}
	private val _broadcastManager by lazy{LocalBroadcastManager.getInstance(this)}
	private val _pref by lazy{Preference.from(this)}
}

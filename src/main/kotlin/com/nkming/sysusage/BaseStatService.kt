package com.nkming.sysusage

import android.app.Service
import android.content.*
import com.nkming.utils.Log

abstract class BaseStatService : Service()
{
	override fun onCreate()
	{
		super.onCreate()
		Log.d(LOG_TAG_C, "onCreate()")
		if (_pref.isOptimizeBattery)
		{
			initScreenReceiver()
		}
		_pref.onSharedPreferenceChangeListener =
				SharedPreferences.OnSharedPreferenceChangeListener{pref, key ->
						onSharedPreferenceChanged(pref, key)}
	}

	override fun onBind(intent: Intent?) = null

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d(LOG_TAG_C, "onStartCommand()")
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
		Log.d(LOG_TAG_C, "onDestroy()")
		_statProvider.stop()
		uninitScreenReceiver()
	}

	protected abstract val _statProvider: BaseStatProvider

	private fun initScreenReceiver()
	{
		if (!_hasInitScreenReceiver)
		{
			registerReceiver(_screenOnReceiver,
					IntentFilter(Intent.ACTION_SCREEN_ON))
			registerReceiver(_screenOffReceiver,
					IntentFilter(Intent.ACTION_SCREEN_OFF))
			_hasInitScreenReceiver = true
		}
	}

	private fun uninitScreenReceiver()
	{
		if (_hasInitScreenReceiver)
		{
			unregisterReceiver(_screenOnReceiver)
			unregisterReceiver(_screenOffReceiver)
			_hasInitScreenReceiver = false
		}
	}

	private fun onSharedPreferenceChanged(pref: SharedPreferences, key: String)
	{
		when (key)
		{
			getString(R.string.pref_optimize_battery_key) ->
					if (_pref.isOptimizeBattery)
					{
						initScreenReceiver()
					}
					else
					{
						uninitScreenReceiver()
					}
		}
	}

	private val LOG_TAG_C = "*${this.javaClass.canonicalName}"

	private val _screenOnReceiver = object: BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent?)
		{
			_statProvider.start()
		}
	}

	private val _screenOffReceiver = object: BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent?)
		{
			_statProvider.stop()
		}
	}

	private var _plugCount = 0
	private var _hasInitScreenReceiver = false
	private val _pref by lazy{Preference.from(this)}
}

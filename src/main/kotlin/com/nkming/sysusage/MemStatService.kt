package com.nkming.sysusage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nkming.utils.Log

class MemStatService : BaseStatService()
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

	private fun createMemoryStatProvider(): MemoryStatProvider
	{
		return MemoryStatProvider(this, {
			val i = Intent(Res.ACTION_MEM_STAT_AVAILABLE)
			i.putExtra(Res.EXTRA_STAT, it)
			_broadcastManager.sendBroadcast(i)
		}, {msg, e ->
			Log.e("$LOG_TAG.createMemoryStatProvider", msg, e)
			val i = Intent(Res.ACTION_MEM_STAT_AVAILABLE)
			i.putExtra(Res.EXTRA_STAT, MemStat())
			_broadcastManager.sendBroadcast(i)
		})
	}

	protected override val _statProvider by lazy{createMemoryStatProvider()}

	private val _broadcastManager by lazy{LocalBroadcastManager.getInstance(this)}
	private val _pref by lazy{Preference.from(this)}
}

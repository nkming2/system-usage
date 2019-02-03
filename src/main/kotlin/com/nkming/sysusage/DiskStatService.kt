package com.nkming.sysusage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nkming.utils.Log

class DiskStatService : BaseStatService()
{
	companion object
	{
		@JvmStatic
		fun plug(context: Context)
		{
			context.startService(Intent(context, DiskStatService::class.java))
		}

		@JvmStatic
		fun unplug(context: Context)
		{
			val i = Intent(context, DiskStatService::class.java)
			i.action = Res.ACTION_UNPLUG
			context.startService(i)
		}

		private val LOG_TAG = DiskStatService::class.java.canonicalName

		private const val INTERVAL_BASE = 500L
	}

	override fun onCreate()
	{
		super.onCreate()
		Log.d(LOG_TAG, "onCreate()")
		_pref.onSharedPreferenceChangeListener =
				SharedPreferences.OnSharedPreferenceChangeListener{pref, key ->
					when (key)
					{
						getString(R.string.pref_interval_mul_key) ->
								_statProvider.interval =
										_pref.intervalMul * INTERVAL_BASE

						getString(R.string.pref_disk_read_key) ->
								_statProvider.readThroughput = _pref.diskRead

						getString(R.string.pref_disk_write_key) ->
								_statProvider.writeThroughput = _pref.diskWrite
					}}
		_statProvider.readThroughput = _pref.diskRead
		_statProvider.writeThroughput = _pref.diskWrite
		_statProvider.init(_pref.intervalMul * INTERVAL_BASE)
	}

	protected override val _statProvider by lazy{createDiskStatProvider()}

	private fun createDiskStatProvider(): DiskStatProvider
	{
		return DiskStatProvider(this,
		{
			val i = Intent(Res.ACTION_DISK_STAT_AVAILABLE)
			i.putExtra(Res.EXTRA_STAT, it)
			_broadcastManager.sendBroadcast(i)
		})
	}

	private val _broadcastManager by lazy{LocalBroadcastManager.getInstance(this)}
	private val _pref by lazy{Preference.from(this)}
}

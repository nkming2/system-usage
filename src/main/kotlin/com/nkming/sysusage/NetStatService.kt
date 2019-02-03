package com.nkming.sysusage

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nkming.utils.Log

class NetStatService : BaseStatService()
{
	companion object
	{
		@JvmStatic
		fun plug(context: Context)
		{
			context.startService(Intent(context, NetStatService::class.java))
		}

		@JvmStatic
		fun unplug(context: Context)
		{
			val i = Intent(context, NetStatService::class.java)
			i.action = Res.ACTION_UNPLUG
			context.startService(i)
		}

		private val LOG_TAG = NetStatService::class.java.canonicalName

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

							getString(R.string.pref_net_mobile_dl_key) ->
									_statProvider.mobileRxThroughput =
											_pref.netMobileDl

							getString(R.string.pref_net_mobile_ul_key) ->
									_statProvider.mobileTxThroughput =
											_pref.netMobileUl

							getString(R.string.pref_net_wifi_dl_key) ->
									_statProvider.wifiRxThroughput =
											_pref.netWifiDl

							getString(R.string.pref_net_wifi_ul_key) ->
									_statProvider.wifiTxThroughput =
											_pref.netWifiUl
						}}
		_statProvider.mobileRxThroughput = _pref.netMobileDl
		_statProvider.mobileTxThroughput = _pref.netMobileUl
		_statProvider.wifiRxThroughput = _pref.netWifiDl
		_statProvider.wifiTxThroughput = _pref.netWifiUl
		_statProvider.init(_pref.intervalMul * INTERVAL_BASE)
	}

	private fun createNetStatProvider(): NetStatProvider
	{
		return NetStatProvider(this,
		{
			val i = Intent(Res.ACTION_NET_STAT_AVAILABLE)
			i.putExtra(Res.EXTRA_STAT, it)
			_broadcastManager.sendBroadcast(i)
		})
	}

	protected override val _statProvider by lazy{createNetStatProvider()}

	private val _broadcastManager by lazy{LocalBroadcastManager.getInstance(this)}
	private val _pref by lazy{Preference.from(this)}
}

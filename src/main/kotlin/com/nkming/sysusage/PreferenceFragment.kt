package com.nkming.sysusage

import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.TextView
import com.nkming.utils.Log
import com.nkming.utils.preference.PreferenceFragmentEx
import com.nkming.utils.preference.SeekBarPreference

class PreferenceFragment : PreferenceFragmentEx(),
		SharedPreferences.OnSharedPreferenceChangeListener
{
	companion object
	{
		@JvmStatic
		fun create(): PreferenceFragment
		{
			return PreferenceFragment()
		}

		private val LOG_TAG = "PreferenceFragment"
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		preferenceManager.sharedPreferencesName = getString(R.string.pref_file)
		addPreferencesFromResource(R.xml.preference)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		init()
	}

	override fun onResume()
	{
		super.onResume()
		preferenceManager.sharedPreferences
				.registerOnSharedPreferenceChangeListener(this)
	}

	override fun onPause()
	{
		super.onPause()
		preferenceManager.sharedPreferences
				.unregisterOnSharedPreferenceChangeListener(this)
	}

	override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String)
	{
		if (key == getString(R.string.pref_enable_cpu_notif_key))
		{
			onEnableCpuNotifChange(pref.getBoolean(key, true))
		}
		else if (key == getString(R.string.pref_enable_mem_notif_key))
		{
			onEnableMemNotifChange(pref.getBoolean(key, true))
		}
		else if (key == getString(R.string.pref_enable_net_notif_key))
		{
			onEnableNetNotifChange(pref.getBoolean(key, true))
		}
		else if (key == getString(R.string.pref_net_mobile_template_key))
		{
			onNetMobileTemplateChange(pref.getString(key, "LTE"))
		}
	}

	private fun init()
	{
		initIntervalMulPref()
	}

	private fun initIntervalMulPref()
	{
		val pref = findPreference(getString(R.string.pref_interval_mul_key))
				as SeekBarPreference
		pref.setPreviewListener(object: SeekBarPreference.DefaultPreviewListener()
		{
			override fun onPreviewChange(preview: View, value: Int)
			{
				val v = preview as TextView
				v.text = "%.1fs".format(value * 500 / 1000.0)
			}
		})
		pref.setSummaryListener{"%.1fs".format(it * 500 / 1000.0)}
	}

	private fun onEnableCpuNotifChange(v: Boolean)
	{
		if (v)
		{
			NotifService.start(activity)
		}
	}

	private fun onEnableMemNotifChange(v: Boolean)
	{
		if (v)
		{
			NotifService.start(activity)
		}
	}

	private fun onEnableNetNotifChange(v: Boolean)
	{
		if (v)
		{
			NotifService.start(activity)
		}
	}

	private fun onNetMobileTemplateChange(v: String)
	{
		try
		{
			val throughput = NetUtils.getMobileNetThroughput(v)
			_netMobileDlPref.setValue(throughput.first)
			_netMobileUlPref.setValue(throughput.second)
		}
		catch (e: IllegalArgumentException)
		{
			Log.e("$LOG_TAG.onNetMobileTemplateChange",
					"Failed while getMobileNetThroughput()", e)
		}
	}

	private val _netMobileDlPref by viewAwareLazy(
	{
		findPreference(activity.getString(R.string.pref_net_mobile_dl_key))
				as NetThroughputPreference
	})

	private val _netMobileUlPref by viewAwareLazy(
	{
		findPreference(activity.getString(R.string.pref_net_mobile_ul_key))
				as NetThroughputPreference
	})
}

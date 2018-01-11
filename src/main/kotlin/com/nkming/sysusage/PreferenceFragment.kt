package com.nkming.sysusage

import android.annotation.TargetApi
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceGroup
import android.provider.Settings
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
		else if (key == getString(R.string.pref_enable_disk_notif_key))
		{
			onEnableDiskNotifChange(pref.getBoolean(key, true))
		}
		else if (key == getString(R.string.pref_enable_key))
		{
			onEnableChange(pref.getBoolean(key, true))
		}
	}

	private fun init()
	{
		initCpuPref()
		initIntervalMulPref()
		initPriorityPref()
		initLockScreenPref()
		initAbout()
	}

	private fun initCpuPref()
	{
		val group = preferenceScreen.findPreference(getString(
				R.string.pref_cpu_notification_category_key)) as PreferenceGroup
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			group.removePreference(findPreference(getString(
					R.string.pref_enable_cpu_notif_key)))
			group.removePreference(findPreference(getString(
					R.string.pref_overall_cpu_key)))
		}
		else
		{
			group.removePreference(findPreference(getString(
					R.string.pref_cpu_compat_note_key)))
		}
	}

	private fun initPriorityPref()
	{
		val pref = findPreference(getString(R.string.pref_high_priority_key))
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			// On O+, user should config via notification channel
			preferenceScreen.removePreference(pref)
		}
	}

	private fun initLockScreenPref()
	{
		val pref = findPreference(getString(R.string.pref_lock_screen_key))
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M)
		{
			pref.isEnabled = false
		}
		else
		{
			pref.setOnPreferenceClickListener{
				startNotifSetting()
				true
			}
		}
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

	private fun initAbout()
	{
		val aboutVersion = findPreference(getString(R.string.about_version_key))
		aboutVersion.summary = BuildConfig.VERSION_NAME
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

	private fun onEnableDiskNotifChange(v: Boolean)
	{
		if (v)
		{
			NotifService.start(activity)
		}
	}

	private fun onEnableChange(v: Boolean)
	{
		if (v)
		{
			val pref = Preference.from(activity)
			if (pref.isEnableCpu || pref.isEnableMem || pref.isEnableNet
					|| pref.isEnableDisk)
			{
				NotifService.start(activity)
			}
		}
	}

	private fun startNotifSetting()
	{
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			startNotifSettingO()
		}
		else if (Build.VERSION.SDK_INT
				in Build.VERSION_CODES.LOLLIPOP..Build.VERSION_CODES.N_MR1)
		{
			startNotifSettingL()
		}
	}

	@TargetApi(Build.VERSION_CODES.O)
	private fun startNotifSettingO()
	{
		val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
		intent.putExtra(Settings.EXTRA_APP_PACKAGE, BuildConfig.APPLICATION_ID)
		startActivity(intent)
	}

	private fun startNotifSettingL()
	{
		try
		{
			// Undocumented
			val intent = Intent("android.settings.APP_NOTIFICATION_SETTINGS")
			intent.putExtra("app_package", BuildConfig.APPLICATION_ID)
			intent.putExtra("app_uid", activity.applicationInfo.uid)
			startActivity(intent)
		}
		catch (e: Throwable)
		{
			val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
			intent.data = Uri.parse("package:${BuildConfig.APPLICATION_ID}")
			startActivity(intent)
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

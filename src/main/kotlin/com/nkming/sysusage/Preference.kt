package com.nkming.sysusage

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

class Preference(context: Context, pref: SharedPreferences)
{
	companion object
	{
		@JvmStatic
		fun from(context: Context): Preference
		{
			return Preference(context, context.getSharedPreferences(
					context.getString(R.string.pref_file), Context.MODE_PRIVATE))
		}
	}

	fun commit(): Boolean
	{
		return _editLock.withLock(
		{
			if (_edit.commit())
			{
				__edit = null
				return true
			}
			else
			{
				return false
			}
		})
	}

	fun apply()
	{
		_editLock.withLock(
		{
			_edit.apply()
			__edit = null
		})
	}

	var onSharedPreferenceChangeListener
			: SharedPreferences.OnSharedPreferenceChangeListener? = null
		set(v)
		{
			if (field != null)
			{
				_pref.unregisterOnSharedPreferenceChangeListener(field)
			}
			if (v != null)
			{
				_pref.registerOnSharedPreferenceChangeListener(v)
				field = v
			}
		}

	var hasRunSetupWizard: Boolean
		get()
		{
			return _pref.getBoolean(_runSetupWizardKey, false)
		}
		set(v)
		{
			_edit.putBoolean(_runSetupWizardKey, v)
		}

	var lastVersion: Int
		get()
		{
			return _pref.getInt(_lastVersionKey, -1)
		}
		set(v)
		{
			_edit.putInt(_lastVersionKey, v)
		}

	var intervalMul: Int
		get()
		{
			return Math.max(_pref.getInt(_intervalMulKey, 4), 2)
		}
		set(v)
		{
			_edit.putInt(_intervalMulKey, v)
		}

	var isHighPriority: Boolean
		get()
		{
			return _pref.getBoolean(_highPriorityKey, false)
		}
		set(v)
		{
			_edit.putBoolean(_highPriorityKey, v)
		}

	var isAutorun: Boolean
		get()
		{
			return _pref.getBoolean(_autorunKey, false)
		}
		set(v)
		{
			_edit.putBoolean(_autorunKey, v)
		}

	var isOptimizeBattery: Boolean
		get()
		{
			return _pref.getBoolean(_optimizeBatteryKey, true)
		}
		set(v)
		{
			_edit.putBoolean(_optimizeBatteryKey, v)
		}

	var isEnableCpu: Boolean
		get()
		{
			return _pref.getBoolean(_enableCpuNotifKey, true)
		}
		set(v)
		{
			_edit.putBoolean(_enableCpuNotifKey, v)
		}

	var isOverallCpu: Boolean
		get()
		{
			return _pref.getBoolean(_overallCpuKey, true)
		}
		set(v)
		{
			_edit.putBoolean(_overallCpuKey, v)
		}

	var isEnableMem: Boolean
		get()
		{
			return _pref.getBoolean(_enableMemNotifKey, true)
		}
		set(v)
		{
			_edit.putBoolean(_enableMemNotifKey, v)
		}

	var isEnableNet: Boolean
		get()
		{
			return _pref.getBoolean(_enableNetNotifKey, true)
		}
		set(v)
		{
			_edit.putBoolean(_enableNetNotifKey, v)
		}

	var netMobileTemplate: String
		get()
		{
			return _pref.getString(_netMobileTemplateKey, "LTE")
		}
		set(v)
		{
			_edit.putString(_netMobileTemplateKey, v)
		}

	var netMobileDl: Long
		get()
		{
			return _pref.getLong(_netMobileDlKey, 1000000L)
		}
		set(v)
		{
			_edit.putLong(_netMobileDlKey, v)
		}

	var netMobileUl: Long
		get()
		{
			return _pref.getLong(_netMobileUlKey, 1000000L)
		}
		set(v)
		{
			_edit.putLong(_netMobileUlKey, v)
		}

	var netWifiDl: Long
		get()
		{
			return _pref.getLong(_netWifiDlKey, 100000000L)
		}
		set(v)
		{
			_edit.putLong(_netWifiDlKey, v)
		}

	var netWifiUl: Long
		get()
		{
			return _pref.getLong(_netWifiUlKey, 100000000L)
		}
		set(v)
		{
			_edit.putLong(_netWifiUlKey, v)
		}

	var isEnableDisk: Boolean
		get()
		{
			return _pref.getBoolean(_enableDiskNotifKey, true)
		}
		set(v)
		{
			_edit.putBoolean(_enableDiskNotifKey, v)
		}

	var diskRead: Long
		get()
		{
			return _pref.getLong(_diskReadKey, 20971520L)
		}
		set(v)
		{
			_edit.putLong(_diskReadKey, v)
		}

	var diskWrite: Long
		get()
		{
			return _pref.getLong(_diskWriteKey, 10485760L)
		}
		set(v)
		{
			_edit.putLong(_diskWriteKey, v)
		}

	var isDarkTheme: Boolean
		get()
		{
			return _pref.getBoolean(_darkThemeKey, true)
		}
		set(v)
		{
			_edit.putBoolean(_darkThemeKey, v)
		}

	private val _runSetupWizardKey by lazy{_context.getString(
			R.string.pref_run_setup_wizard_key)}
	private val _lastVersionKey by lazy{_context.getString(
			R.string.pref_last_version_key)}

	private val _intervalMulKey by lazy{_context.getString(
			R.string.pref_interval_mul_key)}
	private val _highPriorityKey by lazy{_context.getString(
			R.string.pref_high_priority_key)}
	private val _autorunKey by lazy{_context.getString(
			R.string.pref_autorun_key)}
	private val _optimizeBatteryKey by lazy{_context.getString(
			R.string.pref_optimize_battery_key)}

	private val _enableCpuNotifKey by lazy{_context.getString(
			R.string.pref_enable_cpu_notif_key)}
	private val _overallCpuKey by lazy{_context.getString(
			R.string.pref_overall_cpu_key)}

	private val _enableMemNotifKey by lazy{_context.getString(
			R.string.pref_enable_mem_notif_key)}

	private val _enableNetNotifKey by lazy{_context.getString(
			R.string.pref_enable_net_notif_key)}
	private val _netMobileTemplateKey by lazy{_context.getString(
			R.string.pref_net_mobile_template_key)}
	private val _netMobileDlKey by lazy{_context.getString(
			R.string.pref_net_mobile_dl_key)}
	private val _netMobileUlKey by lazy{_context.getString(
			R.string.pref_net_mobile_ul_key)}
	private val _netWifiDlKey by lazy{_context.getString(
			R.string.pref_net_wifi_dl_key)}
	private val _netWifiUlKey by lazy{_context.getString(
			R.string.pref_net_wifi_ul_key)}

	private val _enableDiskNotifKey by lazy{_context.getString(
			R.string.pref_enable_disk_notif_key)}
	private val _diskReadKey by lazy{_context.getString(
			R.string.pref_disk_read_key)}
	private val _diskWriteKey by lazy{_context.getString(
			R.string.pref_disk_write_key)}

	private val _darkThemeKey by lazy{_context.getString(
			R.string.pref_dark_theme_key)}

	private val _context = context
	private val _pref = pref
	private val _edit: SharedPreferences.Editor
		get()
		{
			_editLock.withLock(
			{
				if (__edit == null)
				{
					__edit = _pref.edit()
				}
				return __edit!!
			})
		}
	private var __edit: SharedPreferences.Editor? = null
	private val _editLock = ReentrantLock(true)
}

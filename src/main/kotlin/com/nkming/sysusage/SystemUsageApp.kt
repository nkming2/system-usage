package com.nkming.sysusage

import android.app.Application
import android.content.Context
import android.preference.PreferenceManager
import com.nkming.utils.Log

class SystemUsageApp : Application()
{
	override fun onCreate()
	{
		super.onCreate()
		initLog()
		initDefaultPref()
		startServiceIfNeeded()
	}

	private fun initLog()
	{
		Log.isShowDebug = BuildConfig.DEBUG
		Log.isShowVerbose = BuildConfig.DEBUG
	}

	private fun initDefaultPref()
	{
		PreferenceManager.setDefaultValues(this, getString(R.string.pref_file),
				Context.MODE_PRIVATE, R.xml.preference, false)
	}

	private fun startServiceIfNeeded()
	{
		val pref = Preference.from(this)
		if (pref.isEnableCpu || pref.isEnableMem || pref.isEnableNet
				|| pref.isEnableDisk)
		{
			NotifService.start(this)
		}
	}
}

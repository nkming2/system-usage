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

		val pref = Preference.from(this)
		migrateVersion(pref)
		startServiceIfNeeded(pref)
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

	private fun migrateVersion(pref: Preference)
	{
		if (pref.lastVersion == BuildConfig.VERSION_CODE)
		{
			// Same version
			return
		}
		else if (pref.lastVersion == -1)
		{
			// New install
		}
		else if (pref.lastVersion < BuildConfig.VERSION_CODE)
		{
			// Upgrade
			// Currently no migration needed
		}
		else if (pref.lastVersion > BuildConfig.VERSION_CODE)
		{
			// Downgrade o.O
		}
		pref.lastVersion = BuildConfig.VERSION_CODE
		pref.commit()
	}

	private fun startServiceIfNeeded(pref: Preference)
	{
		if (!pref.hasRunSetupWizard)
		{
			return
		}
		if (pref.isEnableCpu || pref.isEnableMem || pref.isEnableNet
				|| pref.isEnableDisk)
		{
			NotifService.start(this)
		}
	}
}

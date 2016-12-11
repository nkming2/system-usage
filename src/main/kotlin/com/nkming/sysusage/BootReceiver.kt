package com.nkming.sysusage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver()
{
	override fun onReceive(context: Context, intent: Intent)
	{
		val pref = Preference.from(context)
		if (pref.isAutorun)
		{
			NotifService.start(context)
		}
	}
}

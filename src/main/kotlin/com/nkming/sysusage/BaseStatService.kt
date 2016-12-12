package com.nkming.sysusage

import android.app.Service
import android.content.Intent
import com.nkming.utils.Log

abstract class BaseStatService : Service()
{
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
	}

	protected abstract val _statProvider: BaseStatProvider

	private val LOG_TAG_C = "*${this.javaClass.canonicalName}"

	private var _plugCount = 0
}

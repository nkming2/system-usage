package com.nkming.sysusage

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.*
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.NotificationCompat
import com.nkming.utils.Log

class NotifService : Service(),
		SharedPreferences.OnSharedPreferenceChangeListener
{
	companion object
	{
		@JvmStatic
		fun start(context: Context)
		{
			context.startService(Intent(context, NotifService::class.java))
		}

		private val LOG_TAG = NotifService::class.java.canonicalName
	}

	override fun onBind(intent: Intent?) = null

	override fun onCreate()
	{
		super.onCreate()
		Log.d(LOG_TAG, "onCreate()")
		_broadcastManager.registerReceiver(_cpuStatAvailableReceiver,
				IntentFilter(Res.ACTION_CPU_STAT_AVAILABLE))
		_broadcastManager.registerReceiver(_memStatAvailableReceiver,
				IntentFilter(Res.ACTION_MEM_STAT_AVAILABLE))
		_broadcastManager.registerReceiver(_netStatAvailableReceiver,
				IntentFilter(Res.ACTION_NET_STAT_AVAILABLE))
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int
	{
		Log.d(LOG_TAG, "onStartCommand()")
		if (!_hasInit)
		{
			_hasInit = true
			startForeground(1, buildLoadingNotif())

			_pref.onSharedPreferenceChangeListener = this
			if (_pref.isEnableCpu)
			{
				enableCpu()
			}
			if (_pref.isEnableMem)
			{
				enableMem()
			}
			if (_pref.isEnableNet)
			{
				enableNet()
			}

			stopIfDisabled()
		}
		return START_STICKY
	}

	override fun onDestroy()
	{
		super.onDestroy()
		Log.d(LOG_TAG, "onDestroy()")
		stopForeground(false)
		for (i in 0.._notifCount - 1)
		{
			_notifManager.cancel(i + 1)
		}

		if (_isEnableCpu)
		{
			CpuStatService.unplug(this)
		}
		if (_isEnableMem)
		{
			MemStatService.unplug(this)
		}
		if (_isEnableNet)
		{
			NetStatService.unplug(this)
		}

		_pref.onSharedPreferenceChangeListener = null
	}

	override fun onSharedPreferenceChanged(pref: SharedPreferences, key: String)
	{
		if (key == getString(R.string.pref_enable_cpu_notif_key))
		{
			if (pref.getBoolean(key, true))
			{
				enableCpu()
			}
			else
			{
				disableCpu()
			}
		}
		else if (key == getString(R.string.pref_enable_mem_notif_key))
		{
			if (pref.getBoolean(key, true))
			{
				enableMem()
			}
			else
			{
				disableMem()
			}
		}
		else if (key == getString(R.string.pref_enable_net_notif_key))
		{
			if (pref.getBoolean(key, true))
			{
				enableNet()
			}
			else
			{
				disableNet()
			}
		}
		else if (key == getString(R.string.pref_overall_cpu_key))
		{
			_cpuNotifBuilder.isOverall = pref.getBoolean(key, true)
		}
		else if (key == getString(R.string.pref_high_priority_key))
		{
			val priority = if (pref.getBoolean(key, false))
					NotificationCompat.PRIORITY_HIGH else
					NotificationCompat.PRIORITY_LOW
			_cpuNotifBuilder.priority = priority
			_memNotifBuilder.priority = priority
			_netNotifBuilder.priority = priority
		}
	}

	private fun onStatAvailable(stat: CpuStat)
	{
		_cpuStat = stat
		postStatAvailable()
	}

	private fun onStatAvailable(stat: MemStat)
	{
		_memStat = stat
		postStatAvailable()
	}

	private fun onStatAvailable(stat: NetStat)
	{
		_netStat = stat
		postStatAvailable()
	}

	private fun postStatAvailable()
	{
		if ((!_isEnableCpu || _cpuStat != null)
				&& (!_isEnableMem || _memStat != null)
				&& (!_isEnableNet || _netStat != null))
		{
			val notifs = arrayListOf<Notification>()
			if (_isEnableCpu)
			{
				val cpuNotifs = _cpuNotifBuilder.build(_cpuStat!!, _when)
				notifs += cpuNotifs
				_cpuStat = null
			}
			if (_isEnableMem)
			{
				val memNotif = _memNotifBuilder.build(_memStat!!, _when - 1000)
				notifs += memNotif
				_memStat = null
			}
			if (_isEnableNet)
			{
				val netNotifs = _netNotifBuilder.build(_netStat!!, _when - 2000)
				notifs += netNotifs
				_netStat = null
			}

			for ((i, n) in notifs.withIndex())
			{
				_notifManager.notify(i + 1, n)
			}

			if (_notifCount > notifs.size)
			{
				// e.g., overall stat option has changed
				for (i in notifs.size.._notifCount - 1)
				{
					_notifManager.cancel(i + 1)
				}
			}
			_notifCount = notifs.size
		}
	}

	private fun enableCpu()
	{
		Log.d(LOG_TAG, "enableCpu()")
		if (!_isEnableCpu)
		{
			_isEnableCpu = true
			CpuStatService.plug(this)
		}
	}

	private fun disableCpu()
	{
		Log.d(LOG_TAG, "disableCpu()")
		if (_isEnableCpu)
		{
			CpuStatService.unplug(this)
			_isEnableCpu = false
			stopIfDisabled()
		}
	}

	private fun enableMem()
	{
		Log.d(LOG_TAG, "enableMem()")
		if (!_isEnableMem)
		{
			_isEnableMem = true
			MemStatService.plug(this)
		}
	}

	private fun disableMem()
	{
		Log.d(LOG_TAG, "disableMem()")
		if (_isEnableMem)
		{
			MemStatService.unplug(this)
			_isEnableMem = false
			stopIfDisabled()
		}
	}

	private fun enableNet()
	{
		Log.d(LOG_TAG, "enableNet()")
		if (!_isEnableNet)
		{
			_isEnableNet = true
			NetStatService.plug(this)
		}
	}

	private fun disableNet()
	{
		Log.d(LOG_TAG, "disableNet()")
		if (_isEnableNet)
		{
			NetStatService.unplug(this)
			_isEnableNet = false
			stopIfDisabled()
		}
	}

	private fun stopIfDisabled()
	{
		if (!_isEnableCpu && !_isEnableMem && !_isEnableNet)
		{
			Log.d("$LOG_TAG.stopIfDisabled", "Stopping service...")
			stopSelf()
		}
	}

	private fun buildLoadingNotif(): Notification
	{
		return NotificationCompat.Builder(this)
				.setContentTitle(getString(R.string.notif_loading))
				.setProgress(100, 0, true)
				.setSmallIcon(R.drawable.ic_sync_white_24dp)
				.setOnlyAlertOnce(true)
				.setPriority(_priority)
				.setWhen(_when)
				.setShowWhen(false)
				.setLocalOnly(true)
				.build()
	}

	private val _cpuStatAvailableReceiver = object: BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			val stat = intent.getParcelableExtra<CpuStat>(Res.EXTRA_STAT)
			onStatAvailable(stat)
		}
	}

	private val _memStatAvailableReceiver = object: BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			val stat = intent.getParcelableExtra<MemStat>(Res.EXTRA_STAT)
			onStatAvailable(stat)
		}
	}

	private val _netStatAvailableReceiver = object: BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			val stat = intent.getParcelableExtra<NetStat>(Res.EXTRA_STAT)
			onStatAvailable(stat)
		}
	}

	private val _priority: Int
		get()
		{
			return if (_pref.isHighPriority) NotificationCompat.PRIORITY_HIGH
					else NotificationCompat.PRIORITY_LOW
		}

	private val _broadcastManager by lazy{ LocalBroadcastManager.getInstance(this)}
	private val _notifManager by lazy{getSystemService(
			Context.NOTIFICATION_SERVICE) as NotificationManager}
	private val _pref by lazy{Preference.from(this)}
	private var _notifCount = 0
	// To order the notifications
	private val _when by lazy{System.currentTimeMillis()}
	private var _hasInit = false

	private var _isEnableCpu = false
	private var _cpuStat: CpuStat? = null
	private val _cpuNotifBuilder by lazy(
	{
		val product = CpuNotifBuilder(this)
		product.isOverall = _pref.isOverallCpu
		product.priority = _priority
		product
	})

	private var _isEnableMem = false
	private var _memStat: MemStat? = null
	private val _memNotifBuilder by lazy(
	{
		val product = MemNotifBuilder(this)
		product.priority = _priority
		product
	})

	private var _isEnableNet = false
	private var _netStat: NetStat? = null
	private val _netNotifBuilder by lazy(
	{
		val product = NetNotifBuilder(this)
		product.priority = _priority
		product
	})
}
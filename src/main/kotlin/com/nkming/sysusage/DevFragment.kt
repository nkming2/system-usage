package com.nkming.sysusage

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.nkming.utils.app.FragmentEx

class DevFragment : FragmentEx()
{
	companion object
	{
		fun create(): DevFragment
		{
			return DevFragment()
		}

		private val LOG_TAG = DevFragment::class.java.canonicalName
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		return inflater.inflate(R.layout.dev_frag, container, false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		_broadcastManager.registerReceiver(_statAvailableReceiver,
				IntentFilter(Res.ACTION_CPU_STAT_AVAILABLE))
	}

	override fun onStart()
	{
		super.onStart()
		CpuStatService.plug(context!!)
	}

	override fun onStop()
	{
		super.onStop()
		CpuStatService.unplug(context!!)
	}

	private fun onStatAvailable(stat: CpuStat)
	{
		_count.text = "${stat.cores.size}"

		val statusBuilder = StringBuilder()
		val usageBuilder = StringBuilder()
		for (c in stat.cores)
		{
			statusBuilder.append(if (c.isOnline) "O" else "X")
			usageBuilder.append("${(c.usage * 100).toInt()}% ")
		}
		_status.text = statusBuilder.toString()
		_usage.text = usageBuilder.toString()
	}

	private val _statAvailableReceiver = object: BroadcastReceiver()
	{
		override fun onReceive(context: Context, intent: Intent)
		{
			val stat = intent.getParcelableExtra<CpuStat>(Res.EXTRA_STAT)
			onStatAvailable(stat)
		}
	}

	private val _broadcastManager by lazy{
			LocalBroadcastManager.getInstance(context!!)}

	private val _count by lazyView<TextView>(R.id.count)
	private val _status by lazyView<TextView>(R.id.status)
	private val _usage by lazyView<TextView>(R.id.usage)
}

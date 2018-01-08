package com.nkming.sysusage

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.content.ContextCompat
import com.nkming.utils.Log
import java.util.*

class MemNotifBuilder(context: Context, channelId: String)
{
	companion object
	{
		private val LOG_TAG = MemNotifBuilder::class.java.canonicalName
	}

	var priority = NotificationCompat.PRIORITY_LOW

	fun build(stat: MemStat, when_: Long = System.currentTimeMillis())
			: Notification
	{
		return buildNotif(stat.avail, stat.total, when_)
	}

	private fun buildNotif(avail: Long, total: Long, when_: Long): Notification
	{
		val usage = 1.0 - (avail / total.toDouble())
		val level = (usage * 100.0).toInt()
		if (level < 0 || level > 100)
		{
			Log.e("$LOG_TAG.buildNotif", "Erratic level: $level")
		}
		val level_ = Math.min(Math.max(0, level), 100)

		val usageMb = (total - avail) / 1024 / 1024
		val availMb = avail / 1024 / 1024

		val iconId = _context.resources.getIdentifier(
				"ic_mem_%d_white_24dp".format(Locale.US, level_), "drawable",
				BuildConfig.APPLICATION_ID)
		if (iconId == 0)
		{
			throw RuntimeException(
					"Icon not found: ic_mem_%d_white_24dp".format(Locale.US,
							level_))
		}

		val builder = NotificationCompat.Builder(_context, _channelId)
				.setContentTitle(_context.getString(R.string.mem_notif_title,
						level_))
				.setContentText(_context.getString(R.string.mem_notif_content,
						usageMb, availMb))
				.setProgress(100, level, false)
				.setSmallIcon(iconId)
				.setContentIntent(getOnClickIntent())
				.setOnlyAlertOnce(true)
				.setWhen(when_)
				.setShowWhen(false)
				.setOngoing(true)
				.setLocalOnly(true)
				.setColor(ContextCompat.getColor(_context, R.color.notif))
				.setGroup(when_.toString())
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O)
		{
			builder.priority = priority
		}
		return builder.build()
	}

	private fun getOnClickIntent(): PendingIntent
	{
		val i = Intent(_context, PreferenceActivity::class.java)
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		return PendingIntent.getActivity(_context, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT)
	}

	private val _context = context
	private val _channelId = channelId
}

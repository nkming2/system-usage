package com.nkming.sysusage

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nkming.utils.Log
import java.util.*

class CpuNotifBuilder(context: Context, channelId: String)
{
	companion object
	{
		private val LOG_TAG = CpuNotifBuilder::class.java.canonicalName
	}

	var isOverall = true
	var priority = NotificationCompat.PRIORITY_LOW

	fun build(stat: CpuStat, when_: Long = System.currentTimeMillis())
			: List<Notification>
	{
		return if (!stat.isGood)
		{
			listOf(buildError(when_))
		}
		else if (isOverall)
		{
			listOf(buildOverall(stat, when_))
		}
		else
		{
			buildIndividual(stat, when_)
		}
	}

	private fun buildOverall(stat: CpuStat, when_: Long): Notification
	{
		var overallLevel = .0
		var onlineCount = 0
		for (c in stat.cores)
		{
			val level = c.usage * 100
			overallLevel += level / stat.cores.size
			if (c.isOnline)
			{
				++onlineCount
			}
		}
		return buildOverallNotif(overallLevel.toInt(), onlineCount,
				stat.cores.size, when_)
	}

	private fun buildOverallNotif(level: Int, onlineCoreCount: Int,
			totalCoreCount: Int, when_: Long): Notification
	{
		if (level < 0 || level > 100)
		{
			Log.e("$LOG_TAG.buildOverallNotif", "Erratic level: $level")
		}
		val level_ = Math.min(Math.max(0, level), 100)

		val iconId = _context.resources.getIdentifier(
				"ic_cpu_%d_white_24dp".format(Locale.US, level_), "drawable",
				BuildConfig.APPLICATION_ID)
		if (iconId == 0)
		{
			throw RuntimeException(
					"Icon not found: ic_cpu_%d_white_24dp".format(Locale.US,
							level_))
		}

		val builder = getNotifBuilder(when_)
				.setContentTitle(_context.getString(
						R.string.cpu_notif_title_overall, level_))
				.setProgress(100, level, false)
				.setSmallIcon(iconId)
		if (totalCoreCount > 1)
		{
			builder.setContentText(_context.getString(
					R.string.cpu_notif_content_overall, onlineCoreCount,
					totalCoreCount))
		}
		return builder.build()
	}

	private fun buildIndividual(stat: CpuStat, when_: Long): List<Notification>
	{
		val product = ArrayList<Notification>(4)
		for ((i, c) in stat.cores.withIndex())
		{
			val level = (c.usage * 100).toInt()
			product += buildIndividualNotif(i, c.isOnline, level, when_ - i)
		}
		return product
	}

	private fun buildIndividualNotif(coreId: Int, isOnline: Boolean,
			level: Int, when_: Long) : Notification
	{
		return if (isOnline)
		{
			if (level < 0 || level > 100)
			{
				Log.e("$LOG_TAG.buildIndividualNotif", "Erratic level: $level")
			}
			val level_ = Math.min(Math.max(0, level), 100)

			val iconId = _context.resources.getIdentifier(
					"ic_cpu_%d_white_24dp".format(Locale.US, level_), "drawable",
					BuildConfig.APPLICATION_ID)
			if (iconId == 0)
			{
				throw RuntimeException(
						"Icon not found: ic_cpu_%d_white_24dp".format(Locale.US,
								level_))
			}
			val title = _context.getString(R.string.cpu_notif_title, coreId + 1,
					level_)

			getNotifBuilder(when_)
					.setContentTitle(title)
					.setProgress(100, level, false)
					.setSmallIcon(iconId)
					.build()
		}
		else
		{
			val title = _context.getString(R.string.cpu_notif_title_disabled,
					coreId + 1)
			getNotifBuilder(when_)
					.setContentTitle(title)
					.setSmallIcon(R.drawable.ic_cpu_disabled_white_24dp)
					.build()
		}
	}

	private fun buildError(when_: Long): Notification
	{
		return getNotifBuilder(when_)
				.setContentTitle(_context.getString(R.string.cpu_notif_title_error))
				.setSmallIcon(R.drawable.ic_cpu_disabled_white_24dp)
				.build()
	}

	private fun getOnClickIntent(): PendingIntent
	{
		val i = Intent(_context, PreferenceActivity::class.java)
		i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK
				or Intent.FLAG_ACTIVITY_CLEAR_TASK)
		return PendingIntent.getActivity(_context, 0, i,
				PendingIntent.FLAG_UPDATE_CURRENT)
	}

	private fun getNotifBuilder(when_: Long): NotificationCompat.Builder
	{
		val product = NotificationCompat.Builder(_context, _channelId)
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
			product.priority = priority
		}
		return product as NotificationCompat.Builder
	}

	private val _context = context
	private val _channelId = channelId
}

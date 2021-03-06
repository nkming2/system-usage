package com.nkming.sysusage

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import java.util.*

class DiskNotifBuilder(context: Context, channelId: String)
{
	var priority = NotificationCompat.PRIORITY_LOW

	fun build(stat: DiskStat, when_: Long = System.currentTimeMillis())
			: List<Notification>
	{
		return if (!stat.isGood)
		{
			buildError(when_)
		}
		else
		{
			val readNotif = buildReadNotif(stat, when_)
			val writeNotif = buildWriteNotif(stat, when_)
			listOf(readNotif, writeNotif)
		}
	}

	fun buildReadNotif(stat: DiskStat, when_: Long): Notification
	{
		val level = Math.min(Math.max(0, (stat.readUsage * 100.0).toInt()),
				100)
		val iconId = _context.resources.getIdentifier(
				"ic_disk_read_%d_white_24dp".format(Locale.US, level), "drawable",
				BuildConfig.APPLICATION_ID)
		if (iconId == 0)
		{
			throw RuntimeException(
					"Icon not found: ic_disk_read_%d_white_24dp".format(
							Locale.US, level))
		}
		return getNotifBuilder(when_)
				.setContentTitle(_context.getString(
						R.string.disk_read_notif_title, level))
				.setContentText(DiskUtils.getReadableByteSpeed(stat.readBps))
				.setProgress(100, level, false)
				.setSmallIcon(iconId)
				.build()
	}

	fun buildWriteNotif(stat: DiskStat, when_: Long): Notification
	{
		val level = Math.min(Math.max(0, (stat.writeUsage * 100.0).toInt()),
				100)
		val iconId = _context.resources.getIdentifier(
				"ic_disk_write_%d_white_24dp".format(Locale.US, level),
				"drawable", BuildConfig.APPLICATION_ID)
		if (iconId == 0)
		{
			throw RuntimeException(
					"Icon not found: ic_disk_write_%d_white_24dp".format(
							Locale.US, level))
		}
		return getNotifBuilder(when_ - 1)
				.setContentTitle(_context.getString(
						R.string.disk_write_notif_title, level))
				.setContentText(DiskUtils.getReadableByteSpeed(stat.writeBps))
				.setProgress(100, level, false)
				.setSmallIcon(iconId)
				.build()
	}

	private fun buildError(when_: Long): List<Notification>
	{
		val r = getNotifBuilder(when_)
				.setContentTitle(_context.getString(R.string.disk_notif_title_error))
				.setSmallIcon(R.drawable.ic_disk_read_disabled_white_24dp)
				.build()
		val w = getNotifBuilder(when_ - 1)
				.setContentTitle(_context.getString(R.string.disk_notif_title_error))
				.setSmallIcon(R.drawable.ic_disk_write_disabled_white_24dp)
				.build()
		return listOf(r, w)
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

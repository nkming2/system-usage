package com.nkming.sysusage

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.nkming.utils.Log

class StorageNotifBuilder(context: Context, channelId: String)
{
	companion object
	{
		private val LOG_TAG = StorageNotifBuilder::class.java.canonicalName
	}

	var priority = NotificationCompat.PRIORITY_LOW

	fun build(stat: StorageStat, when_: Long = System.currentTimeMillis())
			: List<Notification>
	{
		return if (!stat.isGood)
		{
			listOf(buildError(when_))
		}
		else
		{
			val products = mutableListOf<Notification>()
			for ((i, ds) in stat.dirs.withIndex())
			{
				products += buildNotif(ds.used, ds.available, ds.size, when_ - i, (i != 0))
			}
			products
		}
	}

	private fun buildNotif(used: Long, avail: Long, total: Long, when_: Long,
			isExternal: Boolean)
			: Notification
	{
		val usage = used / total.toDouble()
		val level = (usage * 100.0).toInt()
		if (level < 0 || level > 100)
		{
			Log.e("$LOG_TAG.buildNotif", "Erratic level: $level")
		}
		val level_ = Math.min(Math.max(0, level), 100)

		val usageGb = used / 1024f / 1024f / 1024f
		val availGb = avail / 1024f / 1024f / 1024f

		val iconId = _context.resources.getIdentifier("ic_storage_${level_}_white_24dp",
				"drawable", BuildConfig.APPLICATION_ID)
		if (iconId == 0)
		{
			throw RuntimeException("Icon not found: ic_storage_${level_}_white_24dp")
		}

		val builder = getNotifBuilder(when_)
				.setContentText(_context.getString(R.string.storage_notif_content,
						usageGb, availGb))
				.setProgress(100, level, false)
				.setSmallIcon(iconId)
		if (isExternal)
		{
			builder.setContentTitle(_context.getString(R.string.storage_notif_ext_title,
					level_))
		}
		else
		{
			builder.setContentTitle(_context.getString(R.string.storage_notif_title,
					level_))
		}
		return builder.build()
	}

	private fun buildError(when_: Long): Notification
	{
		return getNotifBuilder(when_)
				.setContentTitle(_context.getString(R.string.storage_notif_title_error))
				.setSmallIcon(R.drawable.ic_storage_disabled_white_24dp)
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

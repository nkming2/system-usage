package com.nkming.sysusage

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.support.v4.app.NotificationCompat
import java.util.*

class NetNotifBuilder(context: Context)
{
	companion object
	{
		private val LOG_TAG = NetNotifBuilder::class.java.canonicalName
	}

	var priority = NotificationCompat.PRIORITY_LOW

	fun build(stat: NetStat, when_: Long = System.currentTimeMillis())
			: List<Notification>
	{
		val rxNotif = if (!stat.isOnline)
		{
			getNotifBuilder(when_)
					.setContentTitle(_context.getString(
							R.string.net_notif_title_disabled))
					.setSmallIcon(R.drawable.ic_net_dl_disabled_white_24dp)
					.build()
		}
		else
		{
			val level = Math.min(Math.max(0, (stat.rxUsage * 100.0).toInt()),
					100)
			val iconId = _context.resources.getIdentifier(
					"ic_net_dl_%d_white_24dp".format(Locale.US, level),
					"drawable", BuildConfig.APPLICATION_ID)
			if (iconId == 0)
			{
				throw RuntimeException(
						"Icon not found: ic_net_dl_%d_white_24dp".format(
								Locale.US, level))
			}
			getNotifBuilder(when_)
					.setContentTitle(_context.getString(
							R.string.net_rx_notif_title, level))
					.setContentText(NetUtils.getReadableByteSpeed(stat.rxBps))
					.setProgress(100, level, false)
					.setSmallIcon(iconId)
					.build()
		}

		val txNotif = if (!stat.isOnline)
		{
			getNotifBuilder(when_)
					.setContentTitle(_context.getString(
							R.string.net_notif_title_disabled))
					.setSmallIcon(R.drawable.ic_net_ul_disabled_white_24dp)
					.build()
		}
		else
		{
			val level = Math.min(Math.max(0, (stat.txUsage * 100.0).toInt()),
					100)
			val iconId = _context.resources.getIdentifier(
					"ic_net_ul_%d_white_24dp".format(Locale.US, level),
					"drawable", BuildConfig.APPLICATION_ID)
			if (iconId == 0)
			{
				throw RuntimeException(
						"Icon not found: ic_net_ul_%d_white_24dp".format(
								Locale.US, level))
			}
			getNotifBuilder(when_ - 1)
					.setContentTitle(_context.getString(
							R.string.net_tx_notif_title, level))
					.setContentText(NetUtils.getReadableByteSpeed(stat.txBps))
					.setProgress(100, level, false)
					.setSmallIcon(iconId)
					.build()
		}

		return listOf(rxNotif, txNotif)
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
		val product = NotificationCompat.Builder(_context)
				.setContentIntent(getOnClickIntent())
				.setOnlyAlertOnce(true)
				.setPriority(priority)
				.setWhen(when_)
				.setShowWhen(false)
				.setOngoing(true)
				.setLocalOnly(true)
				.setColor(ContextCompat.getColor(_context, R.color.notif))
				.setGroup(when_.toString())
		return product as NotificationCompat.Builder
	}

	private val _context = context
}

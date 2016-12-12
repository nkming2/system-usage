package com.nkming.sysusage

import android.app.ActivityManager
import android.content.Context
import android.os.Parcel
import android.os.Parcelable

data class MemStat(
	val avail: Long,
	val total: Long,
	val isLow: Boolean)
	: Parcelable
{
	companion object
	{
		@JvmField
		val CREATOR = object: Parcelable.Creator<MemStat>
		{
			override fun createFromParcel(source: Parcel): MemStat
			{
				return MemStat(source.readLong(),
						source.readLong(),
						(source.readInt() != 0))
			}

			override fun newArray(size: Int): Array<MemStat?>
			{
				return arrayOfNulls(size)
			}
		}
	}

	override fun describeContents() = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeLong(avail)
		dest.writeLong(total)
		dest.writeInt(if (isLow) 1 else 0)
	}
}

class MemoryStatProvider(context: Context,
		onStatUpdate: ((stat: MemStat) -> Unit)? = null,
		onFailure: (() -> Unit)? = null)
		: BaseStatProvider()
{
	var onStatUpdate = onStatUpdate
	var onFailure = onFailure

	protected override fun onUpdate()
	{
		val info = ActivityManager.MemoryInfo()
		_activityManager.getMemoryInfo(info)
		val stat = MemStat(info.availMem, info.totalMem, info.lowMemory)
		onStatUpdate?.invoke(stat)
	}

	private val _context: Context = context
	private val _activityManager by lazy{_context.getSystemService(
			Context.ACTIVITY_SERVICE) as ActivityManager}
}

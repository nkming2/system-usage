package com.nkming.sysusage

import android.content.Context
import android.os.Parcel
import android.os.Parcelable

data class DiskStat(
	// B for Byte
	val readBps: Long,
	val readUsage: Double,
	val writeBps: Long,
	val writeUsage: Double)
	: Parcelable
{
	companion object
	{
		@JvmField
		val CREATOR = object: Parcelable.Creator<DiskStat>
		{
			override fun createFromParcel(source: Parcel): DiskStat
			{
				return DiskStat(source.readLong(),
						source.readDouble(),
						source.readLong(),
						source.readDouble())
			}

			override fun newArray(size: Int): Array<DiskStat?>
			{
				return arrayOfNulls(size)
			}
		}
	}

	override fun describeContents() = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeLong(readBps)
		dest.writeDouble(readUsage)
		dest.writeLong(writeBps)
		dest.writeDouble(writeUsage)
	}
}

class DiskStatProvider(context: Context,
		onStatUpdate: ((stat: DiskStat) -> Unit)? = null,
		onFailure: ((msg: String) -> Unit)? = null)
		: BaseStatProvider()
{
	companion object
	{
		private val LOG_TAG = DiskStatProvider::class.java.canonicalName

		private val UPDATE_SCRIPT = listOf("cat /proc/diskstats")
	}

	var onStatUpdate = onStatUpdate
	var onFailure = onFailure

	// Byte/s
	var readThroughput = 0L
	var writeThroughput = 0L

	protected override fun onUpdate()
	{
		ShHelper.doShCommand(_context, UPDATE_SCRIPT,
				successWhere = {exitCode, output -> exitCode == 0},
				onSuccess = {exitCode, output -> onCommandOutput(output)},
				onFailure = {exitCode, output -> onFailure?.invoke(
						output.joinToString("\n"))})
	}

	protected override fun onStop()
	{
		_isReady = false
	}

	private data class RwStat(
		val completed: Long,
		val merged: Long,
		val sectors: Long,
		val time: Long)

	private data class Stat(
		val majorNumber: Int,
		val minorNumber: Int,
		val dev: String,
		val read: RwStat,
		val write: RwStat,
		val ioInProgress: Long,
		val ioTime: Long,
		val ioTimeWeighted: Long)

	private data class MutableRwStat(
		var completed: Long = 0L,
		var merged: Long = 0L,
		var sectors: Long = 0L,
		var time: Long = 0L)
	{
		fun immutable(): RwStat
		{
			return RwStat(completed, merged, sectors, time)
		}
	}

	private data class MutableStat(
		var majorNumber: Int = 0,
		var minorNumber: Int = 0,
		var dev: String = "",
		var read: MutableRwStat = MutableRwStat(),
		var write: MutableRwStat = MutableRwStat(),
		var ioInProgress: Long = 0L,
		var ioTime: Long = 0L,
		var ioTimeWeighted: Long = 0L)
	{
		fun immutable(): Stat
		{
			return Stat(majorNumber, minorNumber, dev, read.immutable(),
					write.immutable(), ioInProgress, ioTime, ioTimeWeighted)
		}
	}

	private fun onCommandOutput(output: List<String>)
	{
		val now = System.currentTimeMillis()
		val stats = parseOutput(output)
		val combined = stats.fold(MutableStat(), {product, s ->
		run{
			product.read.completed += s.read.completed
			product.read.merged += s.read.merged
			product.read.sectors += s.read.sectors
			product.read.time += s.read.time
			product.write.completed += s.write.completed
			product.write.merged += s.write.merged
			product.write.sectors += s.write.sectors
			product.write.time += s.write.time
			product.ioInProgress += s.ioInProgress
			product.ioTime += s.ioTime
			product.ioTimeWeighted += s.ioTimeWeighted
			product
		}})
		try
		{
			if (_isReady)
			{
				val diskStat = transformStat(now,  combined.immutable())
				onStatUpdate?.invoke(diskStat)
			}
		}
		finally
		{
			_prevTime = now
			_prevStat = combined.immutable()
			_isReady = true
		}
	}

	private fun transformStat(now: Long, combined: Stat): DiskStat
	{
		val delta = Stat(0, 0, "combined",
				RwStat(combined.read.completed - _prevStat!!.read.completed,
						combined.read.merged - _prevStat!!.read.merged,
						combined.read.sectors - _prevStat!!.read.sectors,
						combined.read.time - _prevStat!!.read.time),
				RwStat(combined.write.completed - _prevStat!!.write.completed,
						combined.write.merged - _prevStat!!.write.merged,
						combined.write.sectors - _prevStat!!.write.sectors,
						combined.write.time - _prevStat!!.write.time),
				combined.ioInProgress - _prevStat!!.ioInProgress,
				combined.ioTime - _prevStat!!.ioTime,
				combined.ioTimeWeighted - _prevStat!!.ioTimeWeighted)
		// Sector size is hard-coded as 512 bytes in the kernel
		// See: https://lkml.org/lkml/2015/8/17/269
		val readTotal = delta.read.sectors * 512
		val writeTotal = delta.write.sectors * 512
		val divisor = (now - _prevTime) / 1000.0
		val readBps = readTotal / divisor
		val writeBps = writeTotal / divisor
		val readUsage = if (readThroughput > 0) readBps / readThroughput
				else 0.0
		val writeUsage = if (writeThroughput > 0) writeBps / writeThroughput
				else 0.0
		val stat = DiskStat(Math.round(readBps), readUsage, Math.round(writeBps),
				writeUsage)
		return stat
	}

	private fun parseOutput(output: List<String>): List<Stat>
	{
		// Ref: https://www.kernel.org/doc/Documentation/ABI/testing/procfs-diskstats
		val statStrs = output.map{it.split(' ').filter{it.isNotEmpty()}}
		return statStrs.map(
		{
			Stat(it[0].toInt(),
					it[1].toInt(),
					it[2],
					RwStat(it[3].toLong(),
							it[4].toLong(),
							it[5].toLong(),
							it[6].toLong()),
					RwStat(it[7].toLong(),
							it[8].toLong(),
							it[9].toLong(),
							it[10].toLong()),
					it[11].toLong(),
					it[12].toLong(),
					it[13].toLong())
		})
	}

	private val _context: Context = context
	private var _isReady = false
	private var _prevTime = 0L
	private var _prevStat: Stat? = null
}

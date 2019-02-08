package com.nkming.sysusage

import android.content.Context
import android.os.*
import androidx.core.content.ContextCompat
import com.nkming.utils.Log
import java.io.File

data class StorageDirStat(
	val size: Long,
	val used: Long,
	val available: Long)
	: Parcelable
{
	companion object
	{
		@JvmField
		val CREATOR = object: Parcelable.Creator<StorageDirStat>
		{
			override fun createFromParcel(source: Parcel): StorageDirStat
			{
				return StorageDirStat(source.readLong(),
						source.readLong(),
						source.readLong())
			}

			override fun newArray(size: Int): Array<StorageDirStat?>
			{
				return arrayOfNulls(size)
			}
		}
	}

	override fun describeContents() = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeLong(size)
		dest.writeLong(used)
		dest.writeLong(available)
	}
}

data class StorageStat(
	val dirs: List<StorageDirStat>)
	: Parcelable
{
	companion object
	{
		@JvmField
		val CREATOR = object: Parcelable.Creator<StorageStat>
		{
			override fun createFromParcel(source: Parcel): StorageStat
			{
				val dirs = ArrayList<StorageDirStat>()
				source.readTypedList(dirs, StorageDirStat.CREATOR)
				return StorageStat(dirs)
			}

			override fun newArray(size: Int): Array<StorageStat?>
			{
				return arrayOfNulls(size)
			}
		}
	}

	override fun describeContents() = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeTypedList(dirs)
	}
}

class StorageStatProvider(context: Context,
		 onStatUpdate: ((stat: StorageStat) -> Unit)? = null,
		 onFailure: ((msg: String) -> Unit)? = null)
		: BaseStatProvider()
{
	companion object
	{
		private val LOG_TAG = StorageStatProvider::class.java.canonicalName

		private val UPDATE_SCRIPT = listOf("df")
	}

	var onStatUpdate = onStatUpdate
	var onFailure = onFailure

	protected override fun onUpdate()
	{
		ShHelper.doShCommand(_context, UPDATE_SCRIPT,
				// Why is it returning 1?
				successWhere = {exitCode, output -> ((exitCode == 0 || exitCode == 1)
						&& output.size > 1)},
				onSuccess = {exitCode, output -> onCommandOutput(output)},
				onFailure = {exitCode, output -> onFailure?.invoke(
						output.joinToString("\n"))})
	}

	private class Parser
	{
		data class Result(
			val mountpoints: List<String>,
			val size: Long,
			val used: Long,
			val available: Long
		)

		operator fun invoke(line: String): Result?
		{
			return if (!_hasParsesTitle)
			{
				parseTitle(line)
				_hasParsesTitle = true
				null
			}
			else
			{
				parseContent(line)
			}
		}

		private fun parseTitle(line: String)
		{
			// Parse the 1st line to see what we should expect
			val labels = line.split(' ').filter{it.isNotEmpty()}
			_sizeI = labels.indexOfFirst{it.equals("1K-blocks", ignoreCase = true)
					|| it.equals("Size", ignoreCase = true)}
			if (_sizeI == -1)
			{
				throw IllegalStateException("Failed locating size")
			}
			_usedI = labels.indexOfFirst{it.equals("Used", ignoreCase = true)}
			if (_usedI == -1)
			{
				throw IllegalStateException("Failed locating used")
			}
			_availableI = labels.indexOfFirst{it.equals("Free", ignoreCase = true)
					|| it.equals("Available", ignoreCase = true)}
			if (_availableI == -1)
			{
				throw IllegalStateException("Failed locating available")
			}
		}

		private fun parseContent(line: String): Result
		{
			val columns = line.split(' ').filter{it.isNotEmpty()}
			val mountpoints = mutableListOf<String>()
			var size = 0L
			var used = 0L
			var available = 0L
			for ((i, c) in columns.withIndex())
			{
				if (i == _sizeI)
				{
					size = parseSizeString(c)
				}
				else if (i == _usedI)
				{
					used = parseSizeString(c)
				}
				else if (i == _availableI)
				{
					available = parseSizeString(c)
				}
				else if (!c[0].isDigit())
				{
					mountpoints += c
				}
			}
			return Result(mountpoints, size, used, available)
		}

		private fun parseSizeString(size: String): Long
		{
			return if (size.all{it.isDigit()})
			{
				// All digits, probably from toybox which returns in n 1K-blocks
				size.toLong() * 1024
			}
			else
			{
				// Ending with G, M or K, probably from the old toolbox
				val v = size.slice(0 until size.length - 1).toDouble()
				val unit = size.last()
				when (unit)
				{
					'G' -> (v * 1024 * 1024 * 1024).toLong()
					'M' -> (v * 1024 * 1024).toLong()
					'K' -> (v * 1024).toLong()
					else -> throw IllegalStateException("Unknown unit: $unit")
				}
			}
		}

		private var _hasParsesTitle = false
		private var _sizeI = 0
		private var _usedI = 0
		private var _availableI = 0
	}

	private fun onCommandOutput(output: List<String>)
	{
		try
		{
			val parser = Parser()
			val stats = output.mapNotNull{parser(it)}
			val dataStat = stats.find{it.mountpoints.any{it == "/data"}}!!

			val products = mutableListOf<StorageDirStat>()
			// internal storage
			products += StorageDirStat(size = dataStat.size,
					used = dataStat.used, available = dataStat.available)
			products += queryExternalStorages()
			onStatUpdate?.invoke(StorageStat(products))
		}
		catch (e: Exception)
		{
			Log.w("$LOG_TAG.onCommandOutput",
					"Failed while parsing output:\n${output.joinToString("\n")}", e)
			// Should we return something else?
			onStatUpdate?.invoke(StorageStat(listOf(StorageDirStat(0, 0, 0))))
		}
	}

	private fun queryExternalStorages(): List<StorageDirStat>
	{
		val products = mutableListOf<StorageDirStat>()
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			for (dir in ContextCompat.getExternalFilesDirs(_context, null))
			{
				if (Environment.getExternalStorageState(dir) == Environment.MEDIA_MOUNTED
						&& !Environment.isExternalStorageEmulated(dir))
				{
					// Mounted and not emulated, probably a real SD card
					products += queryExternalStorage(dir)
				}
			}
		}
		else
		{
			if (Environment.getExternalStorageState() == Environment.MEDIA_MOUNTED
					&& !Environment.isExternalStorageEmulated())
			{
				products += queryExternalStorage(Environment.getExternalStorageDirectory())
			}
		}
		return products
	}

	private fun queryExternalStorage(dir: File): StorageDirStat
	{
		val sf = StatFs(dir.absolutePath)
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2)
		{
			val size = sf.blockCountLong * sf.blockSizeLong
			val available = sf.availableBlocksLong * sf.blockSizeLong
			StorageDirStat(size, size - available, available)
		}
		else
		{
			val size = sf.blockCount * sf.blockSize
			val available = sf.availableBlocks * sf.blockSize
			StorageDirStat(size.toLong(), (size - available).toLong(), available.toLong())
		}
	}

	private val _context: Context = context
}

package com.nkming.sysusage

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.nkming.utils.Log
import com.nkming.utils.type.ext.sumByLong
import java.util.*

data class CpuCoreStat(
	val isOnline: Boolean,
	val usage: Double,
	val normalizedUsage: Double)
	: Parcelable
{
	companion object
	{
		@JvmField
		val CREATOR = object: Parcelable.Creator<CpuCoreStat>
		{
			override fun createFromParcel(source: Parcel): CpuCoreStat
			{
				return CpuCoreStat((source.readInt() != 0),
						source.readDouble(),
						source.readDouble())
			}

			override fun newArray(size: Int): Array<CpuCoreStat?>
			{
				return arrayOfNulls(size)
			}
		}
	}

	override fun describeContents() = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeInt(if (isOnline) 1 else 0)
		dest.writeDouble(usage)
		dest.writeDouble(normalizedUsage)
	}
}

data class MutableCpuCoreStat(
	var isOnline: Boolean,
	var usage: Double,
	var normalizedUsage: Double)
{
	fun immutable(): CpuCoreStat
	{
		return CpuCoreStat(isOnline, usage, normalizedUsage)
	}
}

data class CpuStat(
	val cores: List<CpuCoreStat>)
	: Parcelable
{
	companion object
	{
		@JvmField
		val CREATOR = object: Parcelable.Creator<CpuStat>
		{
			override fun createFromParcel(source: Parcel): CpuStat
			{
				val cores = ArrayList<CpuCoreStat>()
				source.readTypedList(cores, CpuCoreStat.CREATOR)
				return CpuStat(cores)
			}

			override fun newArray(size: Int): Array<CpuStat?>
			{
				return arrayOfNulls(size)
			}
		}
	}

	override fun describeContents() = 0

	override fun writeToParcel(dest: Parcel, flags: Int)
	{
		dest.writeTypedList(cores)
	}
}

class CpuStatProvider(context: Context,
		onStatUpdate: ((stat: CpuStat) -> Unit)? = null,
		onFailure: ((msg: String) -> Unit)? = null)
		: BaseStatProvider()
{
	companion object
	{
		private val LOG_TAG = CpuStatProvider::class.java.canonicalName

		private val UPDATE_SCRIPT = listOf(
				"update()",
				"{",
				"	cat /sys/devices/system/cpu/online || return 1",
				"	present=$(cat /sys/devices/system/cpu/present || return 1)",
				"	if [ \"\$?\" == \"1\" ]; then",
				"		return 1",
				"	else",
				"		echo \$present",
				"	fi",
				"	i=0",
				"	while [ \$i -le \${present##*-} ]; do",
				// Could fail if core is off (hotplug)
				"		cat /sys/devices/system/cpu/cpu\$i/cpufreq/stats/time_in_state || echo -1",
				"		echo \":)\"",
				"		i=\$((i+1))",
				"	done",
				"	cat /proc/stat || return 1",
				"}",
				"update",
				"echo \":)\"")
	}

	var onStatUpdate = onStatUpdate
	var onFailure = onFailure

	protected override fun onUpdate()
	{
		ShHelper.doShCommand(_context, UPDATE_SCRIPT,
				successWhere = {exitCode, output -> (exitCode == 0
						&& output.size > 2)},
				onSuccess = {_, output -> onCommandOutput(output)},
				onFailure = {_, output -> onFailure?.invoke(output.joinToString(
						"\n"))})
	}

	protected override fun onStop()
	{
		_isReady = false
	}

	private fun onCommandOutput(output: List<String>)
	{
		try
		{
			val online = output[0]
			val present = output[1]
			val count = parseCoreCountOutput(present)
			val timeStates = ArrayList<List<String>>(count)
			var readLine = 2
			for (i in 0 until count)
			{
				val til = output.withIndex().indexOfFirst{
						it.index >= readLine && it.value == ":)"}
				if (til == -1)
				{
					throw IllegalArgumentException(
							"Unknown output: ${output.joinToString("\n")}")
				}
				timeStates += output.subList(readLine, til)
				readLine = til + 1
			}
			val stats = output.slice(readLine until output.size)
			val stat = parseCommandOutput(online, present, timeStates, stats)
			if (_isReady)
			{
				onStatUpdate?.invoke(stat)
			}
			else
			{
				_isReady = true
			}
		}
		catch (e: Exception)
		{
			Log.w("$LOG_TAG.onCommandOutput", "Failed while parsing output", e)
			// Should we return something else?
			onStatUpdate?.invoke(CpuStat(listOf(CpuCoreStat(true, .0, .0))))
		}
	}

	private fun parseCommandOutput(online: String, present: String,
			timeStates: List<List<String>>, stats: List<String>): CpuStat
	{
		val count = parseCoreCountOutput(present)
		val isOnlines = parseCoreOnlineOutput(count, online)
		val usages = parseCoreStatOutput(count, stats)
		val normalizedUsages = normalizeCpuUsages(usages, timeStates)

		val cores = ArrayList<CpuCoreStat>(count)
		for (i in 0 until count)
		{
			cores += CpuCoreStat(isOnlines[i], usages[i], normalizedUsages[i])
		}
		return CpuStat(cores)
	}

	private fun parseCoreCountOutput(present: String): Int
	{
		return present.split("-").last().toInt() + 1
	}

	private fun parseCoreOnlineOutput(count: Int, online: String): List<Boolean>
	{
		val products = BooleanArray(count, {false})
		// eg, ["0-1", "4-5"]
		val onlineRanges = online.split(",")
		for (or in onlineRanges)
		{
			val or_ = or.split("-")
			if (or_.size == 1)
			{
				products[or_[0].toInt()] = true
			}
			else
			{
				for (i in or_[0].toInt()..or_[1].toInt())
				{
					products[i] = true
				}
			}
		}
		return products.toList()
	}

	private fun parseCoreStatOutput(count: Int, stats: List<String>)
			: List<Double>
	{
		// See: https://linux.die.net/man/5/proc
		if (_prevStats == null)
		{
			_prevStats = Array(count, {LongArray(10, {0L})})
			_prevTimeStates = Array(count, {null})
		}
		val products = DoubleArray(count, {.0})
		for (s in stats)
		{
			val statAry = s.split(" ").filter{it.isNotEmpty()}
			if (!statAry[0].startsWith("cpu") || statAry[0] == "cpu")
			{
				continue
			}
			if (statAry.size < 5)
			{
				// At least we could catch it in console if we throw instead of
				// just logging
				throw IllegalArgumentException("Unknown /proc/stat output: $s")
			}
			val core = statAry[0].substring(3).toInt()

			// Somehow /proc/stat could return a smaller number which is wrong
			// See: http://stackoverflow.com/questions/27627213/proc-stat-idle-time-decreasing
			val diffs = LongArray(statAry.size - 1, {0L})
			val thisCoreStats = LongArray(10, {0L})
			var isErratic = false
			for ((i, stat) in statAry.subList(1, statAry.size).withIndex())
			{
				val v = stat.toLong()
				val diff = v - _prevStats!![core][i]
				if (diff >= 0)
				{
					diffs[i] = diff
					thisCoreStats[i] = v
				}
				else
				{
					// We can't really do anything when a value is wrong...
//					Log.v("$LOG_TAG.parseCoreStatOutput",
//							"Erratic /proc/stat value")
					isErratic = true
					break
				}
			}
			if (isErratic)
			{
				products[core] = _prevUsages?.get(core) ?: .0
				continue
			}
			_prevStats!![core] = thisCoreStats

			val total = diffs.sum()
			if (total == 0L)
			{
				products[core] = .0
			}
			else
			{
				val idle = (if (statAry.size - 1 > 5) diffs[3] + diffs[4]
						else diffs[3])
				products[core] = 1 - (idle.toDouble() / total)
			}
		}
		_prevUsages = products
		return products.toList()
	}

	private fun normalizeCpuUsages(usages: List<Double>,
			timeStates: List<List<String>>): List<Double>
	{
		val products = DoubleArray(usages.size, {.0})
		for (i in 0 until usages.size)
		{
			try
			{
				val usage = usages[i]
				if (timeStates[i].isEmpty() || timeStates[i][0] == "-1")
				{
					// Core is off?
					continue
				}

				val parsedTimeStates = Array(timeStates[i].size, {Pair(0L, 0L)})
				for ((j, t) in timeStates[i].withIndex())
				{
					val tLong = t.split(" ").map{it.toLong()}
					parsedTimeStates[j] = Pair(tLong[0], tLong[1])
				}
				products[i] = usage * getFreqWeighting(i, parsedTimeStates)
			}
			catch (e: NumberFormatException)
			{
				// Core is off?
				//Log.v("$LOG_TAG.normalizeCpuUsages", "Failed while toInt()", e)
			}
			catch (e: Exception)
			{
				// #1, not sure why, but at least stop it from crashing first
				Log.e("$LOG_TAG.normalizeCpuUsages",
						timeStates[i].joinToString("\n"), e)
			}
		}
		return products.toList()
	}

	private fun getFreqWeighting(core: Int,
			parsedTimeStates: Array<Pair<Long, Long>>): Double
	{
		if (_prevTimeStates!![core] == null)
		{
			_prevTimeStates!![core] = parsedTimeStates
			return .0
		}
		val diffs_ = ArrayList<Long>(parsedTimeStates.size)
		for ((prev, cur) in _prevTimeStates!![core]!!.zip(parsedTimeStates))
		{
			diffs_.add(cur.second - prev.second)
		}
		val total: Long
		val diffs: List<Long>
		if (diffs_.any{it < 0})
		{
			// Stat has been reset
			total = parsedTimeStates.sumByLong{it.second}
			diffs = parsedTimeStates.map{it.second}
		}
		else
		{
			total = diffs_.sum()
			diffs = diffs_
		}

		var factor = .0
		for ((i, d) in diffs.withIndex())
		{
			val freqRatio = (parsedTimeStates[i].first.toDouble()
					/ parsedTimeStates.last().first)
			val timeRatio = d.toDouble() / total
			factor += freqRatio * timeRatio
		}
		_prevTimeStates!![core] = parsedTimeStates
		return factor
	}

	private val _context: Context = context
	private var _isReady = false
	private var _prevStats: Array<LongArray>? = null
	private var _prevUsages: DoubleArray? = null
	private var _prevTimeStates: Array<Array<Pair<Long, Long>>?>? = null
}

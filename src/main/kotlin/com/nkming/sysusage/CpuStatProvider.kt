package com.nkming.sysusage

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import com.nkming.utils.Log
import com.nkming.utils.type.ext.sumByLong

data class CpuCoreStat(
	val isOnline: Boolean,
	val usage: Double)
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
	}
}

data class MutableCpuCoreStat(
	var isOnline: Boolean,
	var usage: Double)
{
	fun immutable(): CpuCoreStat
	{
		return CpuCoreStat(isOnline, usage)
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

		// this script won't work on O+ as /proc/stat is no longer accessible
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
				"	cat /proc/stat || echo \":(\"",
				"}",
				"update")
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
			onStatUpdate?.invoke(CpuStat(listOf(CpuCoreStat(true, .0))))
		}
	}

	private fun parseCommandOutput(online: String, present: String,
			timeInStates: List<List<String>>, stats: List<String>): CpuStat
	{
		if (!::_usageProvider.isInitialized)
		{
			_usageProvider = if (stats.firstOrNull() != ":(")
			{
				// Pre Oreo where /proc/stat was freely accessible
				CoreUsageProviderJb()
			}
			else
			{
				CoreUsageProviderO()
			}
		}

		val count = parseCoreCountOutput(present)
		val isOnlines = parseCoreOnlineOutput(count, online)
		val usages = _usageProvider.parse(count, stats, timeInStates)

		val cores = ArrayList<CpuCoreStat>(count)
		for (i in 0 until count)
		{
			cores += CpuCoreStat(isOnlines[i], usages[i])
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

	private val _context: Context = context
	private var _isReady = false
	private lateinit var _usageProvider: CoreUsageProvider
}

private interface CoreUsageProvider
{
	companion object
	{
		private val LOG_TAG = CoreUsageProvider::class.java.canonicalName
	}

	fun parse(count: Int, stats: List<String>, timeInStates: List<List<String>>)
			: List<Double>

	fun parseTimeInStatesOutput(timeInStates: List<String>)
			: Array<Pair<Long, Long>>?
	{
		/*
		 * time_in_state
		 * This gives the amount of time spent in each of the frequencies supported by
		 * this CPU. The cat output will have "<frequency> <time>" pair in each line, which
		 * will mean this CPU spent <time> usertime units of time at <frequency>. Output
		 * will have one line for each of the supported frequencies. usertime units here
		 * is 10mS (similar to other time exported in /proc).
		 * See: https://android.googlesource.com/kernel/common/+/android-3.18/Documentation/cpu-freq/cpufreq-stats.txt
		 */
		try
		{
			if (timeInStates.isEmpty() || timeInStates[0] == "-1")
			{
				// Core is off or we can't read the file
				return null
			}

			return timeInStates.map{
				val tLong = it.split(" ").map{it.toLong()}
				return@map Pair(tLong[0], tLong[1])
			}.toTypedArray()
		}
		catch (e: NumberFormatException)
		{
			Log.d("$LOG_TAG.parseTimeInStatesOutput", "Failed while toLong()", e)
			return null
		}
		catch (e: Exception)
		{
			// #1, not sure why, but at least stop it from crashing first
			Log.e("$LOG_TAG.parseTimeInStatesOutput",
					timeInStates.joinToString("\n"), e)
			return null
		}
	}
}

/**
 * Calculate core usage from /proc/stat and weight it with time_in_state
 */
private class CoreUsageProviderJb : CoreUsageProvider
{
	override fun parse(count: Int, stats: List<String>,
			timeInStates: List<List<String>>): List<Double>
	{
		if (!::_prevStats.isInitialized)
		{
			_prevStats = Array(count, {LongArray(10)})
			_prevUsages = DoubleArray(count)
			_prevTimeInStates = Array(count, {null})
		}

		val usages = parseStats(count, stats)
		return normalizeCpuUsages(usages, timeInStates)
	}

	private fun parseStats(count: Int, stats: List<String>): List<Double>
	{
		val products = DoubleArray(count)
		for (s in stats)
		{
			val statList = s.split(" ").filter{it.isNotEmpty()}
			if (statList.isEmpty() || !statList[0].startsWith("cpu")
					|| statList[0] == "cpu")
			{
				// Unrelated stuff
				continue
			}
			if (statList.size < 5)
			{
				// At least we could catch it in console if we throw instead of
				// just logging
				throw IllegalArgumentException("Unknown /proc/stat output: $s")
			}
			val core = statList[0].substring(3).toInt()

			val usage = parseSingleStat(core, statList)
			products[core] = if (usage < 0) _prevUsages[core] else usage
		}
		_prevUsages = products
		return products.toList()
	}

	/**
	 * Return core usage of a single core
	 *
	 * @param core Which core
	 * @param statList Output of /proc/stat
	 * @return Core usage, or <0 if error
	 */
	private fun parseSingleStat(core:Int, statList: List<String>): Double
	{
		val statVals = statList.subList(1, statList.size).map{it.toLong()}
				.toLongArray()
		val diffs = statVals.zip(_prevStats[core]).map{it.first - it.second}
		// Somehow /proc/stat could return a smaller number which is wrong
		// See: http://stackoverflow.com/questions/27627213/proc-stat-idle-time-decreasing
		if (diffs.any{it < 0})
		{
			return -1.0
		}
		_prevStats[core] = statVals

		val total = diffs.sum()
		return if (total == 0L)
		{
			.0
		}
		else
		{
			val idle = (if (statVals.size >= 5) diffs[3] + diffs[4]
					else diffs[3])
			1.0 - (idle.toDouble() / total)
		}
	}

	private fun normalizeCpuUsages(usages: List<Double>,
			timeInStates: List<List<String>>): List<Double>
	{
		return (0 until usages.size).map{
			val usage = usages[it]
			val timeInStatesResult = parseTimeInStatesOutput(timeInStates[it])
			return@map if (timeInStatesResult == null ) usage else
					usage * getFreqWeighting(it, timeInStatesResult)
		}
	}

	private fun getFreqWeighting(core: Int,
			parsedTimeInStates: Array<Pair<Long, Long>>): Double
	{
		if (_prevTimeInStates[core] == null)
		{
			_prevTimeInStates[core] = parsedTimeInStates
			return .0
		}

		val cpuTime_ = parsedTimeInStates.zip(_prevTimeInStates[core]!!).map{
			Pair(it.first.first, it.first.second - it.second.second)
		}
		val cpuTime = if (cpuTime_.any{it.second < 0})
		{
			// Stat has been reset
			parsedTimeInStates.toList()
		}
		else
		{
			cpuTime_
		}
		val totaCpuTime = cpuTime.sumByLong{it.second}

		val maxFreq = parsedTimeInStates.last().first
		val weights = cpuTime.map{
			val freqRatio = it.first.toDouble() / maxFreq
			val timeRatio = it.second.toDouble() / totaCpuTime
			return@map freqRatio * timeRatio
		}
		_prevTimeInStates[core] = parsedTimeInStates
		return weights.sum()
	}

	private lateinit var _prevStats: Array<LongArray>
	private lateinit var _prevUsages: DoubleArray
	private lateinit var _prevTimeInStates: Array<Array<Pair<Long, Long>>?>
}

/**
 * Calculate core usage from time_in_state alone. This is to workaround an
 * inaccessible /proc/stat
 */
private class CoreUsageProviderO : CoreUsageProvider
{
	override fun parse(count: Int, stats: List<String>,
			timeInStates: List<List<String>>): List<Double>
	{
		if (!::_prevTimeInStates.isInitialized)
		{
			_prevTimeInStates = Array(count, {null})
			_prevUsages = (0 until count).map{.0}
			_prevTime = System.currentTimeMillis()
			// We need dt to work correctly
			return (0 until count).map{.0}
		}

		val now = System.currentTimeMillis()
		val dt = now - _prevTime
		_prevTime = now
		val usages = timeInStates.withIndex().map{parseSingle(dt, it.index,
				it.value)}
		_prevUsages = usages
		return usages
	}

	private fun parseSingle(dt: Long, core: Int, timeInState: List<String>)
			: Double
	{
		val timeInStatesResult = parseTimeInStatesOutput(timeInState)
		timeInStatesResult ?: return -1.0
		if (_prevTimeInStates[core] == null)
		{
			_prevTimeInStates[core] = timeInStatesResult
			return .0
		}

		val cpuTime_ = timeInStatesResult.zip(_prevTimeInStates[core]!!).map{
			// Unit is 10ms
			Pair(it.first.first, (it.first.second - it.second.second) * 10)
		}
		val cpuTime = if (cpuTime_.any{it.second < 0})
		{
			// Stat has been reset
			timeInStatesResult.map{Pair(it.first, it.second * 10)}
		}
		else
		{
			cpuTime_
		}

		val maxFreq = timeInStatesResult.last().first
		val weightedCpuTime = cpuTime.map{
			val freqRatio = it.first.toDouble() / maxFreq
			return@map it.second * freqRatio
		}
		val weightedCpuTimeTotal = weightedCpuTime.sum()
		_prevTimeInStates[core] = timeInStatesResult
		return weightedCpuTimeTotal / dt
	}

	private var _prevTime = 0L
	private lateinit var _prevTimeInStates: Array<Array<Pair<Long, Long>>?>
	private lateinit var _prevUsages: List<Double>
}

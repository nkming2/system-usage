package com.nkming.sysusage

import android.content.Context
import android.widget.Toast
import com.nkming.utils.Log
import eu.chainfire.libsuperuser.Shell

object ShHelper
{
	fun doShCommand(context: Context, scripts: List<String>,
			successWhere: ((exitCode: Int, output: List<String>) -> Boolean) = {
					exitCode, output -> exitCode >= 0},
			onSuccess: ((exitCode: Int, output: List<String>) -> Unit)? = null,
			onFailure: ((exitCode: Int, output: List<String>) -> Unit)? = null)
	{
		setContext(context)
		_doShCommand(scripts, successWhere, onSuccess, onFailure)
	}

	private fun _doShCommand(scripts: List<String>,
			successWhere: ((exitCode: Int, output: List<String>) -> Boolean) = {
					exitCode, output -> exitCode >= 0},
			onSuccess: ((exitCode: Int, output: List<String>) -> Unit)? = null,
			onFailure: ((exitCode: Int, output: List<String>) -> Unit)? = null)
	{
		requestShSession(onFailure).addCommand(scripts, 0,
				{commandCode, exitCode, output ->
				run{
					val output_ = output ?: listOf()
					if (exitCode == Shell.OnCommandResultListener.WATCHDOG_EXIT)
					{
						Log.e("$LOG_TAG._doShCommand", "Watchdog exception")
						// Script deadlock?
						_sh?.kill()
						onFailure?.invoke(exitCode, output_)
					}
					else if (!successWhere(exitCode, output_))
					{
						Log.e("$LOG_TAG._doShCommand",
								"Failed($exitCode) executing\nCommand: ${scripts.joinToString("\n")}\nOutput: ${output_.joinToString("\n")}")
						onFailure?.invoke(exitCode, output_)
					}
					else
					{
						onSuccess?.invoke(exitCode, output_)
					}
				}})
	}

	private fun requestShSession(
			onFailure: ((exitCode: Int, output: List<String>) -> Unit)? = null)
			: Shell.Interactive
	{
		synchronized(this)
		{
			if (_isShStarting || (_sh?.isRunning ?: false))
			{
				return _sh!!
			}

			Log.d("$LOG_TAG.requestShSession", "Starting new session")
			_isShStarting = true
			val sh = Shell.Builder()
					.useSH()
					//.setWantSTDERR(true)
					.setWatchdogTimeout(5)
					.setMinimalLogging(true)
					.open({commandCode, exitCode, output ->
					run{
						Log.d("$LOG_TAG.buildShSession",
								"Shell start status: $exitCode")
						if (exitCode
								!= Shell.OnCommandResultListener.SHELL_RUNNING)
						{
							Log.e("$LOG_TAG.buildShSession",
									"Failed opening shell (exitCode: $exitCode)")
							if (_appContext != null)
							{
								Toast.makeText(_appContext, R.string.sh_failed,
										Toast.LENGTH_LONG).show()
							}
							onFailure?.invoke(exitCode, output ?: listOf())
						}
						else
						{
							Log.i("$LOG_TAG.buildShSession", "Successful")
						}
						_isShStarting = false
					}})
			_sh = sh
			return sh
		}
	}

	private fun setContext(context: Context)
	{
		if (_appContext == null)
		{
			_appContext = context.applicationContext
		}
	}

	private val LOG_TAG = ShHelper::class.java.canonicalName

	private var _appContext: Context? = null

	private var _sh: Shell.Interactive? = null
	private var _isShStarting: Boolean = false
}

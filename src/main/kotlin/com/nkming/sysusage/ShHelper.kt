package com.nkming.sysusage

import android.content.Context
import android.os.Handler
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
		// FIXME Will cause dead loop
		if (!_sh.isRunning)
		{
			_handler.postDelayed({_doShCommand(scripts, successWhere, onSuccess,
					onFailure)}, 200)
		}
		else
		{
			__doShCommand(scripts, successWhere, onSuccess, onFailure)
		}
	}

	private fun __doShCommand(scripts: List<String>,
			successWhere: ((exitCode: Int, output: List<String>) -> Boolean) = {
					exitCode, output -> exitCode >= 0},
			onSuccess: ((exitCode: Int, output: List<String>) -> Unit)? = null,
			onFailure: ((exitCode: Int, output: List<String>) -> Unit)? = null)
	{
		_sh.addCommand(scripts, 0, {commandCode, exitCode, output ->
		run{
			if (exitCode == Shell.OnCommandResultListener.WATCHDOG_EXIT)
			{
				Log.e("$LOG_TAG.__doSuCommand", "Watchdog exception")
				_sh.kill()
				// TODO Limit retry count
				_sh = buildShSession()
				_handler.postDelayed({_doShCommand(scripts, successWhere,
						onSuccess, onFailure)}, 200)
			}
			else if (!successWhere(exitCode, output))
			{
				Log.e("$LOG_TAG.__doSuCommand",
						"Failed($exitCode) executing\nCommand: ${scripts.joinToString("\n")}\nOutput: ${output.joinToString("\n")}")
				onFailure?.invoke(exitCode, output)
			}
			else
			{
				onSuccess?.invoke(exitCode, output)
			}
		}})
	}

	private fun buildShSession(): Shell.Interactive
	{
		Log.d(LOG_TAG, "buildShSession()")
		_isShStarting = true
		return Shell.Builder()
				.useSH()
				//.setWantSTDERR(true)
				.setWatchdogTimeout(5)
				.setMinimalLogging(true)
				.open({commandCode, exitCode, output ->
				run{
					// FIXME not being called?
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
					}
					_isShStarting = false
				}})
	}

	private fun setContext(context: Context)
	{
		if (_appContext == null)
		{
			_appContext = context.applicationContext
		}
	}

	private val LOG_TAG = ShHelper::class.java.canonicalName

	private val _handler = Handler()
	private var _appContext: Context? = null

	private var _sh: Shell.Interactive = buildShSession()
		get()
		{
			if (!field.isRunning && !_isShStarting)
			{
				field = buildShSession()
			}
			return field
		}

	private var _isShStarting: Boolean = false
}

package com.nkming.sysusage

import android.os.Handler

abstract class BaseStatProvider
{
	fun init(interval: Long)
	{
		stop()
		this.interval = interval
		_onUpdate = object: Runnable
		{
			override fun run()
			{
				onUpdate()
				_handler.postDelayed(this, this@BaseStatProvider.interval)
			}
		}
		onInit(interval)
	}

	fun start()
	{
		_onUpdate ?: return
		if (!_isStarted)
		{
			_handler.post(_onUpdate)
			_isStarted = true
		}
		onStart()
	}

	fun stop()
	{
		_onUpdate ?: return
		_handler.removeCallbacks(_onUpdate)
		_isStarted = false
		onStop()
	}

	var interval: Long = 2000

	abstract protected fun onUpdate()
	protected open fun onInit(interval: Long) {}
	protected open fun onStart() {}
	protected open fun onStop() {}

	private val _handler = Handler()
	private var _onUpdate: Runnable? = null
	private var _isStarted = false
}

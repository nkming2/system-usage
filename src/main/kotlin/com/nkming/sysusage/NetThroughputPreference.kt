package com.nkming.sysusage

import android.content.Context
import android.content.res.TypedArray
import android.os.Parcel
import android.os.Parcelable
import android.preference.DialogPreference
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Spinner
import com.nkming.utils.Log
import com.nkming.utils.app.FragmentViewAwareImpl

class NetThroughputPreference : DialogPreference
{
	companion object
	{
		private val LOG_TAG = NetThroughputPreference::class.java.canonicalName
	}

	constructor(context: Context, attrs: AttributeSet)
			: super(context, attrs)

	constructor(context: Context, attrs: AttributeSet, defStyle: Int)
			: super(context, attrs, defStyle)

	override fun onCreateDialogView(): View
	{
		val container = super.onCreateDialogView() as ViewGroup?
		_root = _inflater.inflate(R.layout.net_throughput_pref, container)
		_viewAwareImpl.onActivityCreated(null)
		return _root
	}

	override fun onBindDialogView(view: View)
	{
		super.onBindDialogView(view)
		_input.setText(valueToInput(_value))
		_unit.setSelection(valueToUnit(_value))
	}

	override fun onRestoreInstanceState(state: Parcelable?)
	{
		if (state == null || state.javaClass != SavedState::class.java)
		{
			// Didn't save state for us in onSaveInstanceState
			super.onRestoreInstanceState(state)
			return
		}

		val ss = state as SavedState
		super.onRestoreInstanceState(ss.superState)
		setPref(ss.value)
	}

	override fun onDialogClosed(positiveResult: Boolean)
	{
		super.onDialogClosed(positiveResult)
		if (positiveResult)
		{
			val v = getCurrentValue()
			setValue(v)
		}
	}

	override fun onSetInitialValue(restoreValue: Boolean, defaultValue: Any?)
	{
		if (restoreValue)
		{
			setPref(getPersistedLong(_value))
		}
		else if (defaultValue != null)
		{
			val defLong = (defaultValue as Int).toLong()
			setPref(defLong)
		}
	}

	override fun onGetDefaultValue(a: TypedArray, index: Int): Int
	{
		return a.getInt(index, 100)
	}

	fun setValue(v: Long)
	{
		if (callChangeListener(v))
		{
			setPref(v)
		}
	}

	private class SavedState : BaseSavedState
	{
		constructor(source: Parcel)
				: super(source)
		{
			value = source.readLong()
		}

		constructor(superState: Parcelable)
				: super(superState)

		override fun writeToParcel(dest: Parcel, flags: Int)
		{
			super.writeToParcel(dest, flags)
			dest.writeLong(value)
		}

		var value: Long = 0
	}

	private fun <T : View> lazyView(id: Int): FragmentViewAwareImpl.LazyView<T>
	{
		return _viewAwareImpl.lazyView(id)
	}

	private fun setPref(v: Long)
	{
		val wasBlocking = shouldDisableDependents()

		_value = v
		_isValueReady = true
		persistLong(v)

		val isBlocking = shouldDisableDependents()
		if (isBlocking != wasBlocking)
		{
			notifyDependencyChange(isBlocking)
		}

		summary = NetUtils.getReadableBitSpeed(_value)
	}

	private fun getCurrentValue(): Long
	{
		val mul = when (_unit.selectedItemPosition)
		{
			0 -> 1000
			else -> 1000000
		}
		try
		{
			return (_input.text.toString().toDouble() * mul).toLong()
		}
		catch (e: Exception)
		{
			Log.e("$LOG_TAG.getCurrentValue", "Failed converting number", e)
			return 0L
		}
	}

	private fun valueToInput(v: Long): String
	{
		return NetUtils.getReadableSpeedNoUnit(v)
	}

	private fun valueToUnit(v: Long): Int
	{
		if (v > 1000000)
		{
			return 1
		}
		else
		{
			return 0
		}
	}

	private val _viewAwareImpl = object: FragmentViewAwareImpl()
	{
		override fun getView(): View?
		{
			return _root
		}
	}

	private var _value = 0L
	private var _isValueReady = false

	private val _inflater by lazy{ LayoutInflater.from(context)}
	private lateinit var _root: View
	private val _input by lazyView<EditText>(R.id.input)
	private val _unit by lazyView<Spinner>(R.id.unit)
}

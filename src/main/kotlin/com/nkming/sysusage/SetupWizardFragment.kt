package com.nkming.sysusage

import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.inputmethod.EditorInfo
import android.widget.AdapterView
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.widget.SwitchCompat
import com.nkming.utils.Log
import com.nkming.utils.app.FragmentEx
import com.nkming.utils.unit.DimensionUtils

abstract class SetupWizardFragment : FragmentEx()
{
	override fun onStart()
	{
		super.onStart()
		_hasViewDestroyed = false
	}

	override fun onDestroyView()
	{
		super.onDestroyView()
		_hasViewDestroyed = true
		_handler.removeCallbacksAndMessages(null)
	}

	/**
	 * Called in onActivityCreated(), to return whether frag's currently getting
	 * restored from backstack
	 *
	 * @return
	 */
	fun isRestoreBackstack(): Boolean
	{
		return _hasViewDestroyed
	}

	protected fun startEnterTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = _xAmplitude
			v.alpha = 0f
			_handler.postDelayed(
			{
				v.animate().translationX(0f).alpha(1f)
						.setDuration(Res.ANIMATION_FAST)
						.setInterpolator(DecelerateInterpolator())
			}, Res.ANIMATION_FAST / 2 * i)
		}
		_handler.postDelayed(endListener,
				Res.ANIMATION_FAST / 2 * (views.size + 1) + endDelay)
	}

	protected fun startExitTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = 0f
			v.alpha = 1f
			_handler.postDelayed(
			{
				v.animate().translationX(-_xAmplitude).alpha(0f)
						.setDuration(Res.ANIMATION_FAST)
						.setInterpolator(AccelerateInterpolator())
			}, Res.ANIMATION_FAST / 2 * i)
		}
		_handler.postDelayed(endListener,
				Res.ANIMATION_FAST / 2 * (views.size + 1) + endDelay)
	}

	protected fun startEnterReverseTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = 0f
			v.alpha = 1f
			_handler.postDelayed(
			{
				v.animate().translationX(_xAmplitude).alpha(0f)
						.setDuration(Res.ANIMATION_FAST)
						.setInterpolator(DecelerateInterpolator())
			}, Res.ANIMATION_FAST / 2 * i)
		}
		_handler.postDelayed(endListener,
				Res.ANIMATION_FAST / 2 * (views.size + 1) + endDelay)
	}

	protected fun startExitReverseTransition(views: List<View>,
			endListener: () -> Unit, endDelay: Long = 0)
	{
		for ((i, v) in views.withIndex())
		{
			v.translationX = -_xAmplitude
			v.alpha = 0f
			_handler.postDelayed(
			{
				v.animate().translationX(0f).alpha(1f)
						.setDuration(Res.ANIMATION_FAST)
						.setInterpolator(AccelerateInterpolator())
			}, Res.ANIMATION_FAST / 2 * i)
		}
		_handler.postDelayed(endListener,
				Res.ANIMATION_FAST / 2 * (views.size + 1) + endDelay)
	}

	private val _handler = Handler()
	private val _xAmplitude by lazy{DimensionUtils.dpToPx(context!!, 80f)}
	private var _hasViewDestroyed = false
}

class SetupWizardGreetingFragment : SetupWizardFragment()
{
	companion object
	{
		fun create(): SetupWizardGreetingFragment
		{
			return SetupWizardGreetingFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		return inflater.inflate(R.layout.setup_wizard_greeting_frag, container,
				false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		_theme.isChecked = _pref.isDarkTheme
		if (isRestoreBackstack())
		{
			startExitReverseTransition(_transitViews, {enableInputs()})
		}
		else
		{
			enableInputs()
		}
	}

	private fun onNextClick()
	{
		disableInputs()
		startExitTransition(_transitViews,
		{
			val f = SetupWizardMonitorFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, 100L)
	}

	private fun enableInputs()
	{
		_theme.setOnCheckedChangeListener{compoundButton, isChecked ->
		run{
			_pref.isDarkTheme = isChecked
			_pref.apply()
			// Will auto recreate via BaseActivity
		}}
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_theme.setOnCheckedChangeListener(null)
		_next.setOnClickListener(null)
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _next by lazyView<View>(R.id.next)
	private val _theme by lazyView<SwitchCompat>(R.id.theme_switch)
	private val _transitViews by viewAwareLazy(
	{
		listOf(findView<View>(R.id.message),
				findView(R.id.detail_message),
				findView(R.id.theme_switch))
	})

}

class SetupWizardMonitorFragment : SetupWizardFragment()
{
	companion object
	{
		fun create(): SetupWizardMonitorFragment
		{
			return SetupWizardMonitorFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		return inflater.inflate(R.layout.setup_wizard_monitor_frag, container,
				false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		_cpu.isChecked = _pref.isEnableCpu
		_mem.isChecked = _pref.isEnableMem
		_net.isChecked = _pref.isEnableNet
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			_disk.visibility = View.GONE
		}
		else
		{
			_disk.isChecked = _pref.isEnableDisk
		}
		if (isRestoreBackstack())
		{
			startExitReverseTransition(_doneTransitViews, {enableInputs()})
		}
		else
		{
			startEnterTransition(_normalTransitViews, {enableInputs()})
		}
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_normalTransitViews,
			{
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		disableInputs()
		startExitTransition(_doneTransitViews,
		{
			val f = if (_pref.isEnableNet)
			{
				SetupWizardNetMobileFragment.create()
			}
			else if (_pref.isEnableDisk)
			{
				SetupWizardDiskFragment.create()
			}
			else
			{
				SetupWizardDoneFragment.create()
			}
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, 100L)
	}

	private fun enableInputs()
	{
		_cpu.setOnCheckedChangeListener{compoundButton, isChecked ->
		run{
			_pref.isEnableCpu = isChecked
			_pref.apply()
		}}
		_mem.setOnCheckedChangeListener{compoundButton, isChecked ->
		run{
			_pref.isEnableMem = isChecked
			_pref.apply()
		}}
		_net.setOnCheckedChangeListener{compoundButton, isChecked ->
		run{
			_pref.isEnableNet = isChecked
			_pref.apply()
		}}
		_disk.setOnCheckedChangeListener{compoundButton, isChecked ->
		run{
			_pref.isEnableDisk = isChecked
			_pref.apply()
		}}
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_cpu.setOnCheckedChangeListener(null)
		_mem.setOnCheckedChangeListener(null)
		_net.setOnCheckedChangeListener(null)
		_disk.setOnCheckedChangeListener(null)
		_next.setOnClickListener(null)
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _next by lazyView<View>(R.id.next)
	private val _cpu by lazyView<SwitchCompat>(R.id.cpu_switch)
	private val _mem by lazyView<SwitchCompat>(R.id.mem_switch)
	private val _net by lazyView<SwitchCompat>(R.id.net_switch)
	private val _disk by lazyView<SwitchCompat>(R.id.disk_switch)
	private val _normalTransitViews by viewAwareLazy{listOf(_cpu, _mem, _net,
			_disk)}
	private val _doneTransitViews by viewAwareLazy{
		return@viewAwareLazy if (_pref.isEnableNet || _pref.isEnableDisk)
		{
			_normalTransitViews
		}
		else
		{
			_normalTransitViews + _next
		}
	}
}

class SetupWizardNetMobileFragment : SetupWizardFragment()
{
	companion object
	{
		fun create(): SetupWizardNetMobileFragment
		{
			return SetupWizardNetMobileFragment()
		}

		private val LOG_TAG =
				SetupWizardNetMobileFragment::class.java.canonicalName
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		return inflater.inflate(R.layout.setup_wizard_net_mobile_frag, container,
				false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		val templatePos = _netTemplateStrs.indexOf(_pref.netMobileTemplate)
		if (templatePos != -1)
		{
			_netTemplate.setSelection(templatePos)
		}
		setThroughputValue(_pref.netMobileDl, _dlInput, _dlUnit)
		setThroughputValue(_pref.netMobileUl, _ulInput, _ulUnit)
		if (isRestoreBackstack())
		{
			startExitReverseTransition(_transitViews, {enableInputs()})
		}
		else
		{
			startEnterTransition(_transitViews, {enableInputs()})
		}
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_transitViews,
			{
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		persistPreference()
		disableInputs()
		startExitTransition(_transitViews,
		{
			val f = SetupWizardNetWifiFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, 100L)
	}

	private fun enableInputs()
	{
		_netTemplate.onItemSelectedListener =
				object: AdapterView.OnItemSelectedListener
		{
			override fun onItemSelected(parent: AdapterView<*>?, view: View?,
					position: Int, id: Long)
			{
				try
				{
					val throughput = NetUtils.getMobileNetThroughput(
							_netTemplate.selectedItem.toString())
					setThroughputValue(throughput.first, _dlInput, _dlUnit)
					setThroughputValue(throughput.second, _ulInput, _ulUnit)
				}
				catch (e: IllegalArgumentException)
				{
					Log.e("$LOG_TAG.onItemSelected",
							"Failed while getMobileNetThroughput()", e)
				}
			}

			override fun onNothingSelected(parent: AdapterView<*>?)
			{}
		}
		_ulInput.setOnEditorActionListener{v, actionId, event ->
		run{
			if (actionId == EditorInfo.IME_ACTION_DONE)
			{
				onNextClick()
			}
			false
		}}
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_netTemplate.onItemSelectedListener = null
		_ulInput.setOnEditorActionListener(null)
		_next.setOnClickListener(null)
	}

	private fun persistPreference()
	{
		_pref.netMobileTemplate = _netTemplate.selectedItem.toString()
		val dlThroughput = getThroughputValue(_dlInput, _dlUnit)
		if (dlThroughput != null)
		{
			_pref.netMobileDl = dlThroughput
		}
		val ulThroughput = getThroughputValue(_ulInput, _ulUnit)
		if (ulThroughput != null)
		{
			_pref.netMobileUl = ulThroughput
		}
		_pref.commit()
	}

	private fun getThroughputValue(input: EditText, unit: Spinner): Long?
	{
		val mul = when (unit.selectedItemPosition)
		{
			0 -> 1000
			else -> 1000000
		}
		try
		{
			return (input.text.toString().toDouble() * mul).toLong()
		}
		catch (e: Exception)
		{
			Log.w("$LOG_TAG.getThroughputValue", "Failed converting number", e)
			return null
		}
	}

	private fun setThroughputValue(v: Long, input: EditText, unit: Spinner)
	{
		input.setText(NetUtils.getReadableSpeedNoUnit(v))
		unit.setSelection(if (v >= 1000000L) 1 else 0)
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _next by lazyView<View>(R.id.next)
	private val _netTemplateStrs by lazy{context!!.resources.getStringArray(
			R.array.pref_net_mobile_templates)}
	private val _netTemplate by lazyView<Spinner>(R.id.net_template)
	private val _dlInput by lazyView<EditText>(R.id.dl_input)
	private val _dlUnit by lazyView<Spinner>(R.id.dl_unit)
	private val _ulInput by lazyView<EditText>(R.id.ul_input)
	private val _ulUnit by lazyView<Spinner>(R.id.ul_unit)
	private val _transitViews by viewAwareLazy(
	{
		listOf(findView<View>(R.id.detail_message),
				_netTemplate,
				findView(R.id.dl_container),
				findView(R.id.ul_container))
	})
}

class SetupWizardNetWifiFragment : SetupWizardFragment()
{
	companion object
	{
		fun create(): SetupWizardNetWifiFragment
		{
			return SetupWizardNetWifiFragment()
		}

		private val LOG_TAG =
				SetupWizardNetWifiFragment::class.java.canonicalName
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		return inflater.inflate(R.layout.setup_wizard_net_wifi_frag, container,
				false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		setThroughputValue(_pref.netWifiDl, _dlInput, _dlUnit)
		setThroughputValue(_pref.netWifiUl, _ulInput, _ulUnit)
		if (isRestoreBackstack())
		{
			startExitReverseTransition(_doneTransitViews, {enableInputs()})
		}
		else
		{
			startEnterTransition(_normalTransitViews, {enableInputs()})
		}
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_normalTransitViews,
			{
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		persistPreference()
		disableInputs()
		startExitTransition(_doneTransitViews,
		{
			val f = if (_pref.isEnableDisk)
			{
				SetupWizardDiskFragment.create()
			}
			else
			{
				SetupWizardDoneFragment.create()
			}
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, 100L)
	}

	private fun enableInputs()
	{
		_ulInput.setOnEditorActionListener{v, actionId, event ->
		run{
			if (actionId == EditorInfo.IME_ACTION_DONE)
			{
				onNextClick()
			}
			false
		}}
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_ulInput.setOnEditorActionListener(null)
		_next.setOnClickListener(null)
	}

	private fun persistPreference()
	{
		val dlThroughput = getThroughputValue(_dlInput, _dlUnit)
		if (dlThroughput != null)
		{
			_pref.netWifiDl = dlThroughput
		}
		val ulThroughput = getThroughputValue(_ulInput, _ulUnit)
		if (ulThroughput != null)
		{
			_pref.netWifiUl = ulThroughput
		}
		_pref.commit()
	}

	private fun getThroughputValue(input: EditText, unit: Spinner): Long?
	{
		val mul = when (unit.selectedItemPosition)
		{
			0 -> 1000
			else -> 1000000
		}
		try
		{
			return (input.text.toString().toDouble() * mul).toLong()
		}
		catch (e: Exception)
		{
			Log.w("$LOG_TAG.getThroughputValue", "Failed converting number", e)
			return null
		}
	}

	private fun setThroughputValue(v: Long, input: EditText, unit: Spinner)
	{
		input.setText(NetUtils.getReadableSpeedNoUnit(v))
		unit.setSelection(if (v >= 1000000L) 1 else 0)
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _next by lazyView<View>(R.id.next)
	private val _dlInput by lazyView<EditText>(R.id.dl_input)
	private val _dlUnit by lazyView<Spinner>(R.id.dl_unit)
	private val _ulInput by lazyView<EditText>(R.id.ul_input)
	private val _ulUnit by lazyView<Spinner>(R.id.ul_unit)
	private val _normalTransitViews by viewAwareLazy(
	{
		listOf(findView<View>(R.id.detail_message),
				findView<View>(R.id.dl_container),
				findView<View>(R.id.ul_container))
	})
	private val _doneTransitViews by viewAwareLazy(
	{
		if (_pref.isEnableDisk)
		{
			_normalTransitViews
		}
		else
		{
			listOf(findView<View>(R.id.detail_message),
					findView<View>(R.id.dl_container),
					findView<View>(R.id.ul_container),
					_next)
		}
	})
}

class SetupWizardDiskFragment : SetupWizardFragment()
{
	companion object
	{
		fun create(): SetupWizardDiskFragment
		{
			return SetupWizardDiskFragment()
		}

		private val LOG_TAG =
				SetupWizardDiskFragment::class.java.canonicalName
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		return inflater.inflate(R.layout.setup_wizard_disk_frag, container,
				false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		setThroughputValue(_pref.diskRead, _readInput, _readUnit)
		setThroughputValue(_pref.diskWrite, _writeInput, _writeUnit)
		if (isRestoreBackstack())
		{
			startExitReverseTransition(_doneTransitViews, {enableInputs()})
		}
		else
		{
			startEnterTransition(_normalTransitViews, {enableInputs()})
		}
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_normalTransitViews,
			{
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onNextClick()
	{
		persistPreference()
		disableInputs()
		startExitTransition(_doneTransitViews,
		{
			val f = SetupWizardDoneFragment.create()
			fragmentManager!!.beginTransaction()
					.replace(R.id.container, f)
					.addToBackStack(null)
					.commitAllowingStateLoss()
		}, 100L)
	}

	private fun enableInputs()
	{
		_writeInput.setOnEditorActionListener{v, actionId, event ->
		run{
			if (actionId == EditorInfo.IME_ACTION_DONE)
			{
				onNextClick()
			}
			false
		}}
		_next.setOnClickListener{onNextClick()}
	}

	private fun disableInputs()
	{
		_writeInput.setOnEditorActionListener(null)
		_next.setOnClickListener(null)
	}

	private fun persistPreference()
	{
		val readThroughput = getThroughputValue(_readInput, _readUnit)
		if (readThroughput != null)
		{
			_pref.diskRead = readThroughput
		}
		val writeThroughput = getThroughputValue(_writeInput, _writeUnit)
		if (writeThroughput != null)
		{
			_pref.diskWrite = writeThroughput
		}
		_pref.commit()
	}

	private fun getThroughputValue(input: EditText, unit: Spinner): Long?
	{
		val mul = when (unit.selectedItemPosition)
		{
			0 -> 1024
			else -> 1048576
		}
		try
		{
			return (input.text.toString().toDouble() * mul).toLong()
		}
		catch (e: Exception)
		{
			Log.w("$LOG_TAG.getThroughputValue", "Failed converting number", e)
			return null
		}
	}

	private fun setThroughputValue(v: Long, input: EditText, unit: Spinner)
	{
		input.setText(DiskUtils.getReadableSpeedNoUnit(v))
		unit.setSelection(if (v >= 1048576L) 1 else 0)
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _next by lazyView<View>(R.id.next)
	private val _readInput by lazyView<EditText>(R.id.read_input)
	private val _readUnit by lazyView<Spinner>(R.id.read_unit)
	private val _writeInput by lazyView<EditText>(R.id.write_input)
	private val _writeUnit by lazyView<Spinner>(R.id.write_unit)
	private val _normalTransitViews by viewAwareLazy(
	{
		listOf(findView<View>(R.id.detail_message),
				findView(R.id.read_container),
				findView(R.id.write_container))
	})
	private val _doneTransitViews by viewAwareLazy(
	{
		listOf(findView<View>(R.id.detail_message),
				findView(R.id.read_container),
				findView(R.id.write_container),
				_next)
	})
}

class SetupWizardDoneFragment : SetupWizardFragment()
{
	companion object
	{
		fun create(): SetupWizardDoneFragment
		{
			return SetupWizardDoneFragment()
		}
	}

	override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
			savedInstanceState: Bundle?): View
	{
		return inflater.inflate(R.layout.setup_wizard_done_frag, container,
				false)
	}

	override fun onActivityCreated(savedInstanceState: Bundle?)
	{
		super.onActivityCreated(savedInstanceState)
		startEnterTransition(_transitViews, {enableInputs()})
	}

	override fun onBackPressed(): Boolean
	{
		if (!super.onBackPressed())
		{
			startEnterReverseTransition(_transitViews,
			{
				fragmentManager!!.popBackStack()
			}, 100L)
		}
		return true
	}

	private fun onDoneClick()
	{
		persistPreference()
		activity?.finish()
		if (_pref.isEnableCpu || _pref.isEnableMem || _pref.isEnableNet
				|| _pref.isEnableDisk)
		{
			NotifService.start(context!!)
		}
	}

	private fun enableInputs()
	{
		_done.setOnClickListener{onDoneClick()}
	}

	private fun persistPreference()
	{
		_pref.hasRunSetupWizard = true
		_pref.commit()
	}

	private val _pref by lazy{Preference.from(context!!)}
	private val _done by lazyView<View>(R.id.next)
	private val _transitViews by viewAwareLazy(
	{
		listOf(findView<View>(R.id.message),
				_done)
	})
}

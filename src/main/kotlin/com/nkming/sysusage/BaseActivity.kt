package com.nkming.sysusage

import android.content.SharedPreferences
import android.os.Bundle
import com.nkming.utils.app.AppCompatActivityEx

open class BaseActivity : AppCompatActivityEx()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setTheme(_theme)
		_pref.onSharedPreferenceChangeListener =
				SharedPreferences.OnSharedPreferenceChangeListener{pref, key ->
				run{
					if (key == getString(R.string.pref_dark_theme_key))
					{
						recreate()
					}
				}}
	}

	protected open val _hasActionBar = true

	private val _theme: Int
		get()
		{
			return if (_pref.isDarkTheme)
			{
				if (_hasActionBar) R.style.AppTheme_Dark else
						R.style.AppTheme_Dark_NoActionBar
			}
			else
			{
				if (_hasActionBar) R.style.AppTheme_Light else
						R.style.AppTheme_Light_NoActionBar
			}
		}

	private val _pref by lazy{Preference.from(this)}
}

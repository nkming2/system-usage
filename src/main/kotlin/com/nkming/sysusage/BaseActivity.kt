package com.nkming.sysusage

import android.content.SharedPreferences
import android.os.Bundle
import com.nkming.utils.app.AppCompatActivityEx

open class BaseActivity : AppCompatActivityEx()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setTheme(if (_pref.isDarkTheme) R.style.AppTheme_Dark
				else R.style.AppTheme_Light)
		_pref.onSharedPreferenceChangeListener =
				SharedPreferences.OnSharedPreferenceChangeListener{pref, key ->
				run{
					if (key == getString(R.string.pref_dark_theme_key))
					{
						recreate()
					}
				}}
	}

	private val _pref by lazy{Preference.from(this)}
}

package com.nkming.sysusage

import android.os.Bundle

class PreferenceActivity : BaseActivity()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.frag_activity)
		if (savedInstanceState == null)
		{
			fragmentManager.beginTransaction()
					.add(R.id.container, PreferenceFragment.create())
					.commit()
		}
	}
}

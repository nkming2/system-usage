package com.nkming.sysusage

import android.os.Bundle

class DevActivity : BaseActivity()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		setContentView(R.layout.frag_activity)
		if (savedInstanceState == null)
		{
			supportFragmentManager.beginTransaction()
					.add(R.id.container, DevFragment.create())
					.commit()
		}
	}
}

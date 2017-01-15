package com.nkming.sysusage

import android.content.Intent
import android.os.Bundle

class PreferenceActivity : BaseActivity()
{
	override fun onCreate(savedInstanceState: Bundle?)
	{
		super.onCreate(savedInstanceState)
		val pref = Preference.from(this)
		if (!pref.hasRunSetupWizard)
		{
			startActivity(Intent(this, SetupWizardActivity::class.java))
			finish()
		}
		else
		{
			setContentView(R.layout.frag_activity)
			if (savedInstanceState == null)
			{
				fragmentManager.beginTransaction()
						.add(R.id.container, PreferenceFragment.create())
						.commit()
			}
		}
	}
}

package com.indiza.smstask.tools

import android.content.Context
import androidx.core.content.edit

object SimPreference {

    private const val PREF_NAME = "sim_preferences"
    private const val KEY_SELECTED_SIM = "selected_sim_slot"

    fun saveSelectedSim(context: Context, slot: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit {
            putInt(KEY_SELECTED_SIM, slot)
        }
    }

    fun loadSelectedSim(context: Context): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SELECTED_SIM, -1) // -1 = SIM système
    }
}

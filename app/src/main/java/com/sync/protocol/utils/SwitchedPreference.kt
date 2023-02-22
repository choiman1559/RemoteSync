package com.sync.protocol.utils;

import android.content.Context
import android.os.Build
import android.util.AttributeSet
import androidx.appcompat.widget.SwitchCompat
import androidx.preference.PreferenceViewHolder
import androidx.preference.SwitchPreference
import com.sync.protocol.R

class SwitchedPreference(context: Context?, attrs: AttributeSet?) :
    SwitchPreference(context!!, attrs) {

    private var defaultChecked: Boolean = false
    private var switchCompat: SwitchCompat? = null

    init {
        widgetLayoutResource = if(Build.VERSION.SDK_INT > 24) R.layout.item_switch else R.layout.item_switch_v24
    }

    override fun setChecked(checked: Boolean) {
        super.setChecked(checked)
        defaultChecked = checked
    }

    override fun onBindViewHolder(holder: PreferenceViewHolder) {
        super.onBindViewHolder(holder)
        switchCompat = holder.findViewById(R.id.switchCompat) as SwitchCompat?
        switchCompat?.apply {
            this.isChecked = defaultChecked
        }
    }
}
package com.example.heartrateblock

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.withStyledAttributes

class AppCard @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {
    private val nameTextView : TextView
    private val usageTextView : TextView

    init {
        orientation = HORIZONTAL

        LayoutInflater.from(context).inflate(R.layout.app_card, this)
        nameTextView = findViewById(R.id.name)
        usageTextView = findViewById(R.id.usage)

        attrs?.let {
            context.withStyledAttributes(it, R.styleable.AppCard, 0, 0) {
                nameTextView.text = getString(R.styleable.AppCard_name)
                usageTextView.text = getString(R.styleable.AppCard_usageTime)
            }
        }
    }

    fun setData(name : String, usage : String) {
        nameTextView.text = name
        usageTextView.text = usage
    }
}
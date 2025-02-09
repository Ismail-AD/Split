package com.appdev.split.UI.Fragment

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import com.appdev.split.R
import com.appdev.split.databinding.FragmentChartBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener


class ChartFragment : Fragment() {
    private lateinit var binding: FragmentChartBinding
    private var months: List<String> = emptyList()
    private var values: List<Float> = emptyList()
    private var monthsWithYears: List<String> = emptyList()
    private var onMonthSelectedListener: ((String) -> Unit)? = null

    companion object {
        fun newInstance(
            months: List<String>,
            values: List<Float>,
            monthsWithYears: List<String>
        ) = ChartFragment().apply {
            arguments = Bundle().apply {
                putStringArrayList("months", ArrayList(months))
                putFloatArray("values", values.toFloatArray())
                putStringArrayList("monthsWithYears", ArrayList(monthsWithYears))
            }
        }
    }

    fun setOnMonthSelectedListener(listener: (String) -> Unit) {
        onMonthSelectedListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        months = arguments?.getStringArrayList("months") ?: emptyList()
        values = arguments?.getFloatArray("values")?.toList() ?: emptyList()
        monthsWithYears = arguments?.getStringArrayList("monthsWithYears") ?: emptyList()

        setupBarChart()
    }

    private fun setupBarChart() {
        val entries = values.mapIndexed { index, value -> BarEntry(index.toFloat(), value) }

        val dataSet = BarDataSet(entries, "Monthly Expenses").apply {
            color = ContextCompat.getColor(requireContext(), R.color.myFilledState)
            valueTextSize = 12f
            highLightColor = ContextCompat.getColor(requireContext(), R.color.ClickedState)
            isHighlightEnabled = true
            highLightAlpha = 255
        }

        binding.barChart.apply {
            data = BarData(dataSet)
            description.isEnabled = false
            animateY(1000)
            setFitBars(true)

            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(months)
                granularity = 1f
                setDrawGridLines(false)
            }

            axisLeft.apply {
                axisMinimum = 0f
                // Consider making this dynamic based on max value
                val maxValue = values.maxOrNull() ?: 20000f
                axisMaximum = maxValue * 1.2f // Add 20% padding
                setLabelCount(3, true)
                granularity = maxValue / 2
            }
            axisRight.isEnabled = false
            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val index = it.x.toInt()
                        if (index in monthsWithYears.indices) {
                            onMonthSelectedListener?.invoke(monthsWithYears[index])
                        }
                    }
                }

                override fun onNothingSelected() {
                    // Optional: Handle when nothing is selected
                }
            })
        }
    }

    fun updateChartData(newValues: List<Float>) {
        values = newValues
        setupBarChart()
    }

}
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
import java.util.Calendar


class ChartFragment : Fragment() {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private var months: List<String> = emptyList()
    private var values: List<Float> = emptyList()
    private var monthsWithYears: List<String> = emptyList()
    private var onMonthSelectedListener: ((String) -> Unit)? = null
    private var pendingChartUpdate: List<Float>? = null
    private var firstSelectionDone = false

    private val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
    private val currentMonthYear = "${Calendar.getInstance().get(Calendar.YEAR)}-${currentMonthIndex + 1}"


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
        onMonthSelectedListener = { monthYear ->
            if ((!firstSelectionDone && !HistoryFragment.hasInitialLoadOccurred) || monthYear != currentMonthYear) {
                listener(monthYear)
                firstSelectionDone = true
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentChartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        months = arguments?.getStringArrayList("months") ?: emptyList()
        values = arguments?.getFloatArray("values")?.toList() ?: emptyList()
        monthsWithYears = arguments?.getStringArrayList("monthsWithYears") ?: emptyList()

        setupBarChart()
        val initialSelection = monthsWithYears.indexOf(currentMonthYear)
        if (initialSelection != -1) {
            binding.barChart.highlightValue(initialSelection.toFloat(), 0)
            onMonthSelectedListener?.invoke(currentMonthYear)
        }
        pendingChartUpdate?.let {
            updateChartData(it)
            pendingChartUpdate = null
        }
    }

    private fun setupBarChart() {
        if (!isAdded) return
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
            isHighlightPerDragEnabled = false
            setScaleEnabled(false) // Disable zooming
            setPinchZoom(false) // Disable pinch zoom

            axisLeft.apply {
                axisMinimum = 0f
                val maxValue = values.maxOrNull() ?: 20000f
                axisMaximum = maxValue * 1.2f
                setLabelCount(3, true)
                granularity = maxValue / 2
            }
            axisRight.isEnabled = false

            setOnChartValueSelectedListener(object : OnChartValueSelectedListener {
                override fun onValueSelected(e: Entry?, h: Highlight?) {
                    e?.let {
                        val index = it.x.toInt()
                        if (index in monthsWithYears.indices && it.y > 0f) { // Ignore bars with zero height
                            onMonthSelectedListener?.invoke(monthsWithYears[index])
                        }
                    }
                }

                override fun onNothingSelected() {}
            })
        }
    }


    fun updateChartData(newValues: List<Float>) {
        if (!isAdded || _binding == null) {
            // Store the update for later if the fragment isn't ready
            pendingChartUpdate = newValues
            return
        }

        values = newValues
        setupBarChart()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
package com.appdev.split.UI.Fragment

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.appdev.split.Graph.CustomBarGraph
import com.appdev.split.Model.ViewModel.BarGraphViewModel
import com.appdev.split.R
import com.appdev.split.databinding.FragmentChartBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class ChartFragment : Fragment() {
    private var _binding: FragmentChartBinding? = null
    private val binding get() = _binding!!
    private var months: List<String> = emptyList()
    private var values: List<Float> = emptyList()
    private var monthsWithYears: List<String> = emptyList()
    private var onMonthSelectedListener: ((String) -> Unit)? = null
    private var pendingChartUpdate: List<Float>? = null
    private var firstSelectionDone = false
    private val viewModel: BarGraphViewModel by viewModels()
    private val currentMonthIndex = Calendar.getInstance().get(Calendar.MONTH)
    private var currentMonthYear = "${Calendar.getInstance().get(Calendar.YEAR)}-${Calendar.getInstance().get(Calendar.MONTH) + 1}"

    private var skipInitialCallback = true
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
            Log.d("ClickDebug", "Month selected in ChartFragment: $monthYear")
            Log.d("ClickDebug", "First selection done: $firstSelectionDone")
            Log.d("ClickDebug", "Current month year: $currentMonthYear")

            if (monthYear != currentMonthYear) {
                currentMonthYear = monthYear
                listener(monthYear)
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


    // In ChartFragment
    private fun setupBarChart() {
        if (!isAdded) return

        val customBars = values.mapIndexed { index, value ->
            Log.d("ClickDebug", "Setting up bar for ${months[index]} with value $value")
            CustomBarGraph.BarData(
                value = value,
                label = months[index]
            )
        }

        binding.barChart.apply {
            // Set maximum value with 20% padding
            val maxValue = 1000f
            setMaxValue(maxValue)
            setYAxisConfig(0f, maxValue * 1.2f, 3)
            // Set data and animate
            setOnBarClickListener(object : CustomBarGraph.OnBarClickListener {


                override fun onBarClick(barData: CustomBarGraph.BarData, position: Int) {
                    Log.d("ClickDebug", "Bar clicked: ${barData.label} at position $position")
                    if (position in monthsWithYears.indices) {
                        Log.d("ClickDebug", "Invoking listener with ${monthsWithYears[position]}")
                        onMonthSelectedListener?.invoke(monthsWithYears[position])
                    }
                }
            })

            // Set data
            setData(customBars)
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
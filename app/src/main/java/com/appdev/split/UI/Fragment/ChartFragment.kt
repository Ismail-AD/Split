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
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.appdev.split.Graph.CustomBarGraph
import com.appdev.split.Model.ViewModel.BarGraphViewModel
import com.appdev.split.Model.ViewModel.MainViewModel
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
import kotlinx.coroutines.launch
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
    private var currentMonthYear = "${Calendar.getInstance().get(Calendar.YEAR)}-${
        Calendar.getInstance().get(Calendar.MONTH) + 1
    }"
    private val mainViewModel by activityViewModels<MainViewModel>()

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
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.selectedMonthYears.collect { selectedMonthYear ->
                    updateChartSelection(selectedMonthYear)
                }
            }
        }

        pendingChartUpdate?.let {
            updateChartData(it)
            pendingChartUpdate = null
        }
    }

    private fun updateChartSelection(selectedMonthYear: String) {
        val selectedIndex = monthsWithYears.indexOf(selectedMonthYear)
        if (selectedIndex != -1) {
            binding.barChart.highlightValue(selectedIndex.toFloat(), 0)
        } else {
            binding.barChart.clearHighlight()
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
            val maxValue = 10000f
            setMaxValue(maxValue)
            setYAxisConfig(0f, maxValue * 1.2f, 3)
            // Set data and animate
            setOnBarClickListener(object : CustomBarGraph.OnBarClickListener {


                override fun onBarClick(barData: CustomBarGraph.BarData, position: Int) {
                    if (position in monthsWithYears.indices) {
                        val selectedMonthYear = monthsWithYears[position]
                        mainViewModel.updateSelectedMonth(selectedMonthYear)
                        onMonthSelectedListener?.invoke(selectedMonthYear)
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

        // Create new bar data with the updated values
        val customBars = months.mapIndexed { index, month ->
            CustomBarGraph.BarData(
                value = if (index < newValues.size) newValues[index] else 0f,
                label = month
            )
        }

        // Set data WITHOUT animation for updates
        binding.barChart.setData(customBars, animate = false)

        // Make sure the current selection is maintained
        mainViewModel.selectedMonthYear.value?.let { updateChartSelection(it) }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


}
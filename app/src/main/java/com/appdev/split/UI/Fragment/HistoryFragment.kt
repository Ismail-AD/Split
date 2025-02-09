package com.appdev.split.UI.Fragment

import android.graphics.PorterDuff
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.appdev.split.Adapters.AllFriendExpenseAdapter
import com.appdev.split.Adapters.BillAdapter
import com.appdev.split.Model.Data.FriendExpenseRecord
import com.appdev.split.Model.Data.MySpending
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentAddGroupBinding
import com.appdev.split.databinding.FragmentHistoryBinding
import com.appdev.split.databinding.FragmentHomeBinding
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.listener.OnChartValueSelectedListener
import kotlinx.coroutines.launch
import java.util.Calendar
import kotlin.math.min


class HistoryFragment : Fragment() {

    private var _binding: FragmentHistoryBinding? = null
    private val binding get() = _binding!!
    private lateinit var chartPagerAdapter: ChartPagerAdapter
    private lateinit var adapter: AllFriendExpenseAdapter
    private var isTopDataReady = false
    private var isMainDataReady = false

    private var currentYear = Calendar.getInstance().get(Calendar.YEAR)
    private val allMonthsWithYears = generateMonthYearList() // Add this property
    var monthySpendings: Double = 0.0

    var expenses: List<FriendExpenseRecord> = listOf()
    val mainViewModel by activityViewModels<MainViewModel>()
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHistoryBinding.inflate(inflater, container, false)
        setupShimmer()
        return binding.root
    }

    private fun generateMonthYearList(): List<String> {
        return (0..11).map { monthIndex ->
            val monthNumber = monthIndex + 1
            "$currentYear-$monthNumber"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel.getMonthlyExpense(Utils.getYearMonth())
        mainViewModel.getMonthsTotalSpent(getCurrentlyVisibleMonths())
        val months = listOf(
            "Jan", "Feb", "Mar", "Apr",
            "May", "Jun", "Jul", "Aug",
            "Sep", "Oct", "Nov", "Dec"
        )


        val chartFragments = mutableListOf<Fragment>()


        for (i in months.indices step 3) {
            val monthSubset = months.subList(i, min(i + 3, months.size))
            val monthYearSubset = allMonthsWithYears.subList(i, min(i + 3, allMonthsWithYears.size))
            val chartFragment = ChartFragment.newInstance(
                monthSubset,
                List(monthSubset.size) { 0f },
                monthYearSubset
            ).apply {
                setOnMonthSelectedListener { selectedMonthYear ->
                    // Handle the selected month-year
                    mainViewModel.getMonthlyExpense(selectedMonthYear)
                }
            }
            chartFragments.add(chartFragment)
        }

        chartPagerAdapter = ChartPagerAdapter(this, chartFragments)
        binding.chartViewPager.adapter = chartPagerAdapter

        binding.chartViewPager.registerOnPageChangeCallback(object :
            ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updateNavigationButtons(position)
                val visibleMonths = getCurrentlyVisibleMonths()
                if (mainViewModel.monthsWithYears.isNotEmpty() &&
                    areMonthListsDifferent(mainViewModel.monthsWithYears, visibleMonths)
                ) {
                    mainViewModel.getMonthsTotalSpent(visibleMonths)
                }
            }
        })

        binding.btnPrevious.setOnClickListener {
            val current = binding.chartViewPager.currentItem
            if (current > 0) {
                binding.chartViewPager.currentItem = current - 1
            }
        }

        binding.btnNext.setOnClickListener {
            val current = binding.chartViewPager.currentItem
            if (current < chartPagerAdapter.itemCount - 1) {
                binding.chartViewPager.currentItem = current + 1
            }
        }
        updateNavigationButtons(0)
        setupStateObservers()

    }

    private fun setupStateObservers() {
        // Observer for months total spent (Top Data)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.monthsTotalSpentState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            if (!returnShimmerState()) {
                                showTopShimmer()
                            }
                        }

                        is UiState.Success -> {
                            isTopDataReady = true
                            hideTopShimmer()
                            monthySpendings = state.data.sumOf { it.totalAmountSpend }
                            binding.totalSpent.text = String.format("%.2f", monthySpendings)
                            updateChartData(state.data)

                        }

                        is UiState.Error -> {
                            isTopDataReady = true
                            hideTopShimmer()
                            Log.d("CHL","Top "+state.message)
                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }

                        is UiState.Stable -> {

                        }
                    }
                }
            }
        }

        // Observer for monthly expenses (Bottom Data)
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.monthBaseExpensesState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            showBottomShimmer()
                        }

                        is UiState.Success -> {
                            isMainDataReady = true
                            if (isTopDataReady) {
                                hideBottomShimmer()
                                expenses = state.data
                                updateRecyclerView(expenses)
                            }
                        }

                        is UiState.Error -> {
                            hideBottomShimmer()
                            Log.d("CHL","Bottom "+state.message)

                            Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
                        }

                        is UiState.Stable -> {

                        }
                    }
                }
            }
        }
    }

    private fun updateRecyclerView(expenses: List<FriendExpenseRecord>) {
        // Check if binding is null before accessing
        val safeBinding = _binding ?: return

        if (expenses.isEmpty()) {
            safeBinding.noBill.visibility = View.VISIBLE
            safeBinding.recyclerViewRecentBills.visibility = View.GONE
        } else {
            safeBinding.noBill.visibility = View.GONE
            safeBinding.recyclerViewRecentBills.visibility = View.VISIBLE

            adapter = AllFriendExpenseAdapter(expenses, ::goToDetails)
            safeBinding.recyclerViewRecentBills.adapter = adapter
            safeBinding.recyclerViewRecentBills.layoutManager =
                LinearLayoutManager(requireContext())
        }
    }

    fun goToDetails(expenseList: FriendExpenseRecord) {
        Log.d("CHKIAMG", "I am going in")

        val action = HistoryFragmentDirections.actionHistoryToBillDetails(
            null, null, null, expenseList, null, expenseList.friendId
        )
        findNavController().navigate(action)

    }

    private fun updateChartData(spendingList: List<MySpending>) {
        val chartData = spendingList.map { it.totalAmountSpend.toFloat() }

        for (i in chartPagerAdapter.fragments.indices) {
            val fragment = chartPagerAdapter.fragments[i] as? ChartFragment
            val startIndex = i * 3
            val endIndex = minOf(startIndex + 3, chartData.size)

            if (fragment != null && startIndex < endIndex) {
                val subsetData = chartData.subList(startIndex, endIndex)
                fragment.updateChartData(subsetData)
            }
        }
    }


    private fun areMonthListsDifferent(list1: List<String>, list2: List<String>): Boolean {
        // First check if sizes are different
        if (list1.size != list2.size) return true

        // Compare each element in order
        return list1.zip(list2).any { (month1, month2) -> month1 != month2 }
    }

    private fun getCurrentlyVisibleMonths(): List<String> {
        val currentPosition = binding.chartViewPager.currentItem
        val startIndex = currentPosition * 3
        val endIndex = minOf(startIndex + 3, allMonthsWithYears.size)
        return allMonthsWithYears.subList(startIndex, endIndex)
    }

    private fun updateNavigationButtons(position: Int) {
        val isFirst = position == 0
        val isLast = position == chartPagerAdapter.itemCount - 1

        binding.btnPrevious.isEnabled = !isFirst
        binding.btnNext.isEnabled = !isLast

        binding.btnPrevious.setColorFilter(
            ContextCompat.getColor(requireContext(), if (isFirst) R.color.gray else R.color.black),
            PorterDuff.Mode.SRC_IN
        )

        binding.btnNext.setColorFilter(
            ContextCompat.getColor(requireContext(), if (isLast) R.color.gray else R.color.black),
            PorterDuff.Mode.SRC_IN
        )
    }

    private fun setupShimmer() {
        // Set the layout for the ViewStub
        binding.shimmerViewHome.layoutResource = R.layout.history_page_shimmer
        binding.shimmerViewHome.inflate()
        binding.shimmerViewBar.layoutResource = R.layout.history_graph_shimmer
        binding.shimmerViewBar.inflate()

    }

    fun returnShimmerState(): Boolean {
        return binding.shimmerViewTop.isShimmerStarted
    }

    private fun showTopShimmer() {
        binding.shimmerViewBar.visibility = View.VISIBLE
        binding.topLayer.visibility = View.GONE
        binding.shimmerViewTop.startShimmer()
    }

    private fun hideTopShimmer() {
        binding.shimmerViewBar.visibility = View.GONE
        binding.shimmerViewTop.stopShimmer()
        binding.topLayer.visibility = View.VISIBLE
    }

    private fun showBottomShimmer() {
        binding.shimmerViewHome.visibility = View.VISIBLE
        binding.noBill.visibility = View.GONE
        binding.recyclerViewRecentBills.visibility = View.GONE
        binding.shimmerViewContainer.startShimmer()
    }

    private fun hideBottomShimmer() {
        binding.shimmerViewHome.visibility = View.GONE
        binding.shimmerViewContainer.stopShimmer()
        binding.recyclerViewRecentBills.visibility = View.VISIBLE
    }


}

class ChartPagerAdapter(fragment: Fragment, val fragments: List<Fragment>) :
    FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = fragments.size
    override fun createFragment(position: Int): Fragment = fragments[position]
}

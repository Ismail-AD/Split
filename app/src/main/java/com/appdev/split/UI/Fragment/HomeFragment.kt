package com.appdev.split.UI.Fragment

import android.app.Dialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.BillAdapter
import com.appdev.split.Model.Data.Bill
import com.appdev.split.Model.Data.ExpenseRecord
import com.appdev.split.Model.Data.FriendExpenseRecord
import com.appdev.split.Model.Data.TransactionItem
import com.appdev.split.Model.Data.UiState
import com.appdev.split.Model.ViewModel.MainViewModel
import com.appdev.split.R
import com.appdev.split.UI.Activity.EntryActivity
import com.appdev.split.Utils.ThemeUtils
import com.appdev.split.Utils.Utils
import com.appdev.split.databinding.FragmentHomeBinding
import com.bumptech.glide.Glide
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import javax.inject.Inject


@AndroidEntryPoint
class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BillAdapter
    val mainViewModel by activityViewModels<MainViewModel>()
    var expenses: Map<String, List<FriendExpenseRecord>> = mapOf()

    private var isTopDataReady = false
    private var isMainDataReady = false

    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    lateinit var dialog: Dialog
    private var isExpanded = false
    @Inject
    lateinit var firebaseAnalytics: FirebaseAnalytics


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        mainViewModel.setFriendDefault()
        mainViewModel.updateStateToStable()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        dialog = Dialog(requireContext())
        if(ThemeUtils.isDarkMode(requireContext())){
            binding.card.background = requireContext().resources.getDrawable(R.drawable.blue_maybe)
        } else{
            binding.card.background = requireContext().resources.getDrawable(R.drawable.home_back_four)
        }
        setupShimmer()

        binding.addExpense.setOnClickListener {
            onExpenseClicked()
        }


        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.allExpensesState.collect { state ->
                    when (state) {
                        is UiState.Loading -> {
                            if (!returnShimmerState()) {
                                showShimmer()
                            }
                        }

                        is UiState.Success -> {
                            isMainDataReady = true
                            checkAndShowContent()
                            expenses = state.data.mapValues { (_, expenseList) ->
                                if (expenseList.isNotEmpty()) {
                                    // Get the most recent expense based on timestamp
                                    val mostRecent = expenseList.maxByOrNull { it.timeStamp }
                                    mostRecent?.let { listOf(it) } ?: emptyList()
                                } else {
                                    emptyList()
                                }
                            }
                            Log.d("CHKEXPAZ","${expenses}")
                            Log.d("CHKEXPAZ","${state.data}")
                            updateRecyclerView(expenses)
                            logHomeScreenViewed()
                        }

                        is UiState.Error -> {
                            isMainDataReady = true
                            checkAndShowContent()
                            if (expenses.isEmpty()) {
                                binding.noBill.visibility = View.VISIBLE
                                binding.recyclerViewRecentBills.visibility = View.GONE
                            }
                        }

                        UiState.Stable -> {
                            isMainDataReady = true
                        }
                    }
                }
            }
        }


        val uid = FirebaseAuth.getInstance().currentUser?.uid

        uid?.let {
            mainViewModel.fetchUserData(it)
            mainViewModel.setupRealTimeExpensesListener() // Add this line
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.userData.collect { user ->
                    user?.let {
                        binding.name.text = "Hello, ${it.name} ✌\uFE0F"
                        binding.name.visibility = View.VISIBLE
                        Glide.with(requireContext()).load(user.imageUrl)
                            .placeholder(R.drawable.profile_imaage)
                            .error(R.drawable.profile_imaage).into(binding.circularImage)
                        isTopDataReady = true
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                mainViewModel.loadingState.collect { user ->
                    if (user) {
                        if (!returnShimmerState()) {
                            showShimmer()
                        }
                    } else {
                        checkAndShowContent()
                    }
                }
            }
        }

    }

    private fun logHomeScreenViewed() {
        val params = Bundle().apply {
            putInt("expense_count", expenses.size)
            putBoolean("has_expenses", expenses.isNotEmpty())
            putString("view_timestamp", getCurrentDate(System.currentTimeMillis().toString()))
        }
        firebaseAnalytics.logEvent("home_screen_viewed", params)
    }


    override fun onResume() {
        super.onResume()
        isExpanded = false
    }

    private fun setupShimmer() {
        // Set the layout for the ViewStub
        binding.shimmerViewHome.layoutResource = R.layout.home_page_shimmer
        binding.shimmerViewHome.inflate()
        binding.shimmerViewBar.layoutResource = R.layout.top_bar_shimmer
        binding.shimmerViewBar.inflate()

    }

    fun returnShimmerState(): Boolean {
        return binding.shimmerViewTop.isShimmerStarted
    }

    private fun showShimmer() {
        binding.shimmerContainer.visibility = View.VISIBLE
        binding.topLayer.visibility = View.GONE
        binding.mainBody.visibility = View.GONE
        binding.shimmerViewTop.startShimmer()
        binding.shimmerViewContainer.startShimmer()
    }

    private fun checkAndShowContent() {
        if (isTopDataReady && isMainDataReady) {
            hideShimmer()
        }
    }

    private fun hideShimmer() {
        // Only show content when both top and main data are ready
        if (isTopDataReady && isMainDataReady) {
            binding.shimmerContainer.visibility = View.GONE
            binding.shimmerViewTop.stopShimmer()
            binding.shimmerViewContainer.stopShimmer()

            // Show both layouts together
            binding.topLayer.visibility = View.VISIBLE
            binding.mainBody.visibility = View.VISIBLE
        }
    }


    private fun updateRecyclerView(expenses: Map<String, List<FriendExpenseRecord>>) {
        if (expenses.isEmpty()) {
            binding.noBill.visibility = View.VISIBLE
            binding.recyclerViewRecentBills.visibility = View.GONE
        } else {
            binding.noBill.visibility = View.GONE
            binding.recyclerViewRecentBills.visibility = View.VISIBLE
            // Update adapter with the new data
            adapter = BillAdapter(expenses, ::goToDetails)
            binding.recyclerViewRecentBills.adapter = adapter
            binding.recyclerViewRecentBills.layoutManager = LinearLayoutManager(requireContext())
        }
    }


    private fun onContactClicked() {
        Log.d("CHKERR", mainViewModel.userData.value.toString() + "At home add member")

        val action = HomeFragmentDirections.actionHomePageToAddMembersFragment(false, "")
        findNavController().navigate(action)
    }

    private fun onExpenseClicked() {
        val action =
            HomeFragmentDirections.actionHomePageToPersonalExpenseFragment(null, null)
        findNavController().navigate(action)
    }



    override fun onDestroyView() {
        super.onDestroyView()

        _binding = null
    }

    fun goToDetails(expenseList: List<FriendExpenseRecord>, email: String) {
        val action = HomeFragmentDirections.actionHomePageToFriendsAllExpenses(
            expenseList.toTypedArray(),
            email
        )
        findNavController().navigate(action)
    }
}

fun getCurrentDate(date:String): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val instant = Instant.ofEpochMilli(date.toLong())
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } else {
        val date = Date(date.toLong())
        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        format.format(date)
    }
}
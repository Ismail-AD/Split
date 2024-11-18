package com.appdev.split.UI

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.BillAdapter
import com.appdev.split.Model.Data.Bill
import com.appdev.split.Model.Data.TransactionItem
import com.appdev.split.databinding.FragmentHomeBinding


class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: BillAdapter


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val billList = listOf(
            Bill(
                "Lolita", "24 Sep, 2022", 279.55, "6 persons", 15.00,
                listOf(
                    TransactionItem("Dish 1", 50.0, 2),
                    TransactionItem("Dish 2", 60.0, 1),
                    TransactionItem("Drink", 20.0, 3)
                )
            ),
            Bill(
                "Rooms bar", "15 Sep, 2022", 120.0, "3 persons", 10.00,
                listOf(
                    TransactionItem("Room Fee", 100.0, 1),
                    TransactionItem("Service Fee", 20.0, 1)
                )
            ),
            Bill(
                "Tsota shop", "27 Aug, 2022", 176.40, "4 persons", 8.50,
                listOf(
                    TransactionItem("Item A", 50.0, 2),
                    TransactionItem("Item B", 38.2, 2)
                )
            ),
            Bill(
                "Pull&Bear", "20 Aug, 2022", 125.65, "1 person", 6.30,
                listOf(
                    TransactionItem("Shirt", 45.0, 1),
                    TransactionItem("Jeans", 80.65, 1)
                )
            ),
            Bill(
                "Market", "5 Aug, 2022", 300.75, "5 persons", 20.00,
                listOf(
                    TransactionItem("Groceries", 150.0, 2)
                )
            ),
            Bill(
                "Cafe Mocha", "10 Jul, 2022", 45.00, "2 persons", 3.50,
                listOf(
                    TransactionItem("Coffee", 5.0, 4),
                    TransactionItem("Sandwich", 10.0, 2)
                )
            ),
            Bill(
                "Library Fees", "22 Jun, 2022", 15.30, "1 person", 1.00,
                listOf(
                    TransactionItem("Membership Fee", 15.3, 1)
                )
            ),
            Bill(
                "Grocery", "18 May, 2022", 99.99, "3 persons", 5.00,
                listOf(
                    TransactionItem("Vegetables", 40.0, 1),
                    TransactionItem("Fruits", 59.99, 1)
                )
            ),
            Bill(
                "Electric Bill", "5 May, 2022", 89.50, "1 person", 2.00,
                listOf(
                    TransactionItem("Electricity Usage", 89.5, 1)
                )
            ),
            Bill(
                "Dinner Night", "3 Apr, 2022", 60.20, "2 persons", 4.00,
                listOf(
                    TransactionItem("Main Course", 30.0, 1),
                    TransactionItem("Dessert", 15.1, 2)
                )
            )
        )

        if (billList.isEmpty()) {
            binding.noBill.visibility = View.VISIBLE
            binding.recyclerViewRecentBills.visibility = View.GONE
        } else {
            binding.noBill.visibility = View.GONE
            binding.recyclerViewRecentBills.visibility = View.VISIBLE
            adapter = BillAdapter(billList, ::goToDetails)
            binding.recyclerViewRecentBills.adapter = adapter
            binding.recyclerViewRecentBills.layoutManager = LinearLayoutManager(requireContext())
        }

        // Set up the RecyclerView

    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

    fun goToDetails(bill: Bill) {
        val action = HomeFragmentDirections.actionHomePageToBillDetails(bill)
        findNavController().navigate(action)
    }
}
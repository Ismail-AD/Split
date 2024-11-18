package com.appdev.split

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.appdev.split.Adapters.TransactionItemAdapter
import com.appdev.split.Model.Data.Bill
import com.appdev.split.databinding.FragmentBillDetailsBinding

class BillDetails : Fragment() {

    private var _binding: FragmentBillDetailsBinding? = null
    private val binding get() = _binding!!

    private val args: BillDetailsArgs by navArgs()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentBillDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val bill: Bill = args.billData

        binding.rvTransactionItems.layoutManager = LinearLayoutManager(requireContext())
        binding.rvTransactionItems.adapter = TransactionItemAdapter(bill.listOfTransaction)

        binding.tvName.text = bill.name
        binding.tvDate.text = bill.date
        binding.tvSubtotal.text = bill.amount.toString()

        val total = bill.tax + bill.amount
        binding.tvTotal.text = total.toString()
    }
}
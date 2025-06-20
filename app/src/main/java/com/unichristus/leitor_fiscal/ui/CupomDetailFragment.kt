package com.unichristus.leitor_fiscal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.unichristus.leitor_fiscal.databinding.FragmentCupomDetailBinding
import com.unichristus.leitor_fiscal.ui.adapter.ProductAdapter
import com.unichristus.leitor_fiscal.ui.viewmodel.CupomDetailViewModel
import com.unichristus.leitor_fiscal.ui.viewmodel.CupomDetailViewModelFactory

class CupomDetailFragment : Fragment() {

    private var _binding: FragmentCupomDetailBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: CupomDetailViewModel
    private lateinit var productAdapter: ProductAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentCupomDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val cupomId = arguments?.getLong("cupomId") ?: -1L
        if (cupomId == -1L) {
            return
        }

        val factory = CupomDetailViewModelFactory(requireActivity().application, cupomId)
        viewModel = ViewModelProvider(this, factory).get(CupomDetailViewModel::class.java)

        setupRecyclerView()
        observeViewModel()
    }

    private fun setupRecyclerView() {
        productAdapter = ProductAdapter()
        binding.recyclerViewDetailProducts.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = productAdapter
        }
    }

    private fun observeViewModel() {
        viewModel.cupomDetails.observe(viewLifecycleOwner) { cupom ->
            cupom?.let {
                binding.textViewDetailStoreName.text = it.storeName
                binding.textViewDetailCnpj.text = "CNPJ: ${it.cnpj}"
            }
        }
        viewModel.products.observe(viewLifecycleOwner) { products ->
            productAdapter.submitList(products)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
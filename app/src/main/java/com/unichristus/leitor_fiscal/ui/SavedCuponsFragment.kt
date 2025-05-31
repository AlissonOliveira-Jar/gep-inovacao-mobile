package com.unichristus.leitor_fiscal.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.unichristus.leitor_fiscal.databinding.FragmentSavedCuponsBinding
import com.unichristus.leitor_fiscal.ui.adapter.SavedCupomAdapter
import com.unichristus.leitor_fiscal.ui.viewmodel.SavedCuponsViewModel

class SavedCuponsFragment : Fragment() {

    private var _binding: FragmentSavedCuponsBinding? = null
    private val binding get() = _binding!!

    private val viewModel: SavedCuponsViewModel by viewModels()
    private lateinit var savedCupomAdapter: SavedCupomAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedCuponsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        observeSavedCupons()
    }

    private fun setupRecyclerView() {
        savedCupomAdapter = SavedCupomAdapter { cupom ->
            Toast.makeText(context, "Cupom: ${cupom.storeName} - ID: ${cupom.id}", Toast.LENGTH_SHORT).show()
        }
        binding.recyclerViewSavedCupons.apply {
            layoutManager = LinearLayoutManager(context)
            adapter = savedCupomAdapter
        }
    }

    private fun observeSavedCupons() {
        viewModel.allSavedCupons.observe(viewLifecycleOwner) { cupons ->
            if (cupons.isNullOrEmpty()) {
                binding.recyclerViewSavedCupons.visibility = View.GONE
                binding.textViewNoSavedCupons.visibility = View.VISIBLE
            } else {
                binding.recyclerViewSavedCupons.visibility = View.VISIBLE
                binding.textViewNoSavedCupons.visibility = View.GONE
                savedCupomAdapter.submitList(cupons)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
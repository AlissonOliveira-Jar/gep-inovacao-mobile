package com.unichristus.leitor_fiscal.ui

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.navigation.fragment.findNavController
import com.unichristus.leitor_fiscal.R
import com.unichristus.leitor_fiscal.data.CupomInfo
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

    override fun onResume() {
        super.onResume()
        viewModel.loadSavedCupons()
        Log.d("SavedCuponsFragment", "onResume chamado, recarregando cupons.")
    }

    private fun setupRecyclerView() {
        savedCupomAdapter = SavedCupomAdapter(
            onItemClicked = { cupom ->
                cupom.id?.let { id ->
                    val action = SavedCuponsFragmentDirections.actionSavedCuponsToCupomDetail(id)
                    findNavController().navigate(action)
                }
            },
            onDeleteClicked = { cupom ->
                showDeleteConfirmationDialog(cupom)
            }
        )
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

    private fun showDeleteConfirmationDialog(cupom: CupomInfo) {
        AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Deleção")
            .setMessage("Tem certeza que deseja deletar o cupom de '${cupom.storeName}'?")
            .setPositiveButton("Deletar") { _, _ ->
                viewModel.deleteCupom(cupom)
                Toast.makeText(context, "Cupom deletado.", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ic_delete)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
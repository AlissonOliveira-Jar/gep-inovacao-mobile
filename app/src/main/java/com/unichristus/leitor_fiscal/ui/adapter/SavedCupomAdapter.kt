package com.unichristus.leitor_fiscal.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unichristus.leitor_fiscal.data.CupomInfo
import com.unichristus.leitor_fiscal.databinding.ListItemSavedCupomBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SavedCupomAdapter(private val onItemClicked: (CupomInfo) -> Unit) :
    ListAdapter<CupomInfo, SavedCupomAdapter.SavedCupomViewHolder>(SavedCupomDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedCupomViewHolder {
        val binding = ListItemSavedCupomBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return SavedCupomViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SavedCupomViewHolder, position: Int) {
        val cupom = getItem(position)
        holder.bind(cupom)
        holder.itemView.setOnClickListener {
            onItemClicked(cupom)
        }
    }

    class SavedCupomViewHolder(private val binding: ListItemSavedCupomBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private val dateTimeFormatter = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())

        fun bind(cupom: CupomInfo) {
            binding.textViewSavedStoreName.text = cupom.storeName ?: "Estabelecimento Desconhecido"

            binding.textViewSavedDateTime.text = cupom.scannedAtTimestamp?.let { timestamp ->
                dateTimeFormatter.format(Date(timestamp))
            } ?: "Data Indisponível"

            binding.textViewSavedTotalAmount.text = if (cupom.totalAmount != null) "Total: R$ ${cupom.totalAmount}" else "Total: N/A"
        }
    }

    class SavedCupomDiffCallback : DiffUtil.ItemCallback<CupomInfo>() {
        override fun areItemsTheSame(oldItem: CupomInfo, newItem: CupomInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CupomInfo, newItem: CupomInfo): Boolean {
            return oldItem == newItem
        }
    }
}
package com.unichristus.leitor_fiscal.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.unichristus.leitor_fiscal.data.Product
import com.unichristus.leitor_fiscal.databinding.ListItemProductBinding

class ProductAdapter : ListAdapter<Product, ProductAdapter.ProductViewHolder>(ProductDiffCallback()) {

    var onItemClick: ((Product) -> Unit)? = null

    class ProductViewHolder(
        private val binding: ListItemProductBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(product: Product) {
            binding.textViewProductName.text = product.name

            val detailsText = "${product.quantity} x ${product.unitPrice} = ${product.totalPrice}"
            binding.textViewProductDetails.text = detailsText

            if (product.discount.isNotEmpty() && product.discount != "0" && product.discount != "0.0") {
                binding.textViewProductDiscount.text = "Desconto: ${product.discount}"
                binding.textViewProductDiscount.visibility = View.VISIBLE
            } else {
                binding.textViewProductDiscount.visibility = View.GONE
            }
        }
    }

    private class ProductDiffCallback : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem.code == newItem.code && oldItem.totalPrice == newItem.totalPrice
        }

        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean {
            return oldItem == newItem
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val binding = ListItemProductBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ProductViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        val product = getItem(position)
        holder.bind(product)
    }
}
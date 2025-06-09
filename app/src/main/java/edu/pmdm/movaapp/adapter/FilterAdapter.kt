package edu.pmdm.movaapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import edu.pmdm.movaapp.R

class FilterAdapter(
    private val filters: List<String>,
    private val onFilterSelected: (String) -> Unit
) : RecyclerView.Adapter<FilterAdapter.FilterViewHolder>() {

    inner class FilterViewHolder(val chip: Chip) : RecyclerView.ViewHolder(chip)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FilterViewHolder {
        val chip = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_filter, parent, false) as Chip
        return FilterViewHolder(chip)
    }

    override fun onBindViewHolder(holder: FilterViewHolder, position: Int) {
        val filter = filters[position]
        holder.chip.text = filter
        holder.chip.setOnClickListener {
            onFilterSelected(filter)
        }
    }

    override fun getItemCount() = filters.size
}

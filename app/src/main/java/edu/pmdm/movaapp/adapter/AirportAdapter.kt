package edu.pmdm.movaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class AirportAdapter(
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<AirportAdapter.ViewHolder>() {

    private val airports = mutableListOf<String>()

    fun submitList(newList: List<String>) {
        airports.clear()
        airports.addAll(newList)
        notifyDataSetChanged()
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text = view.findViewById<TextView>(android.R.id.text1)

        init {
            view.setOnClickListener {
                onClick(airports[adapterPosition])
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(android.R.layout.simple_list_item_1, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = airports.size
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.text.text = airports[position]
    }
}

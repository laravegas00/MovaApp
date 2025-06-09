package edu.pmdm.movaapp.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import edu.pmdm.movaapp.R
import edu.pmdm.movaapp.databinding.ItemHotelBinding
import edu.pmdm.movaapp.models.Hotel

class HotelAdapter (
    private val onHotelClick: (Hotel) -> Unit
) : RecyclerView.Adapter<HotelAdapter.HotelViewHolder>() {

    private val hotels = mutableListOf<Hotel>()

    fun setHotels(newHotels: List<Hotel>) {
        hotels.clear()
        hotels.addAll(newHotels)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HotelViewHolder {
        val binding = ItemHotelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return HotelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HotelViewHolder, position: Int) {
        val hotel = hotels[position]
        holder.bind(hotel)
        holder.itemView.setOnClickListener { onHotelClick(hotel) }
    }

    override fun getItemCount(): Int = hotels.size

    inner class HotelViewHolder(private val binding: ItemHotelBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(hotel: Hotel) {
            binding.tvHotelName.text = hotel.hotel_name
            binding.tvHotelCity.text = "${hotel.city ?: ""}, ${hotel.country_trans ?: ""}"

            binding.tvHotelRating.text = hotel.review_score?.let {
                "⭐ %.1f (%d reseñas)".format(it, hotel.review_nr ?: 0)
            } ?: "No puntuation available"

            binding.tvHotelPrice.text = hotel.min_total_price?.let {
                "%.2f %s".format(it, hotel.currencycode ?: "")
            } ?: "Price not available"

            binding.tvHotelDistance.text = hotel.distance?.let {
                "Distance from city centre: ${it} km"
            } ?: "Distancia not available"


            Glide.with(binding.root.context)
                .load(hotel.main_photo_url)
                .placeholder(R.drawable.hotel)
                .centerCrop()
                .into(binding.ivHotelImage)
        }
    }
}

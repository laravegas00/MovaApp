package edu.pmdm.movaapp.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import edu.pmdm.movaapp.R
import edu.pmdm.movaapp.databinding.ItemReservaBinding
import edu.pmdm.movaapp.models.Reservation
import java.text.SimpleDateFormat
import java.util.*

class ReservationAdapter : RecyclerView.Adapter<ReservationAdapter.ReservaViewHolder>() {

    private var reservas: List<Reservation> = listOf()

    fun setReservas(nuevasReservas: List<Reservation>) {
        reservas = nuevasReservas
        notifyDataSetChanged()
    }

    inner class ReservaViewHolder(private val binding: ItemReservaBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(reserva: Reservation) {
            // Mostrar precio final
            binding.tvReservaPrecio.text = reserva.totalPrice ?: "--"

            // Comprobar si la reserva ha pasado
            val checkInDate = reserva.checkIn
            val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            if (checkInDate != null && checkInDate < today) {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        R.color.light
                    )
                )
            } else {
                binding.root.setBackgroundColor(
                    ContextCompat.getColor(
                        binding.root.context,
                        android.R.color.white
                    )
                )
            }

            // Mostrar u ocultar secciones según tipo
            when (reserva.type) {
                "flight" -> {
                    mostrarVuelo(reserva)
                    binding.layoutHotel.visibility = View.GONE
                }
                "hotel" -> {
                    binding.layoutVuelo.visibility = View.GONE
                    mostrarHotel(reserva)
                }
                "flight+hotel" -> {
                    mostrarVuelo(reserva)
                    mostrarHotel(reserva)
                }
                else -> {
                    binding.layoutVuelo.visibility = View.GONE
                    binding.layoutHotel.visibility = View.GONE
                }
            }
        }

        private fun mostrarVuelo(reserva: Reservation) {
            binding.layoutVuelo.visibility = View.VISIBLE

            binding.tvTituloVueloIda.visibility = View.VISIBLE
            binding.tvTituloVueloIda.text = "Outbound Flight"

            val from = reserva.outboundFlight?.get("origin")?.toString() ?: "Origin"
            val to = reserva.outboundFlight?.get("destination")?.toString() ?: "Destination"
            val departureOutbound = reserva.outboundFlight?.get("departureTime")?.toString() ?: "Departure"

            val departureOut = formatDateFlight(departureOutbound)

            binding.tvReservaRuta.text = "$from ➝ $to"
            binding.tvReservaFechaVuelo.text = departureOut

            reserva.returnFlight?.let { vuelo ->
                if (vuelo.isNotEmpty()) {
                    binding.layoutVueloVuelta.visibility = View.VISIBLE

                    binding.tvTituloVueloVuelta.visibility = View.VISIBLE
                    binding.tvTituloVueloVuelta.text = "Return Flight"

                    val from = reserva.returnFlight?.get("origin")?.toString() ?: "Origin"
                    val to = reserva.returnFlight?.get("destination")?.toString() ?: "Destination"
                    val departureOutbound =
                        reserva.returnFlight?.get("departureTime")?.toString() ?: "Departure"

                    val departureOut = formatDateFlight(departureOutbound)

                    binding.tvReservaRuta2.text = "$from ➝ $to"
                    binding.tvReservaFechaVuelo2.text = departureOut
                }
            }
        }

        private fun mostrarHotel(reserva: Reservation) {
            binding.layoutHotel.visibility = View.VISIBLE

            binding.tvTituloHotel.visibility = View.VISIBLE
            binding.tvTituloHotel.text = "Hotel"

            val hotel = reserva.hotel
            val nombreHotel = hotel?.get("name")?.toString() ?: "Hotel"

            val checkIn = reserva.hotel?.get("checkInDate")?.toString() ?: "Check-in"
            val checkOut = reserva.hotel?.get("checkOutDate")?.toString() ?: "Check-out"

            val checkInFormatted = formatDateHotel(checkIn)
            val checkOutFormatted = formatDateHotel(checkOut)

            binding.tvReservaHotel.text = nombreHotel
            binding.tvReservaFechaHotel.text = "$checkInFormatted - $checkOutFormatted"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReservaViewHolder {
        val binding =
            ItemReservaBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ReservaViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ReservaViewHolder, position: Int) {
        holder.bind(reservas[position])
    }

    override fun getItemCount(): Int = reservas.size

    fun formatDateHotel(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate!!)
        } catch (e: Exception) {
            "-"
        }
    }

    fun formatDateFlight(date: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val parsedDate = inputFormat.parse(date)
            outputFormat.format(parsedDate!!)
        } catch (e: Exception) {
            "-"
        }
    }
}

package edu.pmdm.movaapp.adapter
import FlightOffer
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import edu.pmdm.movaapp.R
import java.text.SimpleDateFormat
import java.util.Locale

class FlightAdapter : RecyclerView.Adapter<FlightAdapter.FlightViewHolder>() {

    private val flights = mutableListOf<FlightOffer>()
    private var onFlightSelected: ((FlightOffer) -> Unit)? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlightViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_flight, parent, false)
        return FlightViewHolder(view)
    }

    override fun getItemCount(): Int = flights.size

    override fun onBindViewHolder(holder: FlightViewHolder, position: Int) {
        holder.bind(flights[position])
    }

    fun setFlights(newFlights: List<FlightOffer>) {
        flights.clear()
        flights.addAll(newFlights)
        notifyDataSetChanged()
    }

    inner class FlightViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvDepartureTime = view.findViewById<TextView>(R.id.tvDepartureTime)
        private val tvArrivalTime = view.findViewById<TextView>(R.id.tvArrivalTime)
        private val tvDepartureCity = view.findViewById<TextView>(R.id.tvDepartureCity)
        private val tvArrivalCity = view.findViewById<TextView>(R.id.tvArrivalCity)
        private val tvDuration = view.findViewById<TextView>(R.id.tvFlightDuration)
        private val tvAirline = view.findViewById<TextView>(R.id.tvAirline)
        private val tvPrice = view.findViewById<TextView>(R.id.tvPrice)
        private val tvFlightType = view.findViewById<TextView>(R.id.tvFlightType)
        private val tvStopoverDetails = view.findViewById<TextView>(R.id.tvStopoverDetails)


        fun bind(flight: FlightOffer) {
            val itinerary = flight.itineraries[0]
            val firstSegment = itinerary.segments.first()
            val lastSegment = itinerary.segments.last()

            tvDepartureTime.text = formatTime(firstSegment.departure.at)
            tvArrivalTime.text = formatTime(lastSegment.arrival.at)
            tvDepartureCity.text = firstSegment.departure.iataCode
            tvArrivalCity.text = lastSegment.arrival.iataCode
            tvAirline.text = firstSegment.carrierCode
            tvDuration.text = formatDuration(itinerary.duration)
            tvPrice.text = "${flight.price.total} ${flight.price.currency}"

            if (flight.itineraries[0].segments.size > 1) {
                tvFlightType.text = "Con escalas"
                tvFlightType.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.informacion, 0)

                val segments = flight.itineraries[0].segments
                val stopoverDetails = StringBuilder()

                for (i in 0 until segments.size - 1) {
                    val arrival = segments[i].arrival
                    val nextDeparture = segments[i + 1].departure
                    val layoverMinutes = getMinutesBetween(arrival.at, nextDeparture.at)

                    stopoverDetails.append(
                        """
            Scale in: ${arrival.iataCode}
            Arrival: ${formatTime(arrival.at)}
            Departure: ${formatTime(nextDeparture.at)}
            Wait time: ${formatLayover(layoverMinutes)}
            Carrier: ${segments[i + 1].carrierCode}
            
            """.trimIndent()
                    )
                }

                tvStopoverDetails.text = stopoverDetails.toString()
                tvStopoverDetails.visibility = View.GONE

                tvFlightType.setOnClickListener {
                    val isVisible = tvStopoverDetails.visibility == View.VISIBLE
                    tvStopoverDetails.visibility = if (isVisible) View.GONE else View.VISIBLE
                    tvFlightType.setCompoundDrawablesWithIntrinsicBounds(
                        0,
                        0,
                        if (isVisible) R.drawable.informacion else R.drawable.informacion,
                        0
                    )
                }

            } else {
                tvFlightType.text = "Directo"
                tvStopoverDetails.visibility = View.GONE
            }

        }

        init {
            view.setOnClickListener {
                val flight = flights[adapterPosition]
                onFlightSelected?.invoke(flight)
            }
        }



        private fun getMinutesBetween(start: String, end: String): Int {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val startTime = format.parse(start)
            val endTime = format.parse(end)
            val diff = endTime.time - startTime.time
            return (diff / 60000).toInt()
        }

        private fun formatLayover(minutes: Int): String {
            val hours = minutes / 60
            val mins = minutes % 60
            return "${hours}h ${mins}min"
        }


        private fun formatTime(datetime: String): String {
            return try {
                val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
                val date = parser.parse(datetime)
                formatter.format(date!!)
            } catch (e: Exception) {
                datetime
            }
        }

        private fun formatDuration(duration: String): String {
            return duration.replace("PT", "")
                .replace("H", "h ")
                .replace("M", "min")
                .trim()
        }
    }

    fun setOnFlightSelectedListener(listener: (FlightOffer) -> Unit) {
        onFlightSelected = listener
    }

}

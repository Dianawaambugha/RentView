package com.example.rentview.ui.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.rentview.R
import com.example.rentview.data.PropertyItem
import com.example.rentview.ui.Formatters
import com.example.rentview.ui.ImageLoader

// Displays approved properties retrieved from Supabase.
class PropertyAdapter(
    private val onPropertyClicked: (PropertyItem) -> Unit
) : ListAdapter<PropertyItem, PropertyAdapter.PropertyViewHolder>(DIFF) {

    class PropertyViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val photo: ImageView = view.findViewById(R.id.ivPropertyPhoto)
        val title: TextView = view.findViewById(R.id.tvPropertyTitle)
        val location: TextView = view.findViewById(R.id.tvPropertyLocation)
        val bedrooms: TextView = view.findViewById(R.id.tvPropertyBedrooms)
        val rent: TextView = view.findViewById(R.id.tvPropertyRent)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PropertyViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_property, parent, false)

        return PropertyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: PropertyViewHolder,
        position: Int
    ) {
        val property = getItem(position)

        holder.title.text = property.title
        holder.location.text = property.location
        holder.bedrooms.text = Formatters.bedrooms(property.bedrooms)
        holder.rent.text = Formatters.rent(property.rent)

        val firstPhoto = property.photoUris.firstOrNull()

        if (firstPhoto.isNullOrBlank()) {
            holder.photo.visibility = View.GONE
            holder.photo.tag = null
        } else {
            holder.photo.visibility = View.VISIBLE
            ImageLoader.load(holder.photo, firstPhoto, 600)
        }

        holder.itemView.setOnClickListener {
            onPropertyClicked(property)
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<PropertyItem>() {

            override fun areItemsTheSame(
                old: PropertyItem,
                new: PropertyItem
            ): Boolean {
                // Cloud UUIDs identify properties across devices.
                if (!old.supabaseId.isNullOrBlank() &&
                    !new.supabaseId.isNullOrBlank()
                ) {
                    return old.supabaseId == new.supabaseId
                }

                // Temporary compatibility for local Room properties.
                return old.id == new.id
            }

            override fun areContentsTheSame(
                old: PropertyItem,
                new: PropertyItem
            ): Boolean {
                return old == new
            }
        }
    }
}
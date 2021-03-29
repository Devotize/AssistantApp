package com.sychev.assistantapp.presentation.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.sychev.assistantapp.R
import com.sychev.assistantapp.network.model.DetectedClothesDto

class DetectedClothesAdapter(private val clothes: ArrayList<DetectedClothesDto>, private val clickListener: OnItemClickListener):
    RecyclerView.Adapter<DetectedClothesAdapter.DetectedClothesViewHolder>() {

    interface OnItemClickListener{
        fun onItemClick(item: DetectedClothesDto)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectedClothesViewHolder {
        val layoutInflater = parent.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val itemView =  layoutInflater.inflate(R.layout.detected_clothes_item, parent, false)
        return DetectedClothesViewHolder(itemView = itemView)
    }

    override fun onBindViewHolder(holder: DetectedClothesViewHolder, position: Int) {
        holder.bind(clothes = clothes[position], clickListener)
    }

    override fun getItemCount(): Int {
        return clothes.size
    }


    class DetectedClothesViewHolder(
        private val itemView: View
    ): RecyclerView.ViewHolder(itemView){
        private val clothesImage = itemView.findViewById<ImageView>(R.id.detected_clothes_image_view_item)
        private val clothesName = itemView.findViewById<TextView>(R.id.detected_clothes_text_view_item)

        fun bind(clothes: DetectedClothesDto, clickListener: OnItemClickListener){
            Glide.with(itemView)
                .asBitmap()
                .load(clothes.image)
                .into(clothesImage)

            clothesName.text = clothes.clothesName
            itemView.setOnClickListener { clickListener.onItemClick(clothes) }
        }

    }

}







package com.example.psyconnectandroid

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ReviewsAdapter(
    private val reviews: List<Review>
) : RecyclerView.Adapter<ReviewsAdapter.ReviewViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        holder.bind(reviews[position])
    }

    override fun getItemCount(): Int = reviews.size

    inner class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val ivPhoto: ImageView = itemView.findViewById(R.id.ivReviewerPhoto)
        private val tvName: TextView = itemView.findViewById(R.id.tvReviewerName)
        private val ratingBar: RatingBar = itemView.findViewById(R.id.ratingBarReview)
        private val tvComment: TextView = itemView.findViewById(R.id.tvReviewComment)

        fun bind(review: Review) {
            tvName.text = review.reviewerName
            ratingBar.rating = review.rating.toFloat()
            tvComment.text = review.comment

            if (review.reviewerPhotoUrl.isNotEmpty()) {
                Glide.with(itemView.context)
                    .load(review.reviewerPhotoUrl)
                    .circleCrop()
                    .placeholder(R.drawable.ic_person)
                    .into(ivPhoto)
            } else {
                ivPhoto.setImageResource(R.drawable.ic_person)
            }
        }
    }
}
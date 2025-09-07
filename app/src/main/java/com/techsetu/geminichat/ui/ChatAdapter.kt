package com.techsetu.geminichat.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.techsetu.geminichat.R
import com.techsetu.geminichat.data.Message
import com.techsetu.geminichat.databinding.ItemMessageBotBinding
import com.techsetu.geminichat.databinding.ItemMessageUserBinding

class ChatAdapter : ListAdapter<Message, RecyclerView.ViewHolder>(DIFF) {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2

        val DIFF = object : DiffUtil.ItemCallback<Message>() {
            override fun areItemsTheSame(oldItem: Message, newItem: Message) = oldItem.id == newItem.id
            override fun areContentsTheSame(oldItem: Message, newItem: Message) = oldItem == newItem
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_USER) {
            val binding = ItemMessageUserBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            UserVH(binding)
        } else {
            val binding = ItemMessageBotBinding.inflate(LayoutInflater.from(parent.context), parent, false)
            BotVH(binding)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position)
        when (holder) {
            is UserVH -> holder.bind(item)
            is BotVH -> holder.bind(item)
        }
    }

    class UserVH(private val binding: ItemMessageUserBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.tvMessage.text = msg.text
        }
    }

    class BotVH(private val binding: ItemMessageBotBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(msg: Message) {
            binding.tvMessage.text = msg.text
        }
    }
}

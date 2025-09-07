package com.fahim.geminiapistarter.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.fahim.geminiapistarter.R
import com.fahim.geminiapistarter.Message

class ChatAdapter(
    private val items: MutableList<Message> = mutableListOf()
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2
    }

    fun add(message: Message) {
        items.add(message)
        notifyItemInserted(items.lastIndex)
    }

    override fun getItemViewType(position: Int): Int =
        if (items[position].isUser) TYPE_USER else TYPE_BOT

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == TYPE_USER) {
            UserVH(inflater.inflate(R.layout.item_message_user, parent, false))
        } else {
            BotVH(inflater.inflate(R.layout.item_message_bot, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val msg = items[position]
        when (holder) {
            is UserVH -> holder.bind(msg)
            is BotVH -> holder.bind(msg)
        }
    }

    override fun getItemCount(): Int = items.size

    class UserVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv = view.findViewById<TextView>(R.id.tvMessage)
        fun bind(m: Message) { tv.text = m.text }
    }

    class BotVH(view: View) : RecyclerView.ViewHolder(view) {
        private val tv = view.findViewById<TextView>(R.id.tvMessage)
        fun bind(m: Message) { tv.text = m.text }
    }

    fun clear() {
        items.clear()
        notifyDataSetChanged()
    }

}
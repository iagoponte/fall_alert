package com.quedaalerta

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.quedaalerta.databinding.ItemContactBinding
import org.json.JSONArray
import org.json.JSONObject

// ─── Data model ───────────────────────────────────────────────────────────────

data class EmergencyContact(
    val id   : String = java.util.UUID.randomUUID().toString(),
    val name : String,
    val phone: String
)

// ─── Storage manager ──────────────────────────────────────────────────────────

object ContactsManager {

    private const val PREFS_KEY = "contacts_json"

    fun getContacts(ctx: Context): List<EmergencyContact> {
        val prefs = ctx.getSharedPreferences("quedaalerta", Context.MODE_PRIVATE)
        val json  = prefs.getString(PREFS_KEY, "[]") ?: "[]"
        val arr   = JSONArray(json)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            EmergencyContact(
                id    = o.getString("id"),
                name  = o.getString("name"),
                phone = o.getString("phone")
            )
        }
    }

    fun addContact(ctx: Context, contact: EmergencyContact) {
        val list = getContacts(ctx).toMutableList()
        list.add(contact)
        saveContacts(ctx, list)
    }

    fun removeContact(ctx: Context, contact: EmergencyContact) {
        val list = getContacts(ctx).filter { it.id != contact.id }
        saveContacts(ctx, list)
    }

    private fun saveContacts(ctx: Context, contacts: List<EmergencyContact>) {
        val arr = JSONArray()
        contacts.forEach { c ->
            val o = JSONObject()
            o.put("id",    c.id)
            o.put("name",  c.name)
            o.put("phone", c.phone)
            arr.put(o)
        }
        ctx.getSharedPreferences("quedaalerta", Context.MODE_PRIVATE)
            .edit()
            .putString(PREFS_KEY, arr.toString())
            .apply()
    }
}

// ─── RecyclerView Adapter ─────────────────────────────────────────────────────

class ContactsAdapter(
    private val contacts: MutableList<EmergencyContact>,
    private val onDelete: (EmergencyContact) -> Unit
) : RecyclerView.Adapter<ContactsAdapter.ViewHolder>() {

    inner class ViewHolder(private val b: ItemContactBinding) :
        RecyclerView.ViewHolder(b.root) {

        fun bind(contact: EmergencyContact) {
            b.tvName.text    = contact.name
            b.tvPhone.text   = contact.phone
            b.tvInitials.text = contact.name.take(2).uppercase()
            b.btnDelete.setOnClickListener { onDelete(contact) }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemContactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) =
        holder.bind(contacts[position])

    override fun getItemCount() = contacts.size

    fun addContact(contact: EmergencyContact) {
        contacts.add(contact)
        notifyItemInserted(contacts.size - 1)
    }

    fun removeContact(contact: EmergencyContact) {
        val idx = contacts.indexOfFirst { it.id == contact.id }
        if (idx >= 0) {
            contacts.removeAt(idx)
            notifyItemRemoved(idx)
        }
    }
}

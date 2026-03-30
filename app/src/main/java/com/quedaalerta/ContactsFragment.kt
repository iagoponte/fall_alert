package com.quedaalerta

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.quedaalerta.databinding.FragmentContactsBinding

class ContactsFragment : Fragment() {

    private var _binding: FragmentContactsBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ContactsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        adapter = ContactsAdapter(ContactsManager.getContacts(requireContext()).toMutableList()) { contact ->
            showDeleteDialog(contact)
        }

        binding.rvContacts.layoutManager = LinearLayoutManager(requireContext())
        binding.rvContacts.adapter = adapter

        binding.fabAddContact.setOnClickListener {
            showAddContactDialog()
        }

        updateEmptyState()
    }

    private fun showAddContactDialog() {
        val dialogView = LayoutInflater.from(requireContext())
            .inflate(R.layout.dialog_add_contact, null)

        val etName  = dialogView.findViewById<EditText>(R.id.et_name)
        val etPhone = dialogView.findViewById<EditText>(R.id.et_phone)

        AlertDialog.Builder(requireContext())
            .setTitle("Adicionar contato")
            .setView(dialogView)
            .setPositiveButton("Salvar") { _, _ ->
                val name  = etName.text.toString().trim()
                val phone = etPhone.text.toString().trim()
                if (name.isNotEmpty() && phone.isNotEmpty()) {
                    val contact = EmergencyContact(name = name, phone = phone)
                    ContactsManager.addContact(requireContext(), contact)
                    adapter.addContact(contact)
                    updateEmptyState()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun showDeleteDialog(contact: EmergencyContact) {
        AlertDialog.Builder(requireContext())
            .setTitle("Remover contato")
            .setMessage("Deseja remover ${contact.name}?")
            .setPositiveButton("Remover") { _, _ ->
                ContactsManager.removeContact(requireContext(), contact)
                adapter.removeContact(contact)
                updateEmptyState()
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun updateEmptyState() {
        val isEmpty = adapter.itemCount == 0
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.rvContacts.visibility   = if (isEmpty) View.GONE   else View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

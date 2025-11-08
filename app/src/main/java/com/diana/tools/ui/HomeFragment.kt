package com.diana.tools.ui

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diana.tools.MainViewModel
import com.diana.tools.R
import com.diana.tools.databinding.FragmentHomeBinding

interface OnItemClickListener {
    fun onItemClick(position: Int)
}

class HomeFragment : Fragment(), OnItemClickListener {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private val mainViewModel: MainViewModel by activityViewModels()
    private val pickRootDirectoryLauncher = registerForActivityResult<Uri?, Uri?>(
        ActivityResultContracts.OpenDocumentTree()
    ) { directoryUri: Uri? ->
        if (directoryUri != null) {
            mainViewModel.updateRootDirUri(directoryUri)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val selectRootDirButton = binding.selectRootDirButton
        val recyclerCard = binding.recyclerCard
        val recyclerView = binding.recyclerView

        // show button or recyclerView
        if (mainViewModel.rootDirUri.value == null) {
            selectRootDirButton.visibility = View.VISIBLE
            recyclerCard.visibility = View.GONE
        } else {
            selectRootDirButton.visibility = View.GONE
            recyclerCard.visibility = View.VISIBLE
        }

        selectRootDirButton.setOnClickListener {
            pickRootDirectoryLauncher.launch(null)
        }
        mainViewModel.rootDirUri.observe(viewLifecycleOwner, Observer { newData ->
            if (newData != null) {
                selectRootDirButton.visibility = View.GONE
                recyclerCard.visibility = View.VISIBLE
            }
        })

        recyclerView.layoutManager = LinearLayoutManager(context)
        val toolListAdapter = ToolListAdapter(mutableListOf())
        toolListAdapter.setOnItemClickListener(this)
        recyclerView.adapter = toolListAdapter
        mainViewModel.toolDirList.observe(viewLifecycleOwner, Observer { newData ->
            newData?.let {
                toolListAdapter.updateData(it)
            }
        })

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onItemClick(position: Int) {
        Log.i("Item Click", position.toString())
        val resId = mainViewModel.idList.value?.get(position) ?: R.id.nav_home
        Log.i("home id", R.id.nav_home.toString())
        Log.i("ResId", resId.toString())
        val options = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setPopUpTo(R.id.nav_home, false) // 弹出 HomeFragment 之上的所有页面
            .build()
        findNavController().navigate(resId, null, options)
    }
}

class ToolListAdapter(private val itemList: MutableList<String>) :
    RecyclerView.Adapter<ToolListAdapter.ViewHolder>() {
    private var listener: OnItemClickListener? = null
    fun setOnItemClickListener(listener: OnItemClickListener) {
        this.listener = listener
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text_view_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_recycler_item, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val currentText = itemList[position]
        holder.textView.text = currentText

        holder.itemView.setOnClickListener {
            listener?.onItemClick(position)
        }
    }

    override fun getItemCount() = itemList.size

    fun updateData(newItems: List<String>) {
        itemList.clear()
        itemList.addAll(newItems)
        notifyDataSetChanged()
    }
}

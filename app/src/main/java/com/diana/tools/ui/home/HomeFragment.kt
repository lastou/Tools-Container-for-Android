package com.diana.tools.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.diana.tools.R
import com.diana.tools.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(requireActivity()).get(HomeViewModel::class.java)

        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val selectRootDirButton: Button = binding.selectRootDirButton
        selectRootDirButton.setOnClickListener {

        }

        val recyclerView = binding.recyclerView
        recyclerView.layoutManager = LinearLayoutManager(context)
        val toolListAdapter = ToolListAdapter(mutableListOf())
        recyclerView.adapter = toolListAdapter

        // --- 关键：观察 LiveData 的变化 ---
        homeViewModel.toolDirList.observe(viewLifecycleOwner, Observer { newData ->
            newData?.let {
                toolListAdapter.updateData(it) // 假设 Adapter 有 updateData 方法
            }
        })


        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}


// 你的 Adapter 需要一个更新数据的方法
class ToolListAdapter(private val itemList: MutableList<String>) :
    RecyclerView.Adapter<ToolListAdapter.ViewHolder>() {
    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textView: TextView = itemView.findViewById(R.id.text_view_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        // 加载 item 布局
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_recycler_item, parent, false)
        // 返回一个新的 ViewHolder
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        // 获取当前位置的数据
        val currentText = itemList[position]
        // 将数据设置到 ViewHolder 的控件上
        holder.textView.text = currentText
    }

    override fun getItemCount() = itemList.size

    fun updateData(newItems: List<String>) {
        // 清空旧数据
        itemList.clear()
        // 添加新数据
        itemList.addAll(newItems)
        // 通知 RecyclerView 数据集已改变，需要刷新整个列表
        notifyDataSetChanged()
    }

}

package com.arvind.diaryai.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.arvind.diaryai.data.Schema
import com.arvind.diaryai.data.StorageManager
import com.arvind.diaryai.databinding.FragmentDiaryRawBinding
import com.arvind.diaryai.util.DateRange

class DiaryRawFragment : Fragment() {
    private var _binding: FragmentDiaryRawBinding? = null
    private val binding get() = _binding!!
    private lateinit var storage: StorageManager
    private var range: DateRange = DateRange.forType(com.arvind.diaryai.util.RangeType.MONTHLY)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDiaryRawBinding.inflate(inflater, container, false)
        storage = StorageManager(requireContext())
        binding.rvDiaryCards.layoutManager = LinearLayoutManager(requireContext())
        return binding.root
    }

    fun updateRange(newRange: DateRange) {
        range = newRange
        if (view != null) render()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) { render() }

    private fun render() {
        val notes = storage.readRows(Schema.FILE_NOTES, Schema.NOTES_HEADER)
            .filter { range.contains(it["date"] ?: "") }
        binding.rvDiaryCards.adapter = GroupedCardAdapter(notes, "date") { row ->
            "${row["date"]} ${row["time"]}  •  ${row["type"]}" to (row["transcript"] ?: "")
        }
    }

    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}

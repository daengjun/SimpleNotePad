package com.simple.memo.ui.common

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.simple.memo.data.model.MemoEntity
import com.simple.memo.R

class TrashMemoBottomSheetDialogFragment(
    private val memo: MemoEntity,
    private val onRestoreClick: (MemoEntity) -> Unit,
    private val onDeleteClick: (MemoEntity) -> Unit
) : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_trash_memo_bottom_sheet, container, false)
        view.findViewById<TextView>(R.id.tv_restore).setOnClickListener {
            onRestoreClick(memo)
            dismiss()
        }

        view.findViewById<TextView>(R.id.tv_delete).setOnClickListener {
            onDeleteClick(memo)
            dismiss()
        }
        return view
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val bottomSheet = (dialog as BottomSheetDialog)
                .findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)

               /*
               * 태블릿 대응 코드
               * */
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
                behavior.skipCollapsed = true

                it.setBackgroundColor(Color.TRANSPARENT)
            }
        }
        return dialog
    }

    /*
     * 태블릿 바텀 시트
     * 가로 크기 꽉차게 설정
     * */
    override fun getTheme(): Int = R.style.CustomBottomSheetDialogTheme
}
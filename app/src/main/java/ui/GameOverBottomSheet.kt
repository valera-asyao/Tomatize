package ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import com.example.tomatize.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class GameOverBottomSheet : BottomSheetDialogFragment() {

    private var onConfirmListener: (() -> Unit)? = null

    fun setOnConfirmListener(listener: () -> Unit) {
        onConfirmListener = listener
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.bottom_sheet_game_over, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<Button>(R.id.btnOk).setOnClickListener {
            onConfirmListener?.invoke()
            dismiss()
        }
    }

    override fun getTheme(): Int = R.style.BottomSheetDialogTheme
}

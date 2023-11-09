package com.hawky.fr.view;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.LinearLayout;

import com.hawky.fr.R;

public class LoadingDialog extends Dialog {

    public LoadingDialog(Context context, boolean cancelable) {
        super(context, R.style.theme_dialog_loading);
        View view = View.inflate(context, R.layout.dialog_general_loading, null);
        LinearLayout layout = view.findViewById(R.id.ll_loading_dialog);
        setContentView(layout, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        setCancelable(cancelable);
    }

}

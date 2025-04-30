package com.digitallive.leddisplay.ui;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.digitallive.leddisplay.R;

import java.util.Objects;

public class LoadingDialog extends BaseActivity {

    private final Context context;
    private Dialog dialog;

    public LoadingDialog(Context context) {
        this.context = context;
    }

    public void show(boolean cancellable) {
        if (dialog != null) dialog.dismiss();

        dialog = new Dialog(context);
        dialog.setContentView(R.layout.view_loading_dialog);
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCancelable(false);
        dialog.setOnCancelListener(null);
        dialog.setCanceledOnTouchOutside(cancellable);
        dialog.show();
    }

    public void dismiss() {
        if (dialog != null) {
            dialog.dismiss();
        }
    }

    public void hide() {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }

    @Override
    protected void onDestroy() {
        dismiss();
        super.onDestroy();
    }
}

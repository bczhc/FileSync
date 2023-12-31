package pers.zhc.android.filesync;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import androidx.annotation.StringRes;
import org.jetbrains.annotations.NotNull;

public class ToastUtils {
    private static Toast toast;

    public static void show(Context ctx, @StringRes int strRes) {
        show(ctx, ctx.getString(strRes));
    }

    /**
     * Toast.
     *
     * @param ctx          context
     * @param charSequence string
     */
    public static synchronized void show(Context ctx, @NotNull CharSequence charSequence) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            Handler handler = new Handler(Looper.getMainLooper());
            handler.post(() -> uiThreadToast(ctx, charSequence));
        } else uiThreadToast(ctx, charSequence);
    }

    private static void uiThreadToast(Context ctx, @NotNull CharSequence charSequence) {
        if (toast != null) {
            toast.cancel();
        }
        toast = Toast.makeText(ctx, charSequence, Toast.LENGTH_SHORT);
        toast.show();
    }
}

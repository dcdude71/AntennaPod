package de.danoeh.antennapod.ui.screen.playlist;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ImageSpan;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.content.res.AppCompatResources;

import de.danoeh.antennapod.R;
import de.danoeh.antennapod.ui.common.ThemeUtils;

final class PlaylistKindUi {

    private PlaylistKindUi() {
    }

    static void bindTile(@NonNull ImageView view, boolean manual) {
        Context context = view.getContext();
        int padding = Math.round(8 * context.getResources().getDisplayMetrics().density);
        view.setImageResource(manual ? R.drawable.ic_playlist_kind_manual : R.drawable.ic_playlist_kind_smart);
        view.setBackgroundResource(manual ? R.drawable.bg_playlist_kind_manual : R.drawable.bg_playlist_kind_smart);
        view.setPadding(padding, padding, padding, padding);
        view.setImageTintList(ColorStateList.valueOf(ThemeUtils.getColorFromAttr(context,
                manual ? R.attr.colorPrimary : R.attr.colorOnPrimary)));
    }

    @NonNull
    static CharSequence withKind(@NonNull Context context, boolean manual, @NonNull CharSequence rest,
                                 float textSizePx) {
        String kind = context.getString(manual ? R.string.playlist_kind_manual : R.string.playlist_kind_smart);
        SpannableStringBuilder text = new SpannableStringBuilder();
        Drawable icon = AppCompatResources.getDrawable(context,
                manual ? R.drawable.ic_playlist_kind_manual : R.drawable.ic_playlist_kind_smart);
        if (icon != null) {
            icon = icon.mutate();
            int size = Math.round(textSizePx * 1.1f);
            icon.setBounds(0, 0, size, size);
            icon.setTint(ThemeUtils.getColorFromAttr(context, R.attr.colorPrimary));
            text.append(" ");
            text.setSpan(new ImageSpan(icon, ImageSpan.ALIGN_BOTTOM), 0, 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            text.append(" ");
        }
        text.append(context.getString(R.string.playlist_summary, kind, rest));
        return text;
    }
}

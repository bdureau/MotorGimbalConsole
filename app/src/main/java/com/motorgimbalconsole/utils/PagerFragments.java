package com.motorgimbalconsole.utils;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import java.util.function.Supplier;

/**
 * PagerFragments.java
 *
 * @author boris.dureau@neuf.fr
 *
 * Description: helper for the tabbed screens (ViewPager + FragmentPagerAdapter).
 *
 * The screens keep a field per tab (e.g. statusPage1) and call it directly to display live
 * values. When Android re-creates the activity (rotation, process restore), the
 * FragmentManager restores the tab fragments by itself and the adapter does NOT ask for new
 * ones. A screen that blindly created new fragments then held instances that were never
 * displayed: the visible tabs stopped updating. Each screen therefore first looks for the
 * restored instance with {@link #findRestored}, and only creates a new one when none exists.
 */
public final class PagerFragments {

    private PagerFragments() {
        // Static helper class: no instance
    }

    /**
     * Returns the fragment of the given class restored by the FragmentManager, if any.
     * The tabs of one screen all have different classes, so the class identifies the tab.
     *
     * @param fragmentManager fragment manager of the activity
     * @param type            class of the tab fragment
     * @param <T>             fragment type
     * @return the restored fragment, or null on a first creation
     */
    @Nullable
    public static <T extends Fragment> T findRestored(FragmentManager fragmentManager, Class<T> type) {
        for (Fragment fragment : fragmentManager.getFragments()) {
            if (type.isInstance(fragment)) {
                return type.cast(fragment);
            }
        }
        return null;
    }

    /**
     * Returns the restored fragment of the given class, or a new one built by the factory.
     *
     * @param fragmentManager fragment manager of the activity
     * @param type            class of the tab fragment
     * @param factory         builds the fragment when nothing was restored
     * @param <T>             fragment type
     * @return the fragment to bind to the tab
     */
    public static <T extends Fragment> T restoredOr(FragmentManager fragmentManager, Class<T> type,
                                                    Supplier<T> factory) {
        T restored = findRestored(fragmentManager, type);
        return (restored != null) ? restored : factory.get();
    }
}

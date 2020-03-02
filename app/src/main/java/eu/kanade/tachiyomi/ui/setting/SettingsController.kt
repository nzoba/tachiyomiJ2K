package eu.kanade.tachiyomi.ui.setting

import android.content.Context
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceController
import androidx.preference.PreferenceScreen
import com.bluelinelabs.conductor.ControllerChangeHandler
import com.bluelinelabs.conductor.ControllerChangeType
import eu.kanade.tachiyomi.R
import eu.kanade.tachiyomi.data.preference.PreferencesHelper
import eu.kanade.tachiyomi.ui.base.controller.BaseController
import eu.kanade.tachiyomi.util.view.RecyclerWindowInsetsListener
import eu.kanade.tachiyomi.util.view.updateLayoutParams
import rx.Observable
import rx.Subscription
import rx.subscriptions.CompositeSubscription
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

abstract class SettingsController : PreferenceController() {

    val preferences: PreferencesHelper = Injekt.get()

    var untilDestroySubscriptions = CompositeSubscription()
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle?): View {
        if (untilDestroySubscriptions.isUnsubscribed) {
            untilDestroySubscriptions = CompositeSubscription()
        }
        val view = super.onCreateView(inflater, container, savedInstanceState)
        view.updateLayoutParams<FrameLayout.LayoutParams> {
            val attrsArray = intArrayOf(android.R.attr.actionBarSize)
            val array = view.context.obtainStyledAttributes(attrsArray)
            topMargin = array.getDimensionPixelSize(0, 0)
            array.recycle()
        }
        listView.setOnApplyWindowInsetsListener(RecyclerWindowInsetsListener)
        return view
    }

    override fun onDestroyView(view: View) {
        super.onDestroyView(view)
        untilDestroySubscriptions.unsubscribe()
    }

    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        val screen = preferenceManager.createPreferenceScreen(getThemedContext())
        preferenceScreen = screen
        setupPreferenceScreen(screen)
    }

    abstract fun setupPreferenceScreen(screen: PreferenceScreen): Any?

    private fun getThemedContext(): Context {
        val tv = TypedValue()
        activity!!.theme.resolveAttribute(R.attr.preferenceTheme, tv, true)
        return ContextThemeWrapper(activity, tv.resourceId)
    }

    open fun getTitle(): String? {
        return preferenceScreen?.title?.toString()
    }

    fun setTitle() {
        var parentController = parentController
        while (parentController != null) {
            if (parentController is BaseController && parentController.getTitle() != null) {
                return
            }
            parentController = parentController.parentController
        }

        (activity as? AppCompatActivity)?.supportActionBar?.title = getTitle()
    }

    override fun onChangeStarted(handler: ControllerChangeHandler, type: ControllerChangeType) {
        if (type.isEnter) {
            setTitle()
        }
        super.onChangeStarted(handler, type)
    }

    fun <T> Observable<T>.subscribeUntilDestroy(): Subscription {
        return subscribe().also { untilDestroySubscriptions.add(it) }
    }

    fun <T> Observable<T>.subscribeUntilDestroy(onNext: (T) -> Unit): Subscription {
        return subscribe(onNext).also { untilDestroySubscriptions.add(it) }
    }
}

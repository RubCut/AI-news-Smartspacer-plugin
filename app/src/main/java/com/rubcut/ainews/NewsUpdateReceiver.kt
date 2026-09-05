package com.rubcut.ainews

import android.content.Context
import com.kieronquinn.app.smartspacer.sdk.provider.SmartspacerTargetProvider
import com.kieronquinn.app.smartspacer.sdk.receivers.SmartspacerTargetUpdateReceiver
import com.rubcut.ainews.targets.NewsTarget
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Periodic refresh requested by Smartspacer, per target instance. */
class NewsUpdateReceiver : SmartspacerTargetUpdateReceiver() {

    override fun onRequestSmartspaceTargetUpdate(
        context: Context,
        requestTargets: List<RequestTarget>
    ) {
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = SettingsRepository(context)
                coroutineScope {
                    requestTargets.distinctBy { it.smartspacerId }.map { request ->
                        async {
                            NewsUpdater.refresh(
                                context,
                                repository.forTarget(request.smartspacerId)
                            )
                        }
                    }.awaitAll()
                }
            } finally {
                requestTargets.forEach {
                    SmartspacerTargetProvider.notifyChange(
                        context, NewsTarget::class.java, it.smartspacerId
                    )
                }
                pendingResult.finish()
            }
        }
    }
}

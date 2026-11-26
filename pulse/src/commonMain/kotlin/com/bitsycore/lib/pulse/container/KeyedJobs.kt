package com.bitsycore.lib.pulse.container

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.getAndUpdate
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The jobs of a container, by key. Lock-free (the map is swapped by compare-and-set), so it is
 * safe from any thread without a dependency of its own.
 */
internal class KeyedJobs(private val scope: CoroutineScope) {

	private val mJobs = MutableStateFlow<Map<Any, List<Job>>>(emptyMap())

	fun latest(key: Any, block: suspend CoroutineScope.() -> Unit): Job {
		val vJob = prepare(key, block)
		val vBefore = mJobs.getAndUpdate { it + (key to listOf(vJob)) }
		vBefore[key]?.forEach { it.cancel() }
		return started(key, vJob)
	}

	fun once(key: Any, block: suspend CoroutineScope.() -> Unit): Job? {
		val vJob = prepare(key, block)
		var vTaken = false
		mJobs.update { vMap ->
			vTaken = vMap[key].orEmpty().none { !it.isCompleted }
			if (vTaken) vMap + (key to listOf(vJob)) else vMap
		}
		if (!vTaken) {
			vJob.cancel()
			return null
		}
		return started(key, vJob)
	}

	fun queued(key: Any, block: suspend CoroutineScope.() -> Unit): Job {
		var vPrevious: Job? = null
		val vJob = prepare(key) {
			vPrevious?.join()
			vPrevious = null
			block()
		}
		val vBefore = mJobs.getAndUpdate { it + (key to (it[key].orEmpty() + vJob)) }
		vPrevious = vBefore[key]?.lastOrNull()
		return started(key, vJob)
	}

	fun cancel(key: Any) {
		val vBefore = mJobs.getAndUpdate { it - key }
		vBefore[key]?.forEach { it.cancel() }
	}

	fun isRunning(key: Any): Boolean = mJobs.value[key].orEmpty().any { !it.isCompleted }

	private fun prepare(key: Any, block: suspend CoroutineScope.() -> Unit): Job {
		val vJob = scope.launch(start = CoroutineStart.LAZY, block = block)
		vJob.invokeOnCompletion { forget(key, vJob) }
		return vJob
	}

	private fun started(key: Any, job: Job): Job {
		job.start()
		// A scope already cancelled completes the job before it was registered.
		if (job.isCompleted) forget(key, job)
		return job
	}

	private fun forget(key: Any, job: Job) = mJobs.update { vMap ->
		val vList = vMap[key] ?: return@update vMap
		val vLeft = vList.filter { it !== job }
		when {
			vLeft.size == vList.size -> vMap
			vLeft.isEmpty() -> vMap - key
			else -> vMap + (key to vLeft)
		}
	}
}

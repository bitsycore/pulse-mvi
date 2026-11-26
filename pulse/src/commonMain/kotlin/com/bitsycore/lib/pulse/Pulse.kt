package com.bitsycore.lib.pulse

import com.bitsycore.lib.pulse.container.PulseObserver
import kotlin.concurrent.Volatile

/** Library-wide settings. */
public object Pulse {
	/** Sees what every container does, on top of a container's own observer. */
	@Volatile
	public var observer: PulseObserver? = null
}

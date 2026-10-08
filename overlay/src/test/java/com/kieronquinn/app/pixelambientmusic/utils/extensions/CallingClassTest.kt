package com.kieronquinn.app.pixelambientmusic.utils.extensions

import org.junit.Assert.*
import org.junit.Test

class CallingClassTest {
    private fun frame(name: String) = StackTraceElement(name, "invoke", "test", 1)
    @Test fun findsLsplantCaller() {
        assertEquals("qdj", getCallingClassName(arrayOf(frame("hook"), frame("LSPHooker_"), frame("qdj"))))
    }
    @Test fun findsPineCallerAcrossBridgeFrames() {
        assertEquals("qdj", getCallingClassName(arrayOf(frame("hook"),
            frame("top.canyie.pine.Pine"), frame("top.canyie.pine.entry.Arm64Entry"),
            frame("top.canyie.pine.entry.Arm64Entry"), frame("qdj"))))
    }
    @Test fun unknownOrTruncatedStacksReturnNull() {
        assertNull(getCallingClassName(emptyArray()))
        assertNull(getCallingClassName(arrayOf(frame("qdj"))))
        assertNull(getCallingClassName(arrayOf(frame("LSPHooker_"))))
    }
    @Test fun reflectionFramesDoNotBecomeTheCaller() {
        assertEquals("qdj", getCallingClassName(arrayOf(frame("LSPHooker_"),
            frame("java.lang.reflect.Method"), frame("qdj"))))
    }
}

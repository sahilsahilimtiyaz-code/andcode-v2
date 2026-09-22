package com.yugahashimoto.andcode.ui

/**
 * Compatibility alias for the cross-runtime session reference used by the app drawer.
 * The model lives in the data layer; keeping this alias avoids duplicating its fields and
 * preserves the existing drawer behavior.
 */
typealias RuntimeSessionRef = com.yugahashimoto.andcode.data.repository.RuntimeSessionRef

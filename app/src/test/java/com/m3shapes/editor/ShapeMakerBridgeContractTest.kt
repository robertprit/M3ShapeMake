package com.m3shapes.editor

import org.junit.Assert.assertEquals
import org.junit.Test

class ShapeMakerBridgeContractTest {
    @Test
    fun bridgeContractUsesStableActionAndExtras() {
        assertEquals(
            "com.m3shapes.editor.action.CREATE_VISUAL_ASSET",
            ShapeMakerBridgeContract.ACTION_CREATE_VISUAL_ASSET,
        )
        assertEquals(
            "com.m3shapes.editor.extra.REQUESTED_ASSET_TYPE",
            ShapeMakerBridgeContract.EXTRA_REQUESTED_ASSET_TYPE,
        )
    }
}

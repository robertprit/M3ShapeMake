package com.compose.canvas.geometry

import java.util.UUID

typealias ObjectId = String

fun newObjectId(): ObjectId = UUID.randomUUID().toString()

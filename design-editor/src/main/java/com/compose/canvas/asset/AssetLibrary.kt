package com.compose.canvas.asset

import java.io.File

data class AssetLibraryQuery(
    val text: String = "",
    val tags: Set<String> = emptySet(),
    val minimumVersion: Int = 1
)

interface AssetLibrary {
    fun save(asset: VisualAsset)
    fun get(assetId: String, version: Int? = null): VisualAsset?
    fun search(query: AssetLibraryQuery = AssetLibraryQuery()): List<VisualAsset>
    fun versions(assetId: String): List<Int>
    fun remove(assetId: String, version: Int): Boolean
}

class InMemoryAssetLibrary : AssetLibrary {
    private val assets = linkedMapOf<Pair<String, Int>, VisualAsset>()

    override fun save(asset: VisualAsset) {
        require(EmaCodec.validate(asset).valid) { "Only valid assets can be added to the library" }
        assets[asset.identity.assetId to asset.identity.version] = asset
    }

    override fun get(assetId: String, version: Int?): VisualAsset? =
        if (version != null) assets[assetId to version]
        else assets.filterKeys { it.first == assetId }.maxByOrNull { it.key.second }?.value

    override fun search(query: AssetLibraryQuery): List<VisualAsset> {
        val needle = query.text.trim().lowercase()
        return assets.values.asSequence()
            .filter { it.identity.version >= query.minimumVersion }
            .filter { query.tags.isEmpty() || it.identity.tags.containsAll(query.tags) }
            .filter {
                needle.isEmpty() || it.identity.name.lowercase().contains(needle) ||
                    it.identity.assetId.lowercase().contains(needle) ||
                    it.identity.tags.any { tag -> tag.lowercase().contains(needle) }
            }
            .sortedWith(compareBy<VisualAsset> { it.identity.name.lowercase() }.thenByDescending { it.identity.version })
            .toList()
    }

    override fun versions(assetId: String): List<Int> = assets.keys
        .filter { it.first == assetId }
        .map { it.second }
        .sortedDescending()

    override fun remove(assetId: String, version: Int): Boolean = assets.remove(assetId to version) != null
}

class DirectoryAssetLibrary(private val directory: File) : AssetLibrary {
    init { directory.mkdirs() }

    override fun save(asset: VisualAsset) {
        require(EmaCodec.validate(asset).valid) { "Only valid assets can be added to the library" }
        file(asset.identity.assetId, asset.identity.version).writeText(EmaCodec.encode(asset))
    }

    override fun get(assetId: String, version: Int?): VisualAsset? {
        val resolvedVersion = version ?: versions(assetId).firstOrNull() ?: return null
        return file(assetId, resolvedVersion).takeIf(File::isFile)?.readText()?.let(EmaCodec::decode)
    }

    override fun search(query: AssetLibraryQuery): List<VisualAsset> = files().mapNotNull {
        runCatching { EmaCodec.decode(it.readText()) }.getOrNull()
    }.filter { asset ->
        val needle = query.text.trim().lowercase()
        asset.identity.version >= query.minimumVersion &&
            (query.tags.isEmpty() || asset.identity.tags.containsAll(query.tags)) &&
            (needle.isEmpty() || asset.identity.name.lowercase().contains(needle) ||
                asset.identity.assetId.lowercase().contains(needle) ||
                asset.identity.tags.any { it.lowercase().contains(needle) })
    }.sortedWith(compareBy<VisualAsset> { it.identity.name.lowercase() }.thenByDescending { it.identity.version })

    override fun versions(assetId: String): List<Int> = files()
        .mapNotNull { entryPattern.matchEntire(it.name)?.destructured }
        .filter { it.component1() == safe(assetId) }
        .mapNotNull { it.component2().toIntOrNull() }
        .sortedDescending()

    override fun remove(assetId: String, version: Int): Boolean = file(assetId, version).delete()

    private fun files(): List<File> = directory.listFiles { file -> file.isFile && file.extension == EMA_FILE_EXTENSION }
        ?.toList().orEmpty()

    private fun file(assetId: String, version: Int) = File(directory, "${safe(assetId)}--$version.$EMA_FILE_EXTENSION")
    private fun safe(value: String) = value.replace(Regex("[^A-Za-z0-9._-]"), "_")

    private companion object {
        val entryPattern = Regex("(.+)--(\\d+)\\.$EMA_FILE_EXTENSION")
    }
}

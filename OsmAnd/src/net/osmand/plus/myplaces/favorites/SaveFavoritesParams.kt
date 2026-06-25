package net.osmand.plus.myplaces.favorites

import net.osmand.plus.myplaces.favorites.SaveFavoritesTask.SaveFavoritesListener

class SaveFavoritesParams(
	val groups: List<FavoriteGroup>,
	val saveAllGroups: Boolean,
	listeners: Collection<SaveFavoritesListener?>
) {

	constructor(
		groups: List<FavoriteGroup>,
		saveAllGroups: Boolean,
		listener: SaveFavoritesListener?
	) : this(groups, saveAllGroups, listOfNotNull(listener))

	val listeners: LinkedHashSet<SaveFavoritesListener> = listeners
		.filterNotNullTo(LinkedHashSet())

	fun merge(newer: SaveFavoritesParams): SaveFavoritesParams {
		val mergedSaveAll = saveAllGroups || newer.saveAllGroups

		// Always merge maps to guarantee that newer group data overwrites older data.
		// If older had saveAllGroups=true, it provides the base list.
		// Newer groups will overwrite the specific ones that were recently modified.
		val seen = LinkedHashMap<String, FavoriteGroup>()
		for (g in groups) seen[g.name] = g
		for (g in newer.groups) seen[g.name] = g
		val mergedGroups = seen.values.toList()

		val mergedListeners = LinkedHashSet<SaveFavoritesListener>(listeners)
		mergedListeners.addAll(newer.listeners)

		return SaveFavoritesParams(mergedGroups, mergedSaveAll, mergedListeners)
	}
}
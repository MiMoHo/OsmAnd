package net.osmand.plus.myplaces.favorites

class PendingFavoriteDeletions {

	val pointKeys: MutableSet<String> = HashSet()
	val groupNames: MutableSet<String> = HashSet()

	val isEmpty: Boolean
		get() = pointKeys.isEmpty() && groupNames.isEmpty()

	fun deserializeLine(line: String) {
		when {
			line.startsWith(PREFIX_POINT) -> pointKeys.add(line.removePrefix(PREFIX_POINT))
			line.startsWith(PREFIX_GROUP) -> groupNames.add(line.removePrefix(PREFIX_GROUP))
		}
	}

	companion object {
		const val PREFIX_POINT = "point:"
		const val PREFIX_GROUP = "group:"

		@JvmStatic
		fun serializePoint(pointKey: String): String = "$PREFIX_POINT$pointKey"

		@JvmStatic
		fun serializeGroup(groupName: String): String = "$PREFIX_GROUP$groupName"
	}
}
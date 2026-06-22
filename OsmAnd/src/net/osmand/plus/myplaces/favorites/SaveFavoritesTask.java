package net.osmand.plus.myplaces.favorites;

import static net.osmand.IndexConstants.ZIP_EXT;
import static net.osmand.plus.myplaces.favorites.FavouritesHelper.getPointsFromGroups;

import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.PlatformUtil;
import net.osmand.data.FavouritePoint;
import net.osmand.plus.shared.SharedUtil;
import net.osmand.shared.gpx.GpxFile;
import net.osmand.util.Algorithms;

import org.apache.commons.logging.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class SaveFavoritesTask extends AsyncTask<Void, String, Void> {

	private static final Log log = PlatformUtil.getLog(SaveFavoritesTask.class);

	private final FavouritesFileHelper helper;
	private final SaveFavoritesParams params;

	public SaveFavoritesTask(@NonNull FavouritesFileHelper helper,
	                         @NonNull SaveFavoritesParams params) {
		this.helper = helper;
		this.params = params;
	}

	@NonNull
	public SaveFavoritesParams getParams() {
		return params;
	}

	@Override
	protected Void doInBackground(Void... voids) {
		if (params.getSaveAllGroups()) {
			saveAllGroups(params.getGroups());
		} else {
			saveSelectedGroupsOnly(params.getGroups());
		}
		return null;
	}

	private void saveAllGroups(@NonNull List<FavoriteGroup> groups) {
		try {
			Map<String, FavoriteGroup> deletedGroups = new LinkedHashMap<>();
			Map<String, FavouritePoint> deletedPoints = new LinkedHashMap<>();

			if (isCancelled()) {
				return;
			}
			File internalFile = helper.getInternalFile();
			GpxFile gpxFile = SharedUtil.loadGpxFile(internalFile);
			if (gpxFile.getError() == null) {
				helper.collectFavoriteGroups(gpxFile, deletedGroups);
			}
			for (FavoriteGroup group : deletedGroups.values()) {
				for (FavouritePoint point : group.getPoints()) {
					deletedPoints.put(point.getKey(), point);
				}
			}
			for (FavouritePoint point : getPointsFromGroups(groups)) {
				deletedPoints.remove(point.getKey());
			}
			for (FavoriteGroup group : groups) {
				deletedGroups.remove(group.getName());
			}

			// The heaviest operation: skip if a newer task is already queued.
			if (isCancelled()) {
				return;
			}
			helper.saveFile(groups, internalFile);

			if (isCancelled()) {
				return;
			}
			saveExternalFiles(groups, deletedPoints.keySet());

			// All writes succeeded — safe to clear tombstone.
			// Backup is best-effort and doesn't affect data integrity.
			helper.clearPendingDeletions();

			if (isCancelled()) {
				return;
			}
			backup(helper.getBackupFile(), internalFile);

		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void saveSelectedGroupsOnly(@NonNull List<FavoriteGroup> groupsToSave) {
		try {
			for (FavoriteGroup group : groupsToSave) {
				if (isCancelled()) {
					return;
				}
				saveFavoriteGroup(group);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	private void loadGPXFiles(@NonNull Map<String, FavoriteGroup> favoriteGroups) {
		File[] files = helper.getFavoritesFiles();
		if (!Algorithms.isEmpty(files)) {
			for (File file : files) {
				if (isCancelled()) {
					return;
				}
				GpxFile gpxFile = SharedUtil.loadGpxFile(file);
				if (gpxFile.getError() == null) {
					helper.collectFavoriteGroups(gpxFile, favoriteGroups);
				}
			}
		}
	}

	private void saveExternalFiles(@NonNull List<FavoriteGroup> localGroups,
	                               @NonNull Set<String> deleted) {
		Map<String, FavoriteGroup> fileGroups = new LinkedHashMap<>();
		loadGPXFiles(fileGroups);
		if (isCancelled()) {
			return;
		}
		cleanupOrphanedGroupFiles(localGroups, fileGroups);
		saveLocalGroups(localGroups, fileGroups, deleted);
	}

	private void cleanupOrphanedGroupFiles(@NonNull List<FavoriteGroup> localGroups,
	                                       @NonNull Map<String, FavoriteGroup> fileGroups) {
		for (FavoriteGroup fileGroup : fileGroups.values()) {
			// Search corresponding group in memory
			boolean hasLocalGroup = false;
			for (FavoriteGroup group : localGroups) {
				if (Algorithms.stringsEqual(group.getName(), fileGroup.getName())) {
					hasLocalGroup = true;
					break;
				}
			}
			// Delete external group file if it does not exist in local groups
			if (!hasLocalGroup) {
				helper.getExternalFile(fileGroup).delete();
			}
		}
	}

	private void saveLocalGroups(@NonNull List<FavoriteGroup> localGroups,
	                             @NonNull Map<String, FavoriteGroup> fileGroups,
	                             @NonNull Set<String> deleted) {
		for (FavoriteGroup localGroup : localGroups) {
			if (isCancelled()) {
				return;
			}
			FavoriteGroup fileGroup = fileGroups.get(localGroup.getName());
			Map<String, FavouritePoint> all = new LinkedHashMap<>();
			if (fileGroup != null) {
				for (FavouritePoint point : fileGroup.getPoints()) {
					String key = point.getKey();
					if (!deleted.contains(key)) {
						all.put(key, point);
					}
				}
			}
			// Build merged list without mutating localGroup.getPoints() mid-iteration
			List<FavouritePoint> localPoints = new ArrayList<>(localGroup.getPoints());
			for (FavouritePoint point : localPoints) {
				all.remove(point.getKey());
			}
			if (!all.isEmpty()) {
				// Only add extra points from file that aren't already in memory
				localGroup.getPoints().addAll(all.values());
			}
			if (!localGroup.equals(fileGroup)) {
				saveFavoriteGroup(localGroup);
			}
		}
	}

	private void saveFavoriteGroup(@NonNull FavoriteGroup group) {
		File externalFile = helper.getExternalFile(group);
		Exception exception = helper.saveFile(Collections.singletonList(group), externalFile);
		if (exception != null) {
			log.error(exception);
		} else if (externalFile.exists()) {
			group.setSize(externalFile.length());
			group.setTimeModified(externalFile.lastModified());
		}
	}

	private void backup(@NonNull File backupFile, @NonNull File externalFile) {
		String name = backupFile.getName();
		String nameNoExt = name.substring(0, name.lastIndexOf(ZIP_EXT));
		InputStream fis = null;
		ZipOutputStream zos = null;
		try {
			File file = new File(backupFile.getParentFile(), backupFile.getName());
			zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
			fis = new BufferedInputStream(new FileInputStream(externalFile));
			zos.putNextEntry(new ZipEntry(nameNoExt));
			Algorithms.streamCopy(fis, zos);
			zos.closeEntry();
			zos.flush();
			zos.finish();
		} catch (Exception e) {
			log.warn("Backup failed", e);
		} finally {
			Algorithms.closeStream(zos);
			Algorithms.closeStream(fis);
		}
		helper.clearOldBackups();
	}

	@Override
	protected void onPostExecute(Void result) {
		helper.onSaveTaskFinished(this);
		// Only the final non-cancelled task should trigger UI updates.
		if (!isCancelled()) {
			for (SaveFavoritesListener listener : params.getListeners()) {
				listener.onSavingFavoritesFinished();
			}
		}
	}

	@Override
	protected void onCancelled() {
		// Release the reference even for cancelled tasks.
		helper.onSaveTaskFinished(this);
	}

	public interface SaveFavoritesListener {
		void onSavingFavoritesFinished();
	}
}
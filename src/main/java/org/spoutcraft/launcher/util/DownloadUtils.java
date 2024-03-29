/*
 * This file is part of Launcher (http://www.spout.org/).
 *
 * Launcher is licensed under the SpoutDev License Version 1.
 *
 * Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * In addition, 180 days after any changes are published, you can use the
 * software, incorporating those changes, under the terms of the MIT license,
 * as described in the SpoutDev License Version 1.
 *
 * Launcher is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License,
 * the MIT license and the SpoutDev License Version 1 along with this program.
 * If not, see <http://www.gnu.org/licenses/> for the GNU Lesser General Public
 * License and see <http://www.spout.org/SpoutDevLicenseV1.txt> for the full license,
 * including the MIT license.
 */
package org.spoutcraft.launcher.util;

import java.io.File;
import java.io.IOException;

import org.spoutcraft.launcher.Settings;
import org.spoutcraft.launcher.api.Launcher;
import org.spoutcraft.launcher.api.util.Download;
import org.spoutcraft.launcher.api.util.DownloadListener;
import org.spoutcraft.launcher.api.util.Utils;

public class DownloadUtils {
	public static Download downloadFile(String url, String output, String cacheName, String md5, DownloadListener listener) throws IOException {
		int tries = Settings.getLoginTries();
		File outputFile = null;
		Download download = null;
		while (tries > 0) {
			System.out.println("Starting download of " + url + ", with " + tries + " tries remaining");
			tries--;
			download = new Download(url, output);
			download.setListener(listener);
			download.run();
			if (!download.isSuccess()) {
				if (download.getOutFile() != null) {
					download.getOutFile().delete();
				}
				System.err.println("Download of " + url + " Failed!");
				if (listener != null)
					listener.stateChanged("Download Failed, retries remaining: " + tries, 0F);
			} else {
				if (md5 != null) {
					String resultMD5 = MD5Utils.getMD5(download.getOutFile());
					System.out.println("Expected MD5: " + md5 + " Calculated MD5: " + resultMD5);
					if (resultMD5.equals(md5)) {
						outputFile = download.getOutFile();
						break;
					}
				} else {
					outputFile = download.getOutFile();
					break;
				}
			}
		}
		if (outputFile == null) {
			throw new IOException("Failed to download " + url);
		}
		if (cacheName != null) {
			Utils.copy(outputFile, new File(Launcher.getGameUpdater().getBinCacheDir(), cacheName));
		}
		return download;
	}

	public static Download downloadFile(String url, String output, String cacheName) throws IOException {
		return downloadFile(url, output, cacheName, null, null);
	}

	public static Download downloadFile(String url, String output) throws IOException {
		return downloadFile(url, output, null);
	}
}

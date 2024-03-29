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
package org.spoutcraft.diff;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Java Binary patcher (based on bspatch by Colin Percival)
 *
 * @author Joe Desbonnet, joe@galway.net
 */
public class JBPatch {
	@SuppressWarnings("unused")
	private static final String VERSION = "jbdiff-0.1.0";

	/**
	 * Run JBPatch from the command line. Params: oldfile newfile patchfile.
	 * newfile will be created.
	 *
	 * @param arg
	 * @throws IOException
	 */
	public static void main(String[] arg) throws IOException {
		if (arg.length != 3) {
			System.err.println("usage example: java -Xmx200m ie.wombat.jbdiff.JBPatch oldfile newfile patchfile");
		}

		File oldFile = new File(arg[0]);
		File newFile = new File(arg[1]);
		File diffFile = new File(arg[2]);

		bspatch(oldFile, newFile, diffFile);
	}

	@SuppressWarnings("unused")
	public static void bspatch(File oldFile, File newFile, File diffFile)
			throws IOException {
		int oldpos, newpos;

		DataInputStream diffIn = new DataInputStream(new FileInputStream(diffFile));

		// headerMagic at header offset 0 (length 8 bytes)
		long headerMagic = diffIn.readLong();

		// ctrlBlockLen after gzip compression at heater offset 8 (length 8 bytes)
		long ctrlBlockLen = diffIn.readLong();

		// diffBlockLen after gzip compression at header offset 16 (length 8 bytes)
		long diffBlockLen = diffIn.readLong();

		// size of new file at header offset 24 (length 8 bytes)
		int newsize = (int) diffIn.readLong();

		/*
		System.err.println ("newsize=" + newsize);
		System.err.println ("ctrlBlockLen=" + ctrlBlockLen);
		System.err.println ("diffBlockLen=" + diffBlockLen);
		System.err.println ("newsize=" + newsize);
		*/

		FileInputStream in;
		in = new FileInputStream(diffFile);
		in.skip(ctrlBlockLen + 32);
		GZIPInputStream diffBlockIn = new GZIPInputStream(in);

		in = new FileInputStream(diffFile);
		in.skip(diffBlockLen + ctrlBlockLen + 32);
		GZIPInputStream extraBlockIn = new GZIPInputStream(in);

		/*
		 * Read in old file (file to be patched) to oldBuf
		 */
		int oldsize = (int) oldFile.length();
		byte[] oldBuf = new byte[oldsize + 1];
		FileInputStream oldIn = new FileInputStream(oldFile);
		Util.readFromStream(oldIn, oldBuf, 0, oldsize);
		oldIn.close();

		byte[] newBuf = new byte[newsize + 1];

		oldpos = 0;
		newpos = 0;
		int[] ctrl = new int[3];
		int nbytes;
		while (newpos < newsize) {
			for (int i = 0; i <= 2; i++) {
				ctrl[i] = diffIn.readInt();
				//System.err.println ("  ctrl[" + i + "]=" + ctrl[i]);
			}

			if (newpos + ctrl[0] > newsize) {
				System.err.println("Corrupt patch\n");
				return;
			}

			/*
			 * Read ctrl[0] bytes from diffBlock stream
			 */
			if (!Util.readFromStream(diffBlockIn, newBuf, newpos, ctrl[0])) {
				System.err.println("error reading from extraIn");
				return;
			}

			for (int i = 0; i < ctrl[0]; i++) {
				if ((oldpos + i >= 0) && (oldpos + i < oldsize)) {
					newBuf[newpos + i] += oldBuf[oldpos + i];
				}
			}

			newpos += ctrl[0];
			oldpos += ctrl[0];

			if (newpos + ctrl[1] > newsize) {
				System.err.println("Corrupt patch");
				return;
			}

			if (!Util.readFromStream(extraBlockIn, newBuf, newpos, ctrl[1])) {
				System.err.println("error reading from extraIn");
				return;
			}

			newpos += ctrl[1];
			oldpos += ctrl[2];
		}

		// TODO: Check if at end of ctrlIn
		// TODO: Check if at the end of diffIn
		// TODO: Check if at the end of extraIn

		diffBlockIn.close();
		extraBlockIn.close();
		diffIn.close();

		FileOutputStream out = new FileOutputStream(newFile);
		out.write(newBuf, 0, newBuf.length - 1);
		out.close();
	}
}

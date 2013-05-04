/*
This source code file is part of the CASAA Treatment Coding System Utility
    Copyright (C) 2009  UNM CASAA

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package edu.unm.casaa.main;

/**
 * Application version info.
 * 
 * @author UNM CASAA
 */
public class Version {
	public static final int MAJOR 		= 0;
	public static final int MINOR 		= 10;
	public static final int REVISION 	= 0;

	public static String versionString() {
		return "" + MAJOR + "." + MINOR + "." + REVISION;
	}
}

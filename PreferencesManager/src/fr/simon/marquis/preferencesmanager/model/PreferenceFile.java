/*
 * Copyright (C) 2013 Simon Marquis (http://www.simon-marquis.fr)
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package fr.simon.marquis.preferencesmanager.model;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.xmlpull.v1.XmlPullParserException;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import com.spazedog.lib.rootfw.container.FileStat;

import fr.simon.marquis.preferencesmanager.ui.App;
import fr.simon.marquis.preferencesmanager.util.Utils;
import fr.simon.marquis.preferencesmanager.util.XmlUtils;

public class PreferenceFile {

	private Map<String, Object> mPreferences;
	private List<Entry<String, Object>> mList;

	public PreferenceFile() {
		super();
		mPreferences = new HashMap<String, Object>();
	}

	public static PreferenceFile fromXml(String xml) {
		Log.e("", "fromXML");
		Log.e("", xml);

		PreferenceFile preferenceFile = new PreferenceFile();

		// Check for empty files
		if (TextUtils.isEmpty(xml) || xml.trim().isEmpty())
			return preferenceFile;

		try {
			InputStream in = new ByteArrayInputStream(xml.getBytes());
			Map<String, Object> map = XmlUtils.readMapXml(in);
			in.close();

			if (map != null) {
				preferenceFile.setPreferences(map);
			}
		} catch (XmlPullParserException e) {
		} catch (IOException e) {
		}
		return preferenceFile;
	}

	public void setPreferences(Map<String, Object> map) {
		mPreferences = map;
		mList = new ArrayList<Entry<String, Object>>(mPreferences.entrySet());
	}

	public Map<String, Object> getPreferences() {
		return mPreferences;
	}

	public String toXml() {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			XmlUtils.writeMapXml(mPreferences, out);
		} catch (XmlPullParserException e) {
		} catch (IOException e) {
		}
		return out.toString();
	}

	public List<Entry<String, Object>> getList() {
		if (mList == null) {
			mList = new ArrayList<Entry<String, Object>>();
		}
		return mList;
	}

	public void setList(List<Entry<String, Object>> mList) {
		this.mList = mList;
	}

	private void updateValue(String key, Object value) {
		for (Entry<String, Object> entry : mList) {
			if (entry.getKey().equals(key)) {
				entry.setValue(value);
				break;
			}
		}
		mPreferences.put(key, value);
	}

	public void removeValue(String key) {
		mPreferences.remove(key);
		for (Entry<String, Object> entry : mList) {
			if (entry.getKey().equals(key)) {
				mList.remove(entry);
				break;
			}
		}
	}

	private void createAndAddValue(String key, Object value) {
		mList.add(0, new AbstractMap.SimpleEntry<String, Object>(key, value));
		mPreferences.put(key, value);
	}

	public void add(String previousKey, String newKey, Object value,
			boolean editMode) {
		if (TextUtils.isEmpty(newKey)) {
			return;
		}

		if (!editMode) {
			if (mPreferences.containsKey(newKey)) {
				updateValue(newKey, value);
			} else {
				createAndAddValue(newKey, value);
			}
		} else {
			if (newKey.equals(previousKey)) {
				updateValue(newKey, value);
			} else {
				removeValue(previousKey);

				if (mPreferences.containsKey(newKey)) {
					updateValue(newKey, value);
				} else {
					createAndAddValue(newKey, value);
				}
			}
		}
	}

	public static boolean save(PreferenceFile prefFile, String mFile, Context ctx, String packageName) {
		return save(prefFile.toXml(), mFile, ctx, packageName);
	}
	
	public static boolean save(String preferences, String mFile, Context ctx, String packageName) {
		Utils.debugFile(mFile);
		
		if(!isValid(preferences)){
			return false;
		}
		
		FileStat fs = App.getRoot().file.stat(mFile);
		java.io.File f = new java.io.File(ctx.getFilesDir(), "_temp");
		try {
			FileOutputStream outputStream = new FileOutputStream(f);
			outputStream.write(preferences.getBytes());
			outputStream.close();
			App.getRoot().file.move(f.getAbsolutePath(), mFile);
			App.getRoot().file.setPermission(mFile, "0660");
			App.getRoot().file.setOwner(mFile, fs.user()+"", fs.group()+"");
			App.getRoot().processes.kill(packageName);
		} catch (Exception e) {
			return false;
		}
		Utils.debugFile(mFile);
		return true;
	}
	
	private static boolean isValid(String xml) {
		try {
			XmlUtils.readMapXml(new ByteArrayInputStream(xml.getBytes()));
		} catch (Exception e) {
			return false;
		}
		return true;
	}

}

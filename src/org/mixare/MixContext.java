/*
 * Copyright (C) 2010- Peer internet solutions
 * 
 * This file is part of mixare.
 * 
 * This program is free software: you can redistribute it and/or modify it 
 * under the terms of the GNU General Public License as published by 
 * the Free Software Foundation, either version 3 of the License, or 
 * (at your option) any later version. 
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License 
 * for more details. 
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program. If not, see <http://www.gnu.org/licenses/>
 */
package org.mixare;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;
import java.util.Random;

import org.mixare.data.DataSource;
import org.mixare.data.DataSource.DATASOURCE;
import org.mixare.render.Matrix;

import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.Window;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;

public class MixContext extends ContextWrapper {

	public MixView mixView;
	Context ctx;
	boolean isURLvalid = true;
	Random rand;

	DownloadManager downloadManager;

	Location curLoc;
	Location locationAtLastDownload;
	Matrix rotationM = new Matrix();

	float declination = 0f;
	private boolean actualLocation=false;

	LocationManager locationMgr;
	
	private HashMap<DataSource.DATASOURCE,Boolean> selectedDataSources=new HashMap<DataSource.DATASOURCE,Boolean>();
	
	public MixContext(Context appCtx) {
	
		super(appCtx);
		this.mixView = (MixView) appCtx;
		this.ctx = appCtx.getApplicationContext();

		SharedPreferences settings = getSharedPreferences(MixView.PREFS_NAME, 0);
		boolean atLeastOneDatasourceSelected=false;
		
		for(DataSource.DATASOURCE source: DataSource.DATASOURCE.values()) {
			// fill the selectedDataSources HashMap with saved settings
			selectedDataSources.put(source, settings.getBoolean(source.toString(), false));
			if(selectedDataSources.get(source))
				atLeastOneDatasourceSelected=true;
		}
		// select Wikipedia if nothing was previously selected  
		if(!atLeastOneDatasourceSelected)
			setDataSource(DATASOURCE.WIKIPEDIA, true);

		rotationM.toIdentity();

		int locationHash = 0;
		try {
			locationMgr = (LocationManager) appCtx.getSystemService(Context.LOCATION_SERVICE);
			
			Location lastFix= locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
			
			if (lastFix == null){
				lastFix = locationMgr.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
			}
			if (lastFix != null){
				locationHash = ("HASH_" + lastFix.getLatitude() + "_" + lastFix.getLongitude()).hashCode();

				long actualTime= new Date().getTime();
				long lastFixTime = lastFix.getTime();
				long timeDifference = actualTime-lastFixTime;

				actualLocation = timeDifference <= 1200000;	//20 min --- 300000 milliseconds = 5 min
			}
			else
				actualLocation = false;
			
			
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		rand = new Random(System.currentTimeMillis() + locationHash);
	}
	
	public Location getCurrentGPSInfo() {
		return curLoc != null ? curLoc : locationMgr.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	}

	public boolean isGpsEnabled() {
		return mixView.isGpsEnabled();
	}

	public boolean isActualLocation(){
		return actualLocation;
	}

	public DownloadManager getDownloader() {
		return downloadManager;
	}
	
	public void setLocationManager(LocationManager locationMgr){
		this.locationMgr = locationMgr;
	}
	
	public LocationManager getLocationManager(){
		return locationMgr;
	}

	public String getStartUrl() {
		Intent intent = ((Activity) mixView).getIntent();
		if (intent.getAction() != null && intent.getAction().equals(Intent.ACTION_VIEW)) { 
			return intent.getData().toString(); 
		} 
		else { 
			return ""; 
		}
	}

	public void getRM(Matrix dest) {
		synchronized (rotationM) {
			dest.set(rotationM);
		}
	}

	public Location getCurrentLocation() {
		synchronized (curLoc) {
			return curLoc;
		}
	}

	public InputStream getHttpGETInputStream(String urlStr)
	throws Exception {
		InputStream is = null;
		URLConnection conn = null;
		if (urlStr.startsWith("content://"))
			return getContentInputStream(urlStr, null);

		try {
			URL url = new URL(urlStr);
			conn =  url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);

			is = conn.getInputStream();
			
			return is;
		} catch (Exception ex) {
			try {
				is.close();
			} catch (Exception ignore) {			
			}
			try {
				if(conn instanceof HttpURLConnection)
					((HttpURLConnection)conn).disconnect();
			} catch (Exception ignore) {			
			}
			
			throw ex;				

		}
	}

	public String getHttpInputString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8 * 1024);
		StringBuilder sb = new StringBuilder();

		try {
			String line;
			while ((line = reader.readLine()) != null) {
				Log.d(MixView.TAG, "MixContext // " + line);
				sb.append(line + "\n");
			}
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	private static HashMap<String, String> htmlEntities;
	static {
		htmlEntities = new HashMap<String, String>();
		htmlEntities.put("&lt;", "<");
		htmlEntities.put("&gt;", ">");
		htmlEntities.put("&amp;", "&");
		htmlEntities.put("&quot;", "\"");
		htmlEntities.put("&agrave;", "Ã ");
		htmlEntities.put("&Agrave;", "Ã€");
		htmlEntities.put("&acirc;", "Ã¢");
		htmlEntities.put("&auml;", "Ã¤");
		htmlEntities.put("&Auml;", "Ã„");
		htmlEntities.put("&Acirc;", "Ã‚");
		htmlEntities.put("&aring;", "Ã¥");
		htmlEntities.put("&Aring;", "Ã…");
		htmlEntities.put("&aelig;", "Ã¦");
		htmlEntities.put("&AElig;", "Ã†");
		htmlEntities.put("&ccedil;", "Ã§");
		htmlEntities.put("&Ccedil;", "Ã‡");
		htmlEntities.put("&eacute;", "Ã©");
		htmlEntities.put("&Eacute;", "Ã‰");
		htmlEntities.put("&egrave;", "Ã¨");
		htmlEntities.put("&Egrave;", "Ãˆ");
		htmlEntities.put("&ecirc;", "Ãª");
		htmlEntities.put("&Ecirc;", "ÃŠ");
		htmlEntities.put("&euml;", "Ã«");
		htmlEntities.put("&Euml;", "Ã‹");
		htmlEntities.put("&iuml;", "Ã¯");
		htmlEntities.put("&Iuml;", "Ã�");
		htmlEntities.put("&ocirc;", "Ã´");
		htmlEntities.put("&Ocirc;", "Ã”");
		htmlEntities.put("&ouml;", "Ã¶");
		htmlEntities.put("&Ouml;", "Ã–");
		htmlEntities.put("&oslash;", "Ã¸");
		htmlEntities.put("&Oslash;", "Ã˜");
		htmlEntities.put("&szlig;", "ÃŸ");
		htmlEntities.put("&ugrave;", "Ã¹");
		htmlEntities.put("&Ugrave;", "Ã™");
		htmlEntities.put("&ucirc;", "Ã»");
		htmlEntities.put("&Ucirc;", "Ã›");
		htmlEntities.put("&uuml;", "Ã¼");
		htmlEntities.put("&Uuml;", "Ãœ");
		htmlEntities.put("&nbsp;", " ");
		htmlEntities.put("&copy;", "\u00a9");
		htmlEntities.put("&reg;", "\u00ae");
		htmlEntities.put("&euro;", "\u20a0");
	}

	public static String unescapeHTML(String source, int start) {
		int i, j;

		i = source.indexOf("&", start);
		if (i > -1) {
			j = source.indexOf(";", i);
			if (j > i) {
				String entityToLookFor = source.substring(i, j + 1);
				String value = (String) htmlEntities.get(entityToLookFor);
				if (value != null) {
					source = new StringBuffer().append(source.substring(0, i))
					.append(value).append(source.substring(j + 1))
					.toString();
					return unescapeHTML(source, i + 1); // recursive call
				}
			}
		}
		return source;
	}

	public InputStream getHttpPOSTInputStream(String urlStr,
			String params) throws Exception {
		InputStream is = null;
		OutputStream os = null;
		HttpURLConnection conn = null;

		if (urlStr.startsWith("content://"))
			return getContentInputStream(urlStr, params);

		try {
			URL url = new URL(urlStr);
			conn = (HttpURLConnection) url.openConnection();
			conn.setReadTimeout(10000);
			conn.setConnectTimeout(10000);

			if (params != null) {
				conn.setDoOutput(true);
				os = conn.getOutputStream();
				OutputStreamWriter wr = new OutputStreamWriter(os);
				wr.write(params);
				wr.close();
			}

			is = conn.getInputStream();
			
			return is;
		} catch (Exception ex) {

			try {
				is.close();
			} catch (Exception ignore) {			

			}
			try {
				os.close();
			} catch (Exception ignore) {			

			}
			try {
				conn.disconnect();
			} catch (Exception ignore) {
			}

			if (conn != null && conn.getResponseCode() == 405) {
				return getHttpGETInputStream(urlStr);
			} else {		

				throw ex;
			}
		}
	}

	public InputStream getContentInputStream(String urlStr, String params)
	throws Exception {
		ContentResolver cr = mixView.getContentResolver();
		Cursor cur = cr.query(Uri.parse(urlStr), null, params, null, null);

		cur.moveToFirst();
		int mode = cur.getInt(cur.getColumnIndex("MODE"));

		if (mode == 1) {
			String result = cur.getString(cur.getColumnIndex("RESULT"));
			cur.deactivate();

			return new ByteArrayInputStream(result
					.getBytes());
		} else {
			cur.deactivate();

			throw new Exception("Invalid content:// mode " + mode);
		}
	}

	public void returnHttpInputStream(InputStream is) throws Exception {
		if (is != null) {
			is.close();
		}
	}

	public InputStream getResourceInputStream(String name) throws Exception {
		AssetManager mgr = mixView.getAssets();
		return mgr.open(name);
	}

	public void returnResourceInputStream(InputStream is) throws Exception {
		if (is != null)
			is.close();
	}

	public void loadMixViewWebPage(String url) throws Exception {
		// TODO
		WebView webview = new WebView(mixView);
		webview.getSettings().setJavaScriptEnabled(true);

		webview.setWebViewClient(new WebViewClient() {
			public boolean  shouldOverrideUrlLoading  (WebView view, String url) {
			     view.loadUrl(url);
				return true;
			}

		});
				
		Dialog d = new Dialog(mixView) {
			public boolean onKeyDown(int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK)
					this.dismiss();
				return true;
			}
		};
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setGravity(Gravity.BOTTOM);
		d.addContentView(webview, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM));

		d.show();
		
		webview.loadUrl(url);
	}
	public void loadWebPage(String url, Context context) throws Exception {
		// TODO
		WebView webview = new WebView(context);
		
		webview.setWebViewClient(new WebViewClient() {
			public boolean  shouldOverrideUrlLoading  (WebView view, String url) {
			     view.loadUrl(url);
				return true;
			}

		});
				
		Dialog d = new Dialog(context) {
			public boolean onKeyDown(int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK)
					this.dismiss();
				return true;
			}
		};
		d.requestWindowFeature(Window.FEATURE_NO_TITLE);
		d.getWindow().setGravity(Gravity.BOTTOM);
		d.addContentView(webview, new FrameLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT,
				Gravity.BOTTOM));

		d.show();
		
		webview.loadUrl(url);
	}



	public void setDataSource(DataSource.DATASOURCE source, Boolean selection){
		selectedDataSources.put(source,selection);
		SharedPreferences settings = getSharedPreferences(MixView.PREFS_NAME, 0);
		SharedPreferences.Editor editor = settings.edit();
		editor.putBoolean(source.toString(), selection);
		editor.commit();
	}
               
	public Boolean isDataSourceSelected(DataSource.DATASOURCE source) {
		return selectedDataSources.get(source);
	}
	
	public void toogleDataSource(DataSource.DATASOURCE source) {
		setDataSource(source, !selectedDataSources.get(source));
	}
	
	public String getDataSourcesStringList() {
		String ret="";
		boolean first=true;
		for(DataSource.DATASOURCE source: DataSource.DATASOURCE.values()) {
			if(isDataSourceSelected(source)) {
				if(!first) {
					ret+=", ";
				}	
				ret+=source.toString();
				first=false;
			}	
		}
		return ret;
	}

	public Location getLocationAtLastDownload() {
		return locationAtLastDownload;
	}

	public void setLocationAtLastDownload(Location locationAtLastDownload) {
		this.locationAtLastDownload = locationAtLastDownload;
	}
	
}

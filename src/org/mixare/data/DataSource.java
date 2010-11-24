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

package org.mixare.data;

import org.mixare.DataView;
import org.mixare.MixListView;
import org.mixare.MixView;
import org.mixare.R;

import android.content.res.Resources;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.util.Log;

/**
 * @author hannes
 *
 */
public class DataSource {
	
	// Datasource and dataformat are not the same. datasource is where the data comes from
	// and dataformat is how the data is formatted. 
	// this is necessary for example when you have multiple datasources with the same
	// dataformat
	public enum DATASOURCE { WIKIPEDIA, BUZZ, TWITTER, OSM, OWNURL, EVENTS, MERCHANDISE, REFRESHMENTS, RESTROOMS, SOCIAL};
	public enum DATAFORMAT { WIKIPEDIA, BUZZ, TWITTER, OSM, MIXARE, UBIQUITY};	

	/** default URL */
	private static final String WIKI_BASE_URL = "http://ws.geonames.org/findNearbyWikipediaJSON";
	//private static final String WIKI_BASE_URL =	"file:///sdcard/download/data.json";
	private static final String TWITTER_BASE_URL = "http://search.twitter.com/search.json";
	private static final String BUZZ_BASE_URL = "https://www.googleapis.com/buzz/v1/activities/search?alt=json&max-results=20";
	// OpenStreetMap API see http://wiki.openstreetmap.org/wiki/Xapi
	// eg. only railway stations:
	//private static final String OSM_BASE_URL =	"http://www.informationfreeway.org/api/0.6/node[railway=station]";
	//private static final String OSM_BASE_URL =	"http://xapi.openstreetmap.org/api/0.6/node[railway=station]";
	private static final String OSM_BASE_URL =		"http://osmxapi.hypercube.telascience.org/api/0.6/node[railway=station]";
	//all objects that have names: 
	//String OSM_URL = "http://xapi.openstreetmap.org/api/0.6/node[name=*]"; 
	//caution! produces hugh amount of data (megabytes), only use with very small radii or specific queries
	private static final String UBIQUITY_BASE_URL = "http://test.lvms.t-sciences.com/controllers/rest/search/?input="; //LVMS Test
		//"http://ubiquitymobile.com/lvms/controllers/rest/search/?input="; //LVMS Live
	private static final String ubiquityLocation = "\"location\":";
	private static final String ubiquityOpenBracket = "{";
	private static final String ubiquityCloseBracket = "}";
	private static final String ubiquityLat = "\"lat\":";
	private static final String ubiquityLon = "\"lng\":";
	private static final String ubiquityTerm = "\"term\":\"";
	private static final String ubiquityTermDelimiter = "%20"; //it's a space
	// Set default terms
	private static String ubiquityTermSelection = "";
	private static final String ubiquityLimit = "\"limit\":20"; //max 20 objects returned
	private static final String ubiquityOffset = "\"offset\":0"; //set offset to 0
	private static final String ubiquityDelimiter = ",";
	//http://ubiquitymobile.com/lvms/controllers/rest/search/?input=
	//{%22offset%22:0,%22limit%22:1000,%22term%22:%22restrooms%22,%22location%22:{%22lng%22:-115.010079,%22lat%22:36.272265}}
	private static String ubiquityJsonURL = "";
	
	public static Bitmap twitterIcon;
	public static Bitmap buzzIcon;
	
	public DataSource() {
		
	}
	
	public static void createIcons(Resources res) {
		twitterIcon=BitmapFactory.decodeResource(res, R.drawable.twitter);
		buzzIcon=BitmapFactory.decodeResource(res, R.drawable.buzz);
	}
	
	public static Bitmap getBitmap(DATASOURCE ds) {
		Bitmap bitmap=null;
		switch (ds) {
			case TWITTER: bitmap=twitterIcon; break;
			case BUZZ: bitmap=buzzIcon; break;
		}
		return bitmap;
	}
	
	public static DATAFORMAT dataFormatFromDataSource(DATASOURCE ds) {
		DATAFORMAT ret;
		switch (ds) {
			case WIKIPEDIA: ret=DATAFORMAT.WIKIPEDIA; break;
			case BUZZ: ret=DATAFORMAT.BUZZ; break;
			case TWITTER: ret=DATAFORMAT.TWITTER; break;
			case OSM: ret=DATAFORMAT.OSM; break;
			case OWNURL: ret=DATAFORMAT.MIXARE; break;
			case EVENTS:ret=DATAFORMAT.UBIQUITY; break;
			case MERCHANDISE:ret=DATAFORMAT.UBIQUITY; break;
			case REFRESHMENTS:ret=DATAFORMAT.UBIQUITY; break;
			case RESTROOMS:ret=DATAFORMAT.UBIQUITY; break;
			case SOCIAL:ret=DATAFORMAT.UBIQUITY; break;
			default: ret=DATAFORMAT.UBIQUITY; break;
		}
		return ret;
	}
	
	public static String createRequestURL(DATASOURCE source, double lat, double lon, double alt, float radius,String locale) {
		String ret="";
		switch(source) {
		
			case WIKIPEDIA: 
				ret= WIKI_BASE_URL + 
				"?lat=" + lat +
				"&lng=" + lon + 
				"&radius="+ radius +
				"&maxRows=50" +
				"&lang=" + locale; 
			break;
			
			case BUZZ: 
				ret= BUZZ_BASE_URL + 
				"&lat=" + lat+
				"&lon=" + lon + 
				"&radius="+ radius*1000;
			break;
			
			case TWITTER: 
				ret = TWITTER_BASE_URL +
				"?geocode=" + lat + "%2C" + lon + "%2C" + 
				Math.max(radius, 1.0) + "km" ;				
			break;
				
			case OSM: 
				ret = OSM_BASE_URL + XMLHandler.getOSMBoundingBox(lat, lon, radius);
			break;
			
			case OWNURL:
				ret = MixListView.customizedURL +  
				"?latitude=" + Double.toString(lat) + 
				"&longitude=" + Double.toString(lon) + 
				"&altitude=" + Double.toString(alt) +
				"&radius=" + Double.toString(radius);
			break;
			
			case EVENTS:
				ubiquityTermSelection = ubiquityTermDelimiter + "Events";
				setUbiquityJsonURL(lat, lon);
				ret = ubiquityJsonURL;
				//ubiquityTermSelection += ubiquityTermDelimiter + "\"" + getString(DataView.SEARCH_TERM_EVENTS) + "\"";
				break;
			
			case MERCHANDISE:
				ubiquityTermSelection = ubiquityTermDelimiter + "Merchandise";
				setUbiquityJsonURL(lat, lon);
				ret = ubiquityJsonURL;
				//ubiquityTermSelection += ubiquityTermDelimiter + "\"" +  getString(DataView.SEARCH_TERM_MERCHANDISE) + "\"";
				break;
				
			case REFRESHMENTS:
				ubiquityTermSelection = ubiquityTermDelimiter + "Refreshments";
				setUbiquityJsonURL(lat, lon);
				ret = ubiquityJsonURL;
				//ubiquityTermSelection += ubiquityTermDelimiter + "\"" +  getString(DataView.SEARCH_TERM_REFRESHMENTS) + "\"";
				break;
				
			case RESTROOMS:
				ubiquityTermSelection = ubiquityTermDelimiter + "Restrooms" ;
				setUbiquityJsonURL(lat, lon);
				ret = ubiquityJsonURL;
				//ubiquityTermSelection += ubiquityTermDelimiter + "\"" +  getString(DataView.SEARCH_TERM_RESTROOMS) + "\"";
				break;
				
			case SOCIAL:
				ubiquityTermSelection = ubiquityTermDelimiter + "Social";
				setUbiquityJsonURL(lat, lon);
				ret = ubiquityJsonURL;
				//ubiquityTermSelection += ubiquityTermDelimiter + "\"" +  getString(DataView.SEARCH_TERM_SOCIAL) + "\"";
				break;
		}
		/*//single ubiquity JSON request builder
		//That means user selected a combination of Ubiquity Terms (Events, Merchandise, etc.) and wants Ubiquity JSON
		if (ubiquityTermSelection.length() > 0) {
			// Strip previously added " then close up the term selection with a single "
			ubiquityTermSelection.replaceAll("\"", "");
			ubiquityTermSelection += "\"";
			// Build this : 	http://ubiquitymobile.com/lvms/controllers/rest/search/?input=
								//{%22offset%22:0,%22limit%22:20,%22term%22:%22restrooms%22,
								//%22location%22:{%22lng%22:-115.010079,%22lat%22:36.272265}}
			ubiquityJsonURL = UBIQUITY_BASE_URL + ubiquityOpenBracket + ubiquityOffset + ubiquityDelimiter
										+ ubiquityLimit +ubiquityDelimiter + ubiquityOffset + ubiquityDelimiter
										+ ubiquityTerm + ubiquityTermSelection + ubiquityDelimiter
										+ ubiquityLocation + ubiquityOpenBracket
										+ ubiquityLat +lat + ubiquityDelimiter + ubiquityLon + lon
										+ ubiquityCloseBracket + ubiquityCloseBracket;
			ret = ubiquityJsonURL;
		}*/
		Log.i(MixView.TAG, "DataSource // JSON URL: " + ret);
		return ret;
	}
	
	public static void setUbiquityJsonURL(double lat, double lon) {
		ubiquityJsonURL = UBIQUITY_BASE_URL + ubiquityOpenBracket + ubiquityOffset + ubiquityDelimiter
								+ ubiquityLimit +ubiquityDelimiter + ubiquityOffset + ubiquityDelimiter
								+ ubiquityTerm + ubiquityTermSelection + "\"" + ubiquityDelimiter
								+ ubiquityLocation + ubiquityOpenBracket
								+ ubiquityLat +lat + ubiquityDelimiter + ubiquityLon + lon
								+ ubiquityCloseBracket + ubiquityCloseBracket;
	}
	
	public static int getColor(DATASOURCE datasource) {
		int ret;
		switch(datasource) {
			case BUZZ:		ret=Color.rgb(4, 228, 20); break;
			case TWITTER:	ret=Color.rgb(50, 204, 255); break;
			case OSM:		ret=Color.rgb(255, 168, 0); break;
			case WIKIPEDIA:	ret=Color.RED; break;
			case EVENTS: ret=Color.GREEN;break;
			case MERCHANDISE: ret=Color.BLUE;break;
			case REFRESHMENTS: ret=Color.rgb(187,68,000);break; //Orange
			case RESTROOMS: ret=Color.BLACK;break;
			case SOCIAL: ret=Color.rgb(119,204,255);break; //Light Blue
			default:		ret=Color.WHITE; break;
		}
		return ret;
	}

}

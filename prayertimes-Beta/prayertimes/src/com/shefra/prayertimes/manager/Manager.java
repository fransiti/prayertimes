package com.shefra.prayertimes.manager;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import com.shefra.prayertimes.moazen.PrayerTime;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.sqlite.*;
import android.preference.PreferenceManager;

public class Manager extends SQLiteOpenHelper {

	private static String DB_PATH = "/data/data/com.shefra.prayertimes/databases/";
	private static String DB_NAME = "CountriesDB";
	private Context context;
	private SQLiteDatabase db;

	public Manager(Context applicationContext) {
		super(applicationContext, DB_NAME, null, 1);
		this.context = applicationContext;
	}

	public int to24(String time) {
		String[] t = time.split(":");
		String[] AMORPM = t[2].split(" ");
		int houer = Integer.parseInt(t[0]);
		if (AMORPM[1].equals("PM") && houer != 12)
			houer += 12;
		return houer;
	}

	public int getMinute(String time) {
		String[] t = time.split(":");
		return Integer.parseInt(t[1]);
	}

	public int diffrent(int current, int prayer) {
		if (current <= prayer)
			return prayer - current;

		return (prayer + (24 * 3600)) - current;
	}

	public int getSecond(String time) {
		String[] t = time.split(":");
		String[] s = t[2].split(" ");
		return Integer.parseInt(s[0]);
	}

	public int getSec(String time) {
		int sec = 0;
		String[] temp = time.split(":");
		sec = Integer.parseInt(temp[0]) * 3600;
		sec += Integer.parseInt(temp[1]) * 60;
		temp = temp[2].split(" ");
		sec += Integer.parseInt(temp[0]);
		return sec;
	}

	public static String secondsToTime(double time) {

		int hours = (int) (time / 3600);
		time = time - (hours * 3600);
		int minutes = (int) (time / 60);
		time = time - minutes * 60;
		int seconds = (int) time;
		String remTime = hours % 12 + ":" + minutes /* + ":" + seconds */;
		return remTime;
	}

	public int getSec(int hh, int mm, int ss) {
		return ((hh * 3600) + (mm * 60) + ss);
	}

	public int nearestPrayerTime(int hour, int min, int sec, int year,
			int month, int day) throws IOException {
		ArrayList<String> prayerTimes = getPrayerTimes(day, month, year);
		int[] prayerTimeInSeconds = new int[5];

		// Convert prayer times to seconds
		prayerTimeInSeconds[0] = this.getSec(this.to24(prayerTimes.get(0)),
				this.getMinute(prayerTimes.get(0)),
				this.getSecond(prayerTimes.get(0)));
		prayerTimeInSeconds[1] = this.getSec(this.to24(prayerTimes.get(1)),
				this.getMinute(prayerTimes.get(1)),
				this.getSecond(prayerTimes.get(1)));
		prayerTimeInSeconds[2] = this.getSec(this.to24(prayerTimes.get(2)),
				this.getMinute(prayerTimes.get(2)),
				this.getSecond(prayerTimes.get(2)));
		prayerTimeInSeconds[3] = this.getSec(this.to24(prayerTimes.get(3)),
				this.getMinute(prayerTimes.get(3)),
				this.getSecond(prayerTimes.get(3)));
		prayerTimeInSeconds[4] = this.getSec(this.to24(prayerTimes.get(4)),
				this.getMinute(prayerTimes.get(4)),
				this.getSecond(prayerTimes.get(4)));

		// sort ascending
		Arrays.sort(prayerTimeInSeconds);

		// default value is the first prayer in the day
		int nearestPrayer = prayerTimeInSeconds[0];

		// convert current time to seconds
		int currentTime = hour * 3600 + min * 60 + sec;

		for (Integer prayertime : prayerTimeInSeconds) {
			int pt = prayertime;

			// return first prayer after this time ( nearest prayer)
			if (pt > currentTime)
				return pt;
		}
		return nearestPrayer;
	}

	// -----------DataBase methods-----------//
	public String getDatabasePath() {
		return DB_PATH;
	}

	public String getDatabaseName() {
		return DB_NAME;
	}

	private boolean checkDataBase() {

		SQLiteDatabase checkDB = null;

		try {
			String myPath = DB_PATH + DB_NAME;
			checkDB = SQLiteDatabase.openDatabase(myPath, null,
					SQLiteDatabase.OPEN_READONLY);

		} catch (SQLiteException e) {

			// database does't exist yet.

		}

		if (checkDB != null) {

			checkDB.close();

		}

		return checkDB != null ? true : false;
	}

	public void createDatabase() throws IOException {
		boolean dbExist = checkDataBase();

		if (dbExist) {
			// do nothing - database already exist
		} else {

			// By calling this method an empty database will be created into
			// the default system path
			// of your application so we are gonna be able to overwrite that
			// database with our new database.
			this.getReadableDatabase();

			try {

				copyDataBase();

			} catch (IOException e) {

				throw new Error("Error copying database");

			}
		}
	}

	public void copyDataBase() throws IOException {
		// Open your local db as the input stream

		InputStream myInput = this.context.getAssets().open(DB_NAME);
		// Path to the just created empty db
		String outFileName = DB_PATH + DB_NAME;
		// Open the empty db as the output stream
		OutputStream myOutput = new FileOutputStream(outFileName);

		// transfer bytes from the inputfile to the outputfile
		byte[] buffer = new byte[1024];
		int length;
		while ((length = myInput.read(buffer)) > 0) {
			myOutput.write(buffer, 0, length);
		}

		// Close the streams
		myOutput.flush();
		myOutput.close();
		myInput.close();
	}

	@Override
	public synchronized void close() {

		if (db != null)
			db.close();

		super.close();

	}

	@Override
	public void onCreate(SQLiteDatabase db) {

	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

	}

	// -----------set method-----------//
	public void setSetting(settingAttributes sa) {
		azanAttribute aA = this.getData(sa.city.cityNo);
		sa.city.latitude = aA.latitude;
		sa.city.longitude = aA.longitude;
		sa.city.timeZone = aA.timeZone;
		sa.country.countryNo = Integer.parseInt(aA.countryNo);
		this.xmlWriter(sa);
	}

	// -----------XML methods-----------//
	public void xmlWriter(settingAttributes sa) {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this.context);
		Editor editor = pref.edit();
		editor.putString("country", Integer.toString(sa.country.countryNo));
		editor.putString("city", Integer.toString(sa.city.cityNo));
		editor.putString("latitude", sa.city.latitude);
		editor.putString("longitude", sa.city.longitude);
		editor.putString("timeZone", sa.city.timeZone);
		editor.putString("isCityChanged", "true");
		editor.commit();
	}

	public settingAttributes xmlReader() {
		settingAttributes sa = new settingAttributes();
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(this.context);
		sa.city.timeZone = pref.getString("timeZone", "3");
		// Makkah
		sa.city.latitude = pref.getString("latitude", "21.43");
		sa.city.longitude = pref.getString("longitude", "39.82");
		sa.calender = pref.getString("calender", "UmmAlQuraUniv");
		sa.mazhab = pref.getString("mazhab", "2");
		return sa;
	}

	// -----------get methods-----------//
	// id = -1 => means all citys
	public ArrayList<City> getCityList(int id) {
		ArrayList<City> city = new ArrayList<City>();

		db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + DB_NAME, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		String whereClause = null;
		if (id != -1) {
			whereClause = "country_id=" + id;
		}
		Cursor cur = db.query("citiesTable", new String[] { "cityNO",
				"cityName", "cityNameAr", "latitude", "longitude" },
				whereClause, null, null, null, null);
		cur.moveToFirst();
		while (cur.isAfterLast() == false) {
			City c = new City();
			c.cityNo = cur.getInt(0);
			if (cur.getString(2) != null)
				c.cityName = cur.getString(2);
			else
				c.cityName = cur.getString(1);

			c.latitude = cur.getString(3);
			c.longitude = cur.getString(4);
			city.add(c);
			cur.moveToNext();
		}
		cur.close();
		db.close();
		return city;
	}

	public ArrayList<Country> getCountryList() {
		SQLiteDatabase db;
		ArrayList<Country> country = new ArrayList<Country>();

		db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + DB_NAME, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		Cursor cur = db.query("country", null, null, null, null, null, null);
		cur.moveToFirst();
		while (cur.isAfterLast() == false) {
			Country c = new Country();
			c.countryNo = cur.getInt(0);
			if (cur.getString(2) != null)
				c.countryName = cur.getString(2);
			else
				c.countryName = cur.getString(1);

			country.add(c);
			cur.moveToNext();
		}
		cur.close();
		db.close();
		return country;
	}

	public azanAttribute getData(int id) {
		db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + DB_NAME, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);
		azanAttribute aA = new azanAttribute();
		String select = "select cityName,latitude,longitude,timeZone,country_id from citiesTable where cityNO ="
				+ id;
		Cursor cur = db.rawQuery(select, null);
		cur.moveToFirst();
		aA.cityName = cur.getString(0);
		aA.latitude = cur.getString(1);
		aA.longitude = cur.getString(2);
		aA.timeZone = cur.getString(3);
		aA.countryNo = cur.getString(4);
		cur.close();
		db.close();
		return aA;
	}

	public ArrayList<String> getPrayerTimes(int dd, int mm, int yy)
			throws IOException {

		// No need to divide by 10000 or 100 since we use the new database
		// edited by : al-shammeri

		ArrayList<String> prayerList = new ArrayList<String>();
		settingAttributes sa = this.xmlReader();
		PrayerTime prayerTime = new PrayerTime(
				Double.parseDouble(sa.city.longitude)/* / 10000 */,
				Double.parseDouble(sa.city.latitude)/* / 10000 */,
				Integer.parseInt(sa.city.timeZone) /* / 100 */, dd, mm, yy);

		prayerTime.calculate();
		prayerList.add(prayerTime.fajrTime().text());
		prayerList.add(prayerTime.zuhrTime().text());
		prayerList.add(prayerTime.asrTime().text());
		prayerList.add(prayerTime.maghribTime().text());
		prayerList.add(prayerTime.ishaTime().text());
		return prayerList;
	}

	public Context getContext() {
		return context;
	}

	public Country getCountry(int countrId) {
		SQLiteDatabase db;
		Country country = new Country();

		db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + DB_NAME, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		Cursor cur = db.query("country", null, "country_id=" + countrId, null,
				null, null, null);
		cur.moveToFirst();
		while (cur.isAfterLast() == false) {

			country.countryNo = cur.getInt(0);

			if (cur.getString(2) != null)
				country.countryName = cur.getString(2);
			else
				country.countryName = cur.getString(1);

			cur.moveToNext();
		}
		cur.close();
		db.close();
		return country;
	}

	public City getCity(int cityId) {
		City city = new City();

		db = SQLiteDatabase.openOrCreateDatabase(DB_PATH + DB_NAME, null);
		db.setVersion(1);
		db.setLocale(Locale.getDefault());
		db.setLockingEnabled(true);

		Cursor cur = db.query("citiesTable", new String[] { "cityNO",
				"cityName", "cityNameAr" }, "cityNO=" + cityId, null, null,
				null, null);
		cur.moveToFirst();
		while (cur.isAfterLast() == false) {

			city.cityNo = cur.getInt(0);

			if (cur.getString(2) != null)
				city.cityName = cur.getString(2);
			else
				city.cityName = cur.getString(1);

			cur.moveToNext();
		}
		cur.close();
		db.close();
		return city;
	}

	public City getCurrentCity() {
		SharedPreferences pref = PreferenceManager
				.getDefaultSharedPreferences(context);
		String cityId = pref.getString("city", "1");
		City city = this.getCity(Integer.parseInt(cityId));
		return city;

	}

	public void findCurrentCity(double latitude, double longitude) {
		try {
			double min = 0;
			int i = 0, pos = 0;
			ArrayList<City> cityList = this.getCityList(-1);
			for (City city : cityList) {
				double lat = Double.parseDouble(city.latitude);
				double lon = Double.parseDouble(city.longitude);
				double pk = (180 / 3.14159);
				double a1 = (lat / pk);
				double a2 = (lon / pk);

				double b1 = (latitude / pk);
				double b2 = (longitude / pk);

				double t1 = (Math.cos(a1) * Math.cos(a2) * Math.cos(b1) * Math
						.cos(b2));
				double t2 = (Math.cos(a1) * Math.sin(a2) * Math.cos(b1) * Math
						.sin(b2));
				double t3 = (Math.sin(a1) * Math.sin(b1));
				double tt = Math.acos(t1 + t2 + t3);
				double dist = (6366000 * tt);
				if (dist < min || i == 0) {
					min = dist;
					pos = i;
				}
				i++;

			}
			if (pos < cityList.size() && cityList.get(pos) != null) {
				settingAttributes sa = new settingAttributes();
				String cityId = (String) Integer
						.toString(cityList.get(pos).cityNo);
				sa.city.cityNo = -1;
				if (cityId != null) {
					sa.city.cityNo = Integer.parseInt(cityId);
				}
				if (sa.city.cityNo == -1)
					sa.city.cityNo = 1;
				this.setSetting(sa);

			}
		} catch (Exception e) {
			e = e;
		}

	}

}
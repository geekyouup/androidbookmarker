/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.geekyouup.android.bookmarker;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.database.ContentObserver;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Browser;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebIconDatabase;
import android.webkit.WebIconDatabase.IconListener;
import android.widget.BaseAdapter;

import java.io.ByteArrayOutputStream;
import java.util.Date;

class BookmarkAdapter extends BaseAdapter {

    private Cursor                  mCursor;
    private int                     mCount;
    private ContentResolver         mContentResolver;
    private ChangeObserver          mChangeObserver;
    private DataSetObserver         mDataSetObserver;
    private boolean                 mDataValid;
    private Bookmarker				mBookmarker;

    // Implementation of WebIconDatabase.IconListener
    private class IconReceiver implements IconListener {
        public void onReceivedIcon(String url, Bitmap icon) {
            updateBookmarkFavicon(mContentResolver, url, icon);
        }
    }

    // Instance of IconReceiver
    private final IconReceiver mIconReceiver = new IconReceiver();

    /**
     *  Create a new BrowserBookmarksAdapter.
     *  @param b        BrowserBookmarksPage that instantiated this.  
     *                  Necessary so it will adjust its focus
     *                  appropriately after a search.
     */
    public BookmarkAdapter(Bookmarker b, String curPage) {
        this(b, curPage, false);
    }

    /**
     *  Create a new BrowserBookmarksAdapter.
     *  @param b        BrowserBookmarksPage that instantiated this.
     *                  Necessary so it will adjust its focus
     *                  appropriately after a search.
     */
    public BookmarkAdapter(Bookmarker b, String curPage,
            boolean createShortcut) {
        mDataValid = false;
        mBookmarker = b;
        mContentResolver = b.getContentResolver();
        mChangeObserver = new ChangeObserver();
        mDataSetObserver = new MyDataSetObserver();
        // FIXME: Should have a default sort order that the user selects.
        search();
        // FIXME: This requires another query of the database after the
        // initial search(null). Can we optimize this?
        Browser.requestAllIcons(mContentResolver,
                Browser.BookmarkColumns.FAVICON + " is NULL AND " +
                Browser.BookmarkColumns.BOOKMARK + " == 1", mIconReceiver);
    }
    
    public void initialiseAllRows()
    {
    	int processed = 0;
    	
    	//ensures each row has a created time
    	while(processed < mCount)
    	{
	    	for(int i=0;i<mCount;i++)
	    	{
	    		Bundle thisRow = getRow(i, true);
	    		Long thisCreateTime = thisRow.getLong(Browser.BookmarkColumns.CREATED);
	    		if(thisCreateTime==null || thisCreateTime<900)
	    		{
	    			//get createTime of row above and subtract 1000;
	    			long createTime = (mCount-i)*1000;
	    			if(i>0)
	    			{
	    				Bundle prevRow = getRow(i, true);
	    				Long prevCreateTime = prevRow.getLong(Browser.BookmarkColumns.CREATED);
	    				if(prevCreateTime!=null && prevCreateTime>1001)
	    				{
	    					createTime = prevCreateTime-1000;
	    				}
	    			}
	    			
	    			thisRow.putLong(Browser.BookmarkColumns.CREATED,createTime);
	    			updateRow(thisRow, true);
	    			break;//underlying dataset has updated so quit the loop and start again
	    		}
	    		processed=i+1;
	    	}
    	}
    }
    
    /**
     *  Return a hashmap with one row's Title, Url, and favicon.
     *  @param position  Position in the list.
     *  @return Bundle  Stores title, url of row position, favicon, and id
     *                   for the url.  Return a blank map if position is out of
     *                   range.
     */
    public Bundle getRow(int position, boolean includeCreateTime) {
        Bundle map = new Bundle();
        if (position < 0 || position >= mCount) {
            return map;
        }
        mCursor.moveToPosition(position);
        String url = mCursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
        map.putString(Browser.BookmarkColumns.TITLE, 
                mCursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX));
        map.putString(Browser.BookmarkColumns.URL, url);
        byte[] data = mCursor.getBlob(Browser.HISTORY_PROJECTION_FAVICON_INDEX);
        if (data != null) {
        	map.putByteArray("FAV_BYTES", data);
            map.putParcelable(Browser.BookmarkColumns.FAVICON,
                    BitmapFactory.decodeByteArray(data, 0, data.length));
        }
        
        if(includeCreateTime && createdColumnIndex != -1)
        {
        	Long createTime = mCursor.getLong(createdColumnIndex);
        	if(createTime != null && createTime != 0)
        	{
        		map.putLong(Browser.BookmarkColumns.CREATED,createTime);	
        	}
        }
        
        map.putInt("id", mCursor.getInt(Browser.HISTORY_PROJECTION_ID_INDEX));
        
        return map;
    }

    /**
     *  Update a row in the database with new information. 
     *  Requeries the database if the information has changed.
     *  @param map  Bundle storing id, title and url of new information
     */
    public void updateRow(Bundle map, boolean includeCreateTime) {

        // Find the record
        int id = map.getInt("id");
        int position = -1;
        for (mCursor.moveToFirst(); !mCursor.isAfterLast(); mCursor.moveToNext()) {
            if (mCursor.getInt(Browser.HISTORY_PROJECTION_ID_INDEX) == id) {
                position = mCursor.getPosition();
                break;
            }
        }
        if (position < 0) {
            return;
        }

        mCursor.moveToPosition(position);
        ContentValues values = new ContentValues();
        String title = map.getString(Browser.BookmarkColumns.TITLE);
        if (!title.equals(mCursor
                .getString(Browser.HISTORY_PROJECTION_TITLE_INDEX))) {
            values.put(Browser.BookmarkColumns.TITLE, title);
        }
        String url = map.getString(Browser.BookmarkColumns.URL);
        if (!url.equals(mCursor.
                getString(Browser.HISTORY_PROJECTION_URL_INDEX))) {
            values.put(Browser.BookmarkColumns.URL, url);
        }
        
        byte[] favicon = map.getByteArray("FAV_BYTES");
        if(favicon != null)
        {
        	values.put(Browser.BookmarkColumns.FAVICON,favicon);
        }
        
        if(includeCreateTime)
        {
	        Long createTime = map.getLong(Browser.BookmarkColumns.CREATED);
	        if(createTime!= null) values.put(Browser.BookmarkColumns.CREATED,createTime);
        }
        
        if (values.size() > 0
                && mContentResolver.update(Browser.BOOKMARKS_URI, values,
                        "_id = " + id, null) != -1) {
            refreshList();
        }
    }

    /**
     *  Delete a row from the database.  Requeries the database.  
     *  Does nothing if the provided position is out of range.
     *  @param position Position in the list.
     */
    public void deleteRow(int position) {
        if (position < 0 || position >= getCount()) {
            return;
        }
        mCursor.moveToPosition(position);
        String url = mCursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
        WebIconDatabase.getInstance().releaseIconForPageUrl(url);
        Uri uri = ContentUris.withAppendedId(Browser.BOOKMARKS_URI, mCursor
                .getInt(Browser.HISTORY_PROJECTION_ID_INDEX));
        int numVisits = mCursor.getInt(Browser.HISTORY_PROJECTION_VISITS_INDEX);
        if (0 == numVisits) {
            mContentResolver.delete(uri, null, null);
        } else {
            // It is no longer a bookmark, but it is still a visited site.
            ContentValues values = new ContentValues();
            values.put(Browser.BookmarkColumns.BOOKMARK, 0);
            mContentResolver.update(uri, values, null, null);
        }
        refreshList();
    }
    
    /**
     *  Refresh list to recognize a change in the database.
     */
    public void refreshList() {
        searchInternal();
    }

    /**
     *  Search the database for bookmarks that match the input string.
     *  @param like String to use to search the database.  Strings with spaces 
     *              are treated as having multiple search terms using the
     *              OR operator.  Search both the title and url.
     */
    public void search() {
        searchInternal();
    }

    /**
     * Update the bookmark's favicon.
     * @param cr The ContentResolver to use.
     * @param url The url of the bookmark to update.
     * @param favicon The favicon bitmap to write to the db.
     */
    /* package */ static void updateBookmarkFavicon(ContentResolver cr,
            String url, Bitmap favicon) {
        if (url == null || favicon == null) {
            return;
        }
        // Strip the query.
        int query = url.indexOf('?');
        String noQuery = url;
        if (query != -1) {
            noQuery = url.substring(0, query);
        }
        url = noQuery + '?';
        // Use noQuery to search for the base url (i.e. if the url is
        // http://www.yahoo.com/?rs=1, search for http://www.yahoo.com)
        // Use url to match the base url with other queries (i.e. if the url is
        // http://www.google.com/m, search for
        // http://www.google.com/m?some_query)
        final String[] selArgs = new String[] { noQuery, url };
        final String where = "(" + Browser.BookmarkColumns.URL + " == ? OR "
                + Browser.BookmarkColumns.URL + " GLOB ? || '*') AND "
                + Browser.BookmarkColumns.BOOKMARK + " == 1";
        final String[] projection = new String[] { Browser.BookmarkColumns._ID };
        final Cursor c = cr.query(Browser.BOOKMARKS_URI, projection, where,
                selArgs, null);
        boolean succeed = c.moveToFirst();
        ContentValues values = null;
        while (succeed) {
            if (values == null) {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                favicon.compress(Bitmap.CompressFormat.PNG, 100, os);
                values = new ContentValues();
                values.put(Browser.BookmarkColumns.FAVICON, os.toByteArray());
            }
            cr.update(ContentUris.withAppendedId(Browser.BOOKMARKS_URI, c
                    .getInt(0)), values, null, null);
            succeed = c.moveToNext();
        }
        c.close();
    }

    /**
     *  Internal function used in search, sort, and refreshList.
     */
    private int createdColumnIndex = -1;
    private void searchInternal() {
        if (mCursor != null) {
            mCursor.unregisterContentObserver(mChangeObserver);
            mCursor.unregisterDataSetObserver(mDataSetObserver);
            mCursor.deactivate();
        }

    	//need to add the created date column to the query
    	String[] columns = new String[Browser.HISTORY_PROJECTION.length+1];
    	System.arraycopy(Browser.HISTORY_PROJECTION, 0, columns, 0,Browser.HISTORY_PROJECTION.length);
    	createdColumnIndex = columns.length-1;
    	columns[createdColumnIndex] = Browser.BookmarkColumns.CREATED;
    	
    	String whereClause = Browser.BookmarkColumns.BOOKMARK + " == 1";
    	String orderBy = Browser.BookmarkColumns.CREATED + " DESC";
    	String[] selectionArgs = null;
        mCursor = mContentResolver.query(
            Browser.BOOKMARKS_URI,
            columns,
            whereClause,
            selectionArgs, 
            orderBy);
        mCursor.registerContentObserver(mChangeObserver);
        mCursor.registerDataSetObserver(mDataSetObserver);

        mDataValid = true;
        notifyDataSetChanged();

        mCount = mCursor.getCount();
    }

    /**
     * How many items should be displayed in the list.
     * @return Count of items.
     */
    public int getCount() {
        if (mDataValid) {
            return mCount;
        } else {
            return 0;
        }
    }

    public boolean areAllItemsEnabled() {
        return true;
    }

    public boolean isEnabled(int position) {
        return true;
    }

    /**
     * Get the data associated with the specified position in the list.
     * @param position Index of the item whose data we want.
     * @return The data at the specified position.
     */
    public Object getItem(int position) {
        return null;
    }

    /**
     * Get the row id associated with the specified position in the list.
     * @param position Index of the item whose row id we want.
     * @return The id of the item at the specified position.
     */
    public long getItemId(int position) {
        return position;
    }

    int mCurrentSelection = -1;
    public void setSelected(int position)
    {
    	mCurrentSelection = position;
    	notifyDataSetInvalidated();
    }
    
    /**
     * Get a View that displays the data at the specified position
     * in the list.
     * @param position Index of the item whose view we want.
     * @return A View corresponding to the data at the specified position.
     */
    public View getView(int position, View convertView, ViewGroup parent) {
        if (!mDataValid) {
            throw new IllegalStateException(
                    "this should only be called when the cursor is valid");
        }
        if (position < 0 || position > mCount) {
            throw new AssertionError(
                    "BrowserBookmarksAdapter tried to get a view out of range");
        }

        if (convertView == null) {
            convertView = new BookmarkItem(mBookmarker);
        }
        bind((BookmarkItem)convertView, position);
        if(position == mCurrentSelection)
        {
        	((BookmarkItem)convertView).setSelected();
        }else
        {
        	((BookmarkItem)convertView).setUnselected();
        }
        
        return convertView;
    }

    /**
     *  Return the title for this item in the list.
     */
    public String getTitle(int position) {
        return getString(Browser.HISTORY_PROJECTION_TITLE_INDEX, position);
    }

    /**
     *  Return the Url for this item in the list.
     */
    public String getUrl(int position) {
        return getString(Browser.HISTORY_PROJECTION_URL_INDEX, position);
    }

    /**
     * Private helper function to return the title or url.
     */
    private String getString(int cursorIndex, int position) {
        if (position < 0 || position > mCount) {
            return "";
        }
        mCursor.moveToPosition(position);
        return mCursor.getString(cursorIndex);
    }

    private void bind(BookmarkItem b, int position) {
        mCursor.moveToPosition(position);

        String title = mCursor.getString(Browser.HISTORY_PROJECTION_TITLE_INDEX);            
        b.setName(title);
        String url = mCursor.getString(Browser.HISTORY_PROJECTION_URL_INDEX);
        b.setUrl(url);
        byte[] data = mCursor.getBlob(Browser.HISTORY_PROJECTION_FAVICON_INDEX);
        if (data != null) {
            b.setFavicon(BitmapFactory.decodeByteArray(data, 0, data.length));
        } else {
            b.setFavicon(null);
        }
    }

    private class ChangeObserver extends ContentObserver {
        public ChangeObserver() {
            super(new Handler());
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            refreshList();
        }
    }
    
    private class MyDataSetObserver extends DataSetObserver {
        @Override
        public void onChanged() {
            mDataValid = true;
            notifyDataSetChanged();
        }

        @Override
        public void onInvalidated() {
            mDataValid = false;
            notifyDataSetInvalidated();
        }
    }
    
    //returns new position
    public int moveItemUp(int position)
    {
		if(position > 0)
		{
			Bundle rowOnTop = getRow(position-1, false);
			Bundle rowUnder = getRow(position, false);
			
			int upId = rowOnTop.getInt("id");
			rowOnTop.putInt("id",rowUnder.getInt("id"));
			rowUnder.putInt("id",upId);
		
			mCurrentSelection--;
			
			updateRow(rowOnTop, false);
			updateRow(rowUnder, false);
			
			return mCurrentSelection;
		}else return position;
    }
    
    //returns new position
    public int moveItemDown(int position)
    {
		if(position < mCount-1)
		{
			Bundle rowOnTop = getRow(position, false);
			Bundle rowUnder = getRow(position+1, false);
			
			int upId = rowOnTop.getInt("id");
			rowOnTop.putInt("id",rowUnder.getInt("id"));
			rowUnder.putInt("id",upId);
		
			mCurrentSelection++;
			updateRow(rowOnTop, false);
			updateRow(rowUnder, false);
			
			return mCurrentSelection;
		}else return position;
    }
    
    //returns new position
    public int moveToTop(int position)
    {
		if(position > 0)
		{
			//get the row
			Bundle rowToMove = getRow(position, false);
			//set its created time to now
			rowToMove.putLong(Browser.BookmarkColumns.CREATED, new Date().getTime());
			//update the row
			updateRow(rowToMove, true);
			
			mCurrentSelection=0;
			return mCurrentSelection;
		}else return position;
    }
    
    //returns new post
    public int moveToBottom(int position)
    {
		if(position < mCount-1)
		{
			//get the time the last row in the list was created
			Bundle lastRow = getRow(mCount-1, true);
			Long createTime = lastRow.getLong(Browser.BookmarkColumns.CREATED);

			long newCreateTime = (createTime==null || createTime ==0)?0:createTime-1;
			//set the create time of our row to older than the last on the list
			Bundle rowToMove = getRow(position, false);
			rowToMove.putLong(Browser.BookmarkColumns.CREATED, newCreateTime);
			updateRow(rowToMove, true);

			mCurrentSelection=mCount-1;
			return mCurrentSelection;
		}
		else return position;
    }
    
    public String launchUrlOfItem(int position)
    {
    	try
    	{
    		return getRow(position, false).getString(Browser.BookmarkColumns.URL);
    	}catch(Exception e){ return null;}
    }
}

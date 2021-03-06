package com.adrenalus.redditshare;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v4.util.LruCache;
import android.widget.ImageView;

public class ImageCache {
    private static LruCache<String, Bitmap>     mMemoryCache    = null;
    private static int                          CACHE_SIZE       = 1024 * 1024 * 10;

    private static class AsyncLoader extends AsyncTask<String, Void, Bitmap> {
        private ImageView   mTarget;

        public AsyncLoader(ImageView target) {
            mTarget = target;
        }

        @Override
        protected void onPreExecute() {
            mTarget.setTag(this);
        }

        @Override
        protected Bitmap doInBackground(String...urls) {
            String url = urls[0];

            Bitmap result = null;

            if (url != null) {
                result = load(url);

                if (result != null) {
                    mMemoryCache.put(url, result);
                }
            }

            return result;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (mTarget.getTag() == this) {
                mTarget.setTag(null);
                if (result != null) mTarget.setImageBitmap(result);
            } else if (mTarget.getTag() != null) {
                ((AsyncLoader) mTarget.getTag()).cancel(true);
                mTarget.setTag(null);
            }
        }
    }

    public static Bitmap load(String urlString) {
        if (urlString == null || urlString.length() == 0) return null;

        Bitmap bitmap = null;
        URL url = null;

        try {
            url = new URL(urlString);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        try {
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.connect();
            InputStream is = conn.getInputStream();
            bitmap = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return bitmap;
    }

    public static void load(String url, ImageView view) {
        if (url == null || url.length() == 0) return;
        if (mMemoryCache == null) {
            mMemoryCache = new LruCache<String, Bitmap>(CACHE_SIZE) {
                @Override
                protected int sizeOf(String key, Bitmap bitmap) {
                    return (bitmap.getRowBytes() * bitmap.getHeight());
                }
            };
        }

        Bitmap bitmap = mMemoryCache.get(url);
        if (bitmap == null) {
            final AsyncLoader task = (AsyncLoader) new AsyncLoader(view);
            view.setTag(task);
            task.execute(url);
        } else {
            view.setImageBitmap(bitmap);
        }
    }
}
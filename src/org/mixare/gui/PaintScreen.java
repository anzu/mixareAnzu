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
package org.mixare.gui;

import org.mixare.MixContext;
import org.mixare.R;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;

public class PaintScreen {
	Canvas canvas;
	int width, height;
	Paint paint = new Paint();
	Paint bPaint = new Paint();
	/**current context */
	private MixContext mixContext;

	public PaintScreen(MixContext ctx) {
		paint.setTextSize(16);
		paint.setAntiAlias(true);
		paint.setColor(Color.BLUE);
		paint.setStyle(Paint.Style.STROKE);
		this.mixContext = ctx;
	}

	public Canvas getCanvas() {
		return canvas;
	}

	public void setCanvas(Canvas canvas) {
		this.canvas = canvas;
	}

	public void setWidth(int width) {
		this.width = width;
	}

	public void setHeight(int height) {
		this.height = height;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public void setFill(boolean fill) {
		if (fill)
			paint.setStyle(Paint.Style.FILL);
		else
			paint.setStyle(Paint.Style.STROKE);
	}

	public void setColor(int c) {
		paint.setColor(c);
	}

	public void setStrokeWidth(float w) {
		paint.setStrokeWidth(w);
	}

	public void paintLine(float x1, float y1, float x2, float y2) {
		canvas.drawLine(x1, y1, x2, y2, paint);
	}

	public void paintRect(float x, float y, float width, float height) {
		canvas.drawRect(x, y, x + width, y + height, paint);
	}
	
	public void paintBitmap(Bitmap bitmap, float left, float top) {
		canvas.drawBitmap(bitmap, left, top, paint);
	}
	
	public void paintPath(Path path,float x, float y, float width, float height, float rotation, float scale) {
		canvas.save();
		canvas.translate(x + width / 2, y + height / 2);
		canvas.rotate(rotation);
		canvas.scale(scale, scale);
		canvas.translate(-(width / 2), -(height / 2));
		canvas.drawPath(path, paint);
		canvas.restore();
	}

	public void paintCircle(float x, float y, float radius) {
		canvas.drawCircle(x, y, radius, paint);
	}

	public void paintText(float x, float y, String text) {
		canvas.drawText(text, x, y, paint);
	}

	public void paintObj(ScreenObj obj, float x, float y, float rotation,
			float scale) {
		canvas.save();
		canvas.translate(x + obj.getWidth() / 2, y + obj.getHeight() / 2);
		canvas.rotate(rotation);
		canvas.scale(scale, scale);
		canvas.translate(-(obj.getWidth() / 2), -(obj.getHeight() / 2));
		obj.paint(this);
		canvas.restore();
	}

	public float getTextWidth(String txt) {
		return paint.measureText(txt);
	}

	public float getTextAsc() {
		return -paint.ascent();
	}

	public float getTextDesc() {
		return paint.descent();
	}

	public float getTextLead() {
		return 0;
	}

	public void setFontSize(float size) {
		paint.setTextSize(size);
	}
	
	public void paintPin(float x, float y, String  pin, double scale) {
        //Bitmap bmp = Bitmap.createBitmap(368, 456, Bitmap.Config.RGB_565);
        Bitmap bmp = null;
		
		if (pin.trim().equalsIgnoreCase("Events")) {
			bmp = BitmapFactory.decodeResource(mixContext.getResources(), R.drawable.ar_events);
		}
		else if (pin.trim().equalsIgnoreCase("Merchandise")) {
			bmp = BitmapFactory.decodeResource(mixContext.getResources(), R.drawable.ar_merch);
		}
		else if (pin.trim().equalsIgnoreCase("Refreshments")) {
			bmp = BitmapFactory.decodeResource(mixContext.getResources(), R.drawable.ar_refreshments);
		}
		else if (pin.trim().equalsIgnoreCase("Restrooms")) {
			bmp = BitmapFactory.decodeResource(mixContext.getResources(), R.drawable.ar_restroom);
		}
		else if (pin.trim().equalsIgnoreCase("Social")) {
			bmp = BitmapFactory.decodeResource(mixContext.getResources(), R.drawable.ar_social);
		}
		if (bmp!= null) {
			bmp = resizePin(bmp, scale); //Resize pin for better AR visibility
			canvas.drawBitmap(bmp, x, (y - 60), paint); // SUBTRACT to y to RAISE the pin above title text
			//Log.i("Mixare", "Marker.java // AR Pin Drawn");
		}
	}
	public Bitmap resizePin(Bitmap b, double scale) {
		
		if (b != null) {
		// load the original BitMap (368 x 456 px)
        
        int width = b.getWidth();
        int height = b.getHeight();
        int newWidth = (int) (width * scale); // 120;
        int newHeight = (int) (height * scale); // 150;
        
        // calculate the scale - in this case = 0.4f
        float scaleWidth = ((float) newWidth) / width;
        float scaleHeight = ((float) newHeight) / height;
        
        // create a matrix for the manipulation
        Matrix matrix = new Matrix();
        // resize the bit map
        matrix.postScale(scaleWidth, scaleHeight);
        // rotate the Bitmap
        // matrix.postRotate(45);

        // recreate the new Bitmap
        Bitmap resizedBitmap = Bitmap.createBitmap(b, 0, 0, 
                          width, height, matrix, true); 
   
		return resizedBitmap;
		}
			return null;
	}
}
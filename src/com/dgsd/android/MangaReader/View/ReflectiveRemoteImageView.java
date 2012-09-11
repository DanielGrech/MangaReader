package com.dgsd.android.MangaReader.View;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import com.handlerexploit.prime.widgets.RemoteImageView;
import com.nineoldandroids.view.ViewHelper;

public class ReflectiveRemoteImageView extends RemoteImageView {

    private Paint mPaint;

    public ReflectiveRemoteImageView(Context context) {
        super(context);
        init();
    }

    public ReflectiveRemoteImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ReflectiveRemoteImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        ViewHelper.setRotationY(this, 25);

        mPaint = new Paint();
        mPaint.setAntiAlias(true);
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(createReflection(bm));
    }

    /**
     * Creates a copy of the image with a reflection at the bottom.
     *
     * Kudos to http://www.inter-fuser.com/2009/12/android-reflections-with-bitmaps.html
     */
    private Bitmap createReflection(Bitmap originalImage) {
        //The gap we want between the reflection and the original image
        final int reflectionGap = 0;


        int width = originalImage.getWidth();
        int height = originalImage.getHeight();


        //This will not scale but will flip on the Y axis
        Matrix matrix = new Matrix();
        matrix.preScale(1, -1);

        //Create a Bitmap with the flip matix applied to it.
        //We only want the bottom half of the image
        Bitmap reflectionImage = Bitmap.createBitmap(originalImage, 0, height / 5, width, height / 5, matrix, false);

        //Create a new bitmap with same width but taller to fit reflection
        Bitmap bitmapWithReflection = Bitmap.createBitmap(width, (height + height / 5), Bitmap.Config.ARGB_8888);

        //Create a new Canvas with the bitmap that's big enough for
        //the image plus gap plus reflection
        Canvas canvas = new Canvas(bitmapWithReflection);

        //Draw in the original image
        canvas.drawBitmap(originalImage, 0, 0, null);

        //Draw in the gap
        canvas.drawRect(0, height, width, height + reflectionGap, mPaint);

        //Draw in the reflection
        canvas.drawBitmap(reflectionImage, 0, height + reflectionGap, null);

        //Create a shader that is a linear gradient that covers the reflection
        Paint paint = new Paint();
        LinearGradient shader = new LinearGradient(0, originalImage.getHeight(), 0,
                bitmapWithReflection.getHeight() + reflectionGap, 0x30ffffff, 0x00ffffff,
                Shader.TileMode.CLAMP);
        //Set the paint to use this shader (linear gradient)
        paint.setShader(shader);
        //Set the Transfer mode to be porter duff and destination in
        paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        //Draw a rectangle using the paint with our linear gradient
        canvas.drawRect(0, height, width, bitmapWithReflection.getHeight() + reflectionGap, paint);

        return bitmapWithReflection;
    }
}

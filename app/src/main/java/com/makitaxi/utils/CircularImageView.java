package com.makitaxi.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatImageView;

public class CircularImageView extends AppCompatImageView {

    private Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int borderColor = 0x343B71;
    private float borderWidth = 5f;

    private BitmapShader shader;
    private int lastBitmapHash = 0;

    private Matrix shaderMatrix = new Matrix();

    public CircularImageView(Context context) {
        super(context);
        init();
    }

    public CircularImageView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CircularImageView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setColor(borderColor);
        borderPaint.setStrokeWidth(borderWidth);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null || getWidth() == 0 || getHeight() == 0) return;

        Bitmap bitmap = getBitmapFromDrawable(drawable);
        if (bitmap == null) return;

        int bitmapHash = bitmap.hashCode();
        if (shader == null || bitmapHash != lastBitmapHash) {
            int size = Math.min(getWidth(), getHeight());
            Bitmap scaledBitmap = getScaledBitmap(bitmap, size);
            shader = new BitmapShader(scaledBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            lastBitmapHash = bitmapHash;
        }

        paint.setShader(shader);

        float radius = Math.min(getWidth(), getHeight()) / 2f;
        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float borderRadius = radius - borderWidth / 2f;

        canvas.drawCircle(cx, cy, borderRadius, paint);
        canvas.drawCircle(cx, cy, borderRadius, borderPaint);
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap = Bitmap.createBitmap(
                    drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888
            );
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Bitmap getScaledBitmap(Bitmap bitmap, int size) {
        if (bitmap.getWidth() == size && bitmap.getHeight() == size) {
            return bitmap;
        }

        float scale;
        float dx = 0, dy = 0;

        int bWidth = bitmap.getWidth();
        int bHeight = bitmap.getHeight();

        if (bWidth * size > size * bHeight) {
            scale = (float) size / (float) bHeight;
            dx = (size - bWidth * scale) * 0.5f;
        } else {
            scale = (float) size / (float) bWidth;
            dy = (size - bHeight * scale) * 0.5f;
        }

        shaderMatrix.setScale(scale, scale);
        shaderMatrix.postTranslate(Math.round(dx), Math.round(dy));

        Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(output);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        BitmapShader tempShader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        tempShader.setLocalMatrix(shaderMatrix);
        paint.setShader(tempShader);

        float radius = size / 2f;
        canvas.drawCircle(radius, radius, radius, paint);

        return output;
    }
}

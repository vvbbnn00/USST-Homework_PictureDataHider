package cn.vvbbnn00.picture_data_hider.utils;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.*;
import org.opencv.imgproc.Imgproc;


import java.util.ArrayList;
import java.util.List;

public class FFTWatermarkHelper {


    private static final List<Mat> planes = new ArrayList<>();
    private static final List<Mat> allPlanes = new ArrayList<>();


    /**
     * Add watermark to bitmap and return
     * @param bt bitmap
     * @param waterMark watermark
     * @return bitmap
     */
    public static Bitmap doAddWatermark(Bitmap bt, String waterMark) {
        Mat src = new Mat(bt.getHeight(), bt.getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(bt, src);

        Mat imageMat = addImageWatermarkWithText(src, waterMark);
        Bitmap bt3 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imageMat, bt3);

        return bt3;
    }

    /**
     * Get watermark from bitmap and return
     * @param bt bitmap
     * @return bitmap
     */
    public static Bitmap doGetWatermark(Bitmap bt) {
        Mat src = new Mat(bt.getHeight(), bt.getWidth(), CvType.CV_8U);
        Utils.bitmapToMat(bt, src);

        Mat imageMat = getImageWatermarkWithText(src);
        Bitmap bt3 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.RGB_565);
        Utils.matToBitmap(imageMat, bt3);

        return bt3;
    }


    // The following code is referenced from https://www.jianshu.com/p/341dc97801ee


    public static Mat addImageWatermarkWithText(Mat image, String watermarkText) {
        Mat complexImage = new Mat();
        // 优化图像的尺寸
        Mat padded = splitSrc(image);
        padded.convertTo(padded, CvType.CV_32F);
        planes.add(padded);
        planes.add(Mat.zeros(padded.size(), CvType.CV_32F));
        Core.merge(planes, complexImage);
        // dft
        Core.dft(complexImage, complexImage);
        //  添加文本水印
        Scalar scalar = new Scalar(0, 0, 0);
        Point point = new Point(40, 40);

        Imgproc.putText(complexImage, watermarkText,
                point, Imgproc.FONT_HERSHEY_DUPLEX, 1f, scalar);
        Core.flip(complexImage, complexImage, -1);

        Imgproc.putText(complexImage, watermarkText,
                point, Imgproc.FONT_HERSHEY_DUPLEX, 1f, scalar);
        Core.flip(complexImage, complexImage, -1);

        return antiTransformImage(complexImage);
    }

    public static Mat getImageWatermarkWithText(Mat image) {
        List<Mat> planes = new ArrayList<>();
        Mat complexImage = new Mat();
        Mat padded = splitSrc(image);
        padded.convertTo(padded, CvType.CV_32F);
        planes.add(padded);
        planes.add(Mat.zeros(padded.size(), CvType.CV_32F));
        Core.merge(planes, complexImage);
        // dft
        Core.dft(complexImage, complexImage);
        Mat magnitude = createOptimizedMagnitude(complexImage);
        planes.clear();
        return magnitude;
    }

    private static Mat splitSrc(Mat mat) {
        mat = optimizeImageDim(mat);
        Core.split(mat, allPlanes);
        Mat padded;
        if (allPlanes.size() > 1) {
            padded = allPlanes.get(0);
        } else {
            padded = mat;
        }
        return padded;
    }

    private static Mat antiTransformImage(Mat complexImage) {
        Mat invDFT = new Mat();
        Core.idft(complexImage, invDFT, Core.DFT_SCALE | Core.DFT_REAL_OUTPUT, 0);
        Mat restoredImage = new Mat();
        invDFT.convertTo(restoredImage, CvType.CV_8U);
        if (FFTWatermarkHelper.allPlanes.isEmpty()) {
            FFTWatermarkHelper.allPlanes.add(restoredImage);
        } else {
            FFTWatermarkHelper.allPlanes.set(0, restoredImage);
        }
        Mat lastImage = new Mat();
        Core.merge(FFTWatermarkHelper.allPlanes, lastImage);
        return lastImage;
    }


    private static Mat optimizeImageDim(Mat image) {
        Mat padded = new Mat();
        int addPixelRows = Core.getOptimalDFTSize(image.rows());
        int addPixelCols = Core.getOptimalDFTSize(image.cols());
        Core.copyMakeBorder(image, padded, 0, addPixelRows - image.rows(), 0, addPixelCols - image.cols(),
                Core.BORDER_REPLICATE, Scalar.all(0));

        return padded;
    }

    private static Mat createOptimizedMagnitude(Mat complexImage) {
        List<Mat> newPlanes = new ArrayList<>();
        Mat mag = new Mat();
        Core.split(complexImage, newPlanes);
        Core.magnitude(newPlanes.get(0), newPlanes.get(1), mag);
        Core.add(Mat.ones(mag.size(), CvType.CV_32F), mag, mag);
        Core.log(mag, mag);
        shiftDFT(mag);
        mag.convertTo(mag, CvType.CV_8UC1);
        Core.normalize(mag, mag, 0, 255, Core.NORM_MINMAX, CvType.CV_8UC1);
        return mag;
    }

    private static void shiftDFT(Mat image) {
        image = image.submat(new Rect(0, 0, image.cols() & -2, image.rows() & -2));
        int cx = image.cols() / 2;
        int cy = image.rows() / 2;

        Mat q0 = new Mat(image, new Rect(0, 0, cx, cy));
        Mat q1 = new Mat(image, new Rect(cx, 0, cx, cy));
        Mat q2 = new Mat(image, new Rect(0, cy, cx, cy));
        Mat q3 = new Mat(image, new Rect(cx, cy, cx, cy));

        Mat tmp = new Mat();
        q0.copyTo(tmp);
        q3.copyTo(q0);
        tmp.copyTo(q3);
        q1.copyTo(tmp);
        q2.copyTo(q1);
        tmp.copyTo(q2);
    }

}

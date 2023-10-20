package cn.vvbbnn00.picture_data_hider.utils;

import android.graphics.Bitmap;

import org.opencv.android.Utils;
import org.opencv.core.*;


import java.util.ArrayList;
import java.util.List;

public class FFTWatermarkHelper {


    private static final List<Mat> planes = new ArrayList<Mat>();
    private static final List<Mat> allPlanes = new ArrayList<Mat>();


    public static Mat bitmap2Mat(Bitmap bitmap) {
        Mat mat = new Mat();
        Utils.bitmapToMat(bitmap, mat);
        return mat;
    }

    public static Bitmap mat2Bitmap(Mat mat) {
        Bitmap bitmap = Bitmap.createBitmap(mat.cols(), mat.rows(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(mat, bitmap);
        return bitmap;
    }

    public static Mat addImageWatermarkWithText(Mat image, String watermarkText) {
        Mat complexImage = new Mat();

        image.convertTo(image, CvType.CV_32F);
        planes.add(image);
        planes.add(Mat.zeros(image.size(), CvType.CV_32F));
        Core.merge(planes, complexImage);
        // dft
        Core.dft(complexImage, complexImage);
        // 添加文本水印
        Scalar scalar = new Scalar(0, 0, 0);
        Point point = new Point(40, 40);
//        Core.putText(complexImage, watermarkText, point, Core.FONT_HERSHEY_DUPLEX, 1D, scalar);
//        Core.flip(complexImage, complexImage, -1);
//        Core.putText(complexImage, watermarkText, point, Core.FONT_HERSHEY_DUPLEX, 1D, scalar);
        Core.flip(complexImage, complexImage, -1);
        return antitransformImage(complexImage);
    }


    public static Mat getImageWatermarkWithText(Mat image) {
        List<Mat> planes = new ArrayList<Mat>();
        Mat complexImage = new Mat();

        image.convertTo(image, CvType.CV_32F);
        planes.add(image);
        planes.add(Mat.zeros(image.size(), CvType.CV_32F));
        Core.merge(planes, complexImage);
        // dft
        Core.dft(complexImage, complexImage);
        Mat magnitude = createOptimizedMagnitude(complexImage);
        planes.clear();
        return magnitude;
    }

    public static Mat antitransformImage(Mat complexImage) {
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

    public static Mat createOptimizedMagnitude(Mat complexImage) {
        List<Mat> newPlanes = new ArrayList<Mat>();
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

    public static void shiftDFT(Mat image) {
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

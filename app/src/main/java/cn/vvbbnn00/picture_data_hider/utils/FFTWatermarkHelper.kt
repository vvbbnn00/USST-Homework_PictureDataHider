package cn.vvbbnn00.picture_data_hider.utils

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.core.Rect
import org.opencv.core.Scalar
import org.opencv.imgproc.Imgproc

object FFTWatermarkHelper {
    private val planes: MutableList<Mat> = ArrayList()
    private val allPlanes: MutableList<Mat> = ArrayList()

    /**
     * Add watermark to bitmap and return
     * @param bt bitmap
     * @param waterMark watermark
     * @return bitmap
     */
    fun doAddWatermark(bt: Bitmap, waterMark: String?): Bitmap {
        val src = Mat(bt.getHeight(), bt.getWidth(), CvType.CV_8U)
        Utils.bitmapToMat(bt, src)
        val imageMat = addImageWatermarkWithText(src, waterMark)
        val bt3 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.RGB_565)
        Utils.matToBitmap(imageMat, bt3)
        return bt3
    }

    /**
     * Get watermark from bitmap and return
     * @param bt bitmap
     * @return bitmap
     */
    fun doGetWatermark(bt: Bitmap): Bitmap {
        val src = Mat(bt.getHeight(), bt.getWidth(), CvType.CV_8U)
        Utils.bitmapToMat(bt, src)
        val imageMat = getImageWatermarkWithText(src)
        val bt3 = Bitmap.createBitmap(imageMat.cols(), imageMat.rows(), Bitmap.Config.RGB_565)
        Utils.matToBitmap(imageMat, bt3)
        return bt3
    }

    // The following code is referenced from https://www.jianshu.com/p/341dc97801ee
    private fun addImageWatermarkWithText(image: Mat, watermarkText: String?): Mat {
        val complexImage = Mat()
        // 优化图像的尺寸
        val padded = splitSrc(image)
        padded.convertTo(padded, CvType.CV_32F)
        planes.add(padded)
        planes.add(Mat.zeros(padded.size(), CvType.CV_32F))
        Core.merge(planes, complexImage)
        // dft
        Core.dft(complexImage, complexImage)
        //  添加文本水印
        val scalar = Scalar(0.0, 0.0, 0.0)
        val point = Point(40.0, 40.0)
        Imgproc.putText(
            complexImage, watermarkText,
            point, Imgproc.FONT_HERSHEY_DUPLEX, 1.0, scalar
        )
        Core.flip(complexImage, complexImage, -1)
        Imgproc.putText(
            complexImage, watermarkText,
            point, Imgproc.FONT_HERSHEY_DUPLEX, 1.0, scalar
        )
        Core.flip(complexImage, complexImage, -1)
        return antiTransformImage(complexImage)
    }

    private fun getImageWatermarkWithText(image: Mat): Mat {
        val planes: MutableList<Mat> = ArrayList()
        val complexImage = Mat()
        val padded = splitSrc(image)
        padded.convertTo(padded, CvType.CV_32F)
        planes.add(padded)
        planes.add(Mat.zeros(padded.size(), CvType.CV_32F))
        Core.merge(planes, complexImage)
        // dft
        Core.dft(complexImage, complexImage)
        val magnitude = createOptimizedMagnitude(complexImage)
        planes.clear()
        return magnitude
    }

    private fun splitSrc(matObject: Mat): Mat {
        var mat = matObject
        mat = optimizeImageDim(mat)
        Core.split(mat, allPlanes)
        val padded: Mat = if (allPlanes.size > 1) {
            allPlanes[0]
        } else {
            mat
        }
        return padded
    }

    private fun antiTransformImage(complexImage: Mat): Mat {
        val invDFT = Mat()
        Core.idft(complexImage, invDFT, Core.DFT_SCALE or Core.DFT_REAL_OUTPUT, 0)
        val restoredImage = Mat()
        invDFT.convertTo(restoredImage, CvType.CV_8U)
        if (allPlanes.isEmpty()) {
            allPlanes.add(restoredImage)
        } else {
            allPlanes[0] = restoredImage
        }
        val lastImage = Mat()
        Core.merge(allPlanes, lastImage)
        return lastImage
    }

    private fun optimizeImageDim(image: Mat): Mat {
        val padded = Mat()
        val addPixelRows = Core.getOptimalDFTSize(image.rows())
        val addPixelCols = Core.getOptimalDFTSize(image.cols())
        Core.copyMakeBorder(
            image, padded, 0, addPixelRows - image.rows(), 0, addPixelCols - image.cols(),
            Core.BORDER_REPLICATE, Scalar.all(0.0)
        )
        return padded
    }

    private fun createOptimizedMagnitude(complexImage: Mat): Mat {
        val newPlanes: List<Mat> = ArrayList()
        val mag = Mat()
        Core.split(complexImage, newPlanes)
        Core.magnitude(newPlanes[0], newPlanes[1], mag)
        Core.add(Mat.ones(mag.size(), CvType.CV_32F), mag, mag)
        Core.log(mag, mag)
        shiftDFT(mag)
        mag.convertTo(mag, CvType.CV_8UC1)
        Core.normalize(mag, mag, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)
        return mag
    }

    private fun shiftDFT(imageObject: Mat) {
        var image = imageObject
        image = image.submat(Rect(0, 0, image.cols() and -2, image.rows() and -2))
        val cx = image.cols() / 2
        val cy = image.rows() / 2
        val q0 = Mat(image, Rect(0, 0, cx, cy))
        val q1 = Mat(image, Rect(cx, 0, cx, cy))
        val q2 = Mat(image, Rect(0, cy, cx, cy))
        val q3 = Mat(image, Rect(cx, cy, cx, cy))
        val tmp = Mat()
        q0.copyTo(tmp)
        q3.copyTo(q0)
        tmp.copyTo(q3)
        q1.copyTo(tmp)
        q2.copyTo(q1)
        tmp.copyTo(q2)
    }
}

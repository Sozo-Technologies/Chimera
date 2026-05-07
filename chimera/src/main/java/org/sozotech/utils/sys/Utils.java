package org.sozotech.utils.sys;

import org.opencv.core.Mat;

import java.awt.image.BufferedImage;

public class Utils {

    public static BufferedImage matToBufferedImage(Mat mat) {

        int type = BufferedImage.TYPE_3BYTE_BGR;

        byte[] data = new byte[
                mat.rows() *
                        mat.cols() *
                        (int) mat.elemSize()
                ];

        mat.get(0, 0, data);

        BufferedImage image = new BufferedImage(
                mat.cols(),
                mat.rows(),
                type
        );

        image.getRaster().setDataElements(
                0,
                0,
                mat.cols(),
                mat.rows(),
                data
        );

        return image;
    }
}
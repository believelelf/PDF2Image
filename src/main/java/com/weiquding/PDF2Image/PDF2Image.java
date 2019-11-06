package com.weiquding.PDF2Image;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDTrueTypeFont;
import org.apache.pdfbox.pdmodel.font.encoding.WinAnsiEncoding;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 解决PDF转Image，仿宋字段乱码问题
 * <p>
 * 控制台异常信息：
 * 11月 06, 2019 10:45:14 下午 org.apache.pdfbox.pdmodel.font.FileSystemFontProvider loadDiskCache
 * 警告: New fonts found, font cache will be re-built
 * 11月 06, 2019 10:45:14 下午 org.apache.pdfbox.pdmodel.font.FileSystemFontProvider <init>
 * 警告: Building on-disk font cache, this may take a while
 * 11月 06, 2019 10:45:18 下午 org.apache.pdfbox.pdmodel.font.FileSystemFontProvider <init>
 * 警告: Finished building on-disk font cache, found 245 fonts
 * 11月 06, 2019 10:45:18 下午 org.apache.pdfbox.pdmodel.font.PDCIDFontType0 <init>
 * 警告: Using fallback MT-Extra for CID-keyed font STSong-Light
 * 11月 06, 2019 10:45:24 下午 org.apache.pdfbox.rendering.CIDType0Glyph2D getPathForCharacterCode
 * 警告: No glyph for 22826 (CID 0dfe) in font STSong-Light
 * 11月 06, 2019 10:45:24 下午 org.apache.pdfbox.rendering.CIDType0Glyph2D getPathForCharacterCode
 * 警告: No glyph for 24179 (CID 0bdc) in font STSong-Light
 * <p>
 * 解决方案：
 */
public class PDF2Image {

    private static final Logger LOGGER = LoggerFactory.getLogger(PDF2Image.class);

    private static final int POINTS_IN_INCH = 72;

    public static void pdf2Image(String filename) {
        try (PDDocument document = PDDocument.load(PDF2Image.class.getResourceAsStream("/" + filename))) {
            PDTrueTypeFont.load(document, PDF2Image.class.getResourceAsStream("/chinese.stsong.ttf"), WinAnsiEncoding.INSTANCE);
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCounter = 0;
            for (PDPage page : document.getPages()) {
                // note that the page number parameter is zero based
                BufferedImage bim = pdfRenderer.renderImageWithDPI(pageCounter, 300, ImageType.RGB);
                //BufferedImage bim = pdfRenderer.renderImage(pageCounter);
                //BufferedImage bim = extract(pdfRenderer, pageCounter, 300);
                // suffix in filename will be used as the file format
                ImageIOUtil.writeImage(bim, filename + "-" + (pageCounter++) + ".png", 300);
            }
        } catch (IOException e) {
            LOGGER.error("render image error:[{}]", filename, e);
        }

    }

    private static BufferedImage extract(PDFRenderer renderer, int pageIndex, int dpi) throws IOException {
        Rectangle2D rect = new Rectangle2D.Float(0, 0, 960, 120);

        double scale = dpi / POINTS_IN_INCH;
        double bitmapWidth  = rect.getWidth()  * scale;
        double bitmapHeight = rect.getHeight() * scale;
        BufferedImage image = new BufferedImage((int)bitmapWidth, (int)bitmapHeight, BufferedImage.TYPE_INT_RGB);

        AffineTransform transform = AffineTransform.getScaleInstance(scale, scale);
        transform.concatenate(AffineTransform.getTranslateInstance(-rect.getX(), -rect.getY()));

        Graphics2D graphics = image.createGraphics();
        graphics.setBackground(Color.WHITE);
        graphics.setTransform(transform);

        renderer.renderPageToGraphics(pageIndex, graphics);
        graphics.dispose();
        return image;
    }



    public static void main(String[] args) {
        pdf2Image("525d747dd4fa44b5867241b664b779f0.pdf");
    }


}

package com.weiquding.PDF2Image;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.tools.imageio.ImageIOUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.IOException;

/**
 * 解决PDF转Image，STSong-Light字体乱码问题
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
 *
 * 原因：
 *    STSong-Light为Adobe版本字体，只能用于Adobe Reader，其他软件使用时只能找替换字体。（见Cmap_info.txt）
 * 解决方案：
 *    pdfbox在FontMapperImpl里提供了两种字体容错方案
 *      1. 字体替换映射：substitutes
 *      2. 基于规则自动查找匹配字体：org.apache.pdfbox.pdmodel.font.FontMapperImpl#getFontMatches(org.apache.pdfbox.pdmodel.font.PDFontDescriptor, org.apache.pdfbox.pdmodel.font.PDCIDSystemInfo)
 *    但存在的问题是：
 *      1. substitutes只支持14种标准字符，STSong-Light不在其中，且final class FontMapperImpl，在包外不可见。
 *      2. 基于规则自动查找匹配字体,在匹配规则中fallback的字体不能适配到合适字体，
 *         优先级高的字体为MalgunGothic-Semilight等，其存在适配性问题。
 *         另外PDCIDFontType0#PDCIDFontType0(COSDictionary, PDType0Font)创建时字体时对于PDFontDescriptor没有提供设置接口，对于匹配规则修改很困难。
 *    基于目前的了解，较好的方案为将FontMapperImpl复制到当前工程目录，在substitutes中增加映射字体
 *       substitutes.put("STSong-Light", Arrays.asList("STFangsong"));
 *  后续改进建议：
 *  1. 针对substitutes暴露修改接口，这一点在1.8版本的官方文档里有提到映射修改机制，但2.0里去除了。
 *  ~~~
 *  PDFBox will load Resources/PDFBox_External_Fonts.properties off of the classpath to map font names to TTF font files. The UNKNOWN_FONT property in that file will tell PDFBox which font to use when no mapping exists.
 *  ~~~
 *  2. 优化getFontMatches匹配算法
 * 参考资料：
 * 1. [cmap_info.txt](https://github.com/itext/itext7/blob/develop/font-asian/src/main/resources/com/itextpdf/io/font/cmap_info.txt)
 *
 * 注意：
 *    由于数据安全问题，525d747dd4fa44b5867241b664b779f0.pdf未在仓库中包含。pdf示例文件请自行创建。
 */

public class PDF2Image {

    private static final Logger LOGGER = LoggerFactory.getLogger(PDF2Image.class);

    private static final int POINTS_IN_INCH = 72;

    public static void pdf2Image(String filename) {
        try (PDDocument document = PDDocument.load(PDF2Image.class.getResourceAsStream("/" + filename))) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            int pageCounter = 0;
            for (PDPage page : document.getPages()) {
                // note that the page number parameter is zero based
                BufferedImage bim = pdfRenderer.renderImageWithDPI(pageCounter, 300, ImageType.RGB);
                // suffix in filename will be used as the file format
                ImageIOUtil.writeImage(bim, filename + "-" + (pageCounter++) + ".png", 300);
            }
        } catch (IOException e) {
            LOGGER.error("render image error:[{}]", filename, e);
        }

    }

    public static void main(String[] args) {
        pdf2Image("525d747dd4fa44b5867241b664b779f0.pdf");
    }


}

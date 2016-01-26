package com.itextpdf.core.font;

import com.itextpdf.basics.PdfException;
import com.itextpdf.basics.font.FontEncoding;
import com.itextpdf.basics.font.otf.Glyph;
import com.itextpdf.basics.geom.Rectangle;
import com.itextpdf.core.pdf.PdfArray;
import com.itextpdf.core.pdf.PdfDictionary;
import com.itextpdf.core.pdf.PdfDocument;
import com.itextpdf.core.pdf.PdfName;

/**
 * Low-level API class for Type 3 fonts.
 * <p/>
 * In Type 3 fonts, glyphs are defined by streams of PDF graphics operators.
 * These streams are associated with character names. A separate encoding entry
 * maps character codes to the appropriate character names for the glyphs.
 */
public class PdfType3Font extends PdfSimpleFont<Type3FontProgram> {

    private double[] fontMatrix = {0.001, 0, 0, 0.001, 0, 0};

    /**
     * Creates a Type3 font.
     *
     * @param colorized defines whether the glyph color is specified in the glyph descriptions in the font.
     */
    public PdfType3Font(PdfDocument document, boolean colorized) {
        super();
        makeIndirect(document);
        fontProgram = new Type3FontProgram(colorized);
        fontEncoding = FontEncoding.createEmptyFontEncoding();
        subset = true;
    }

    /**
     * Creates a Type3 font based on an existing font dictionary.
     *
     * @param fontDictionary a dictionary of type <code>/Font</code>
     */
    public PdfType3Font(PdfDictionary fontDictionary) {
        super(fontDictionary);
        checkFontDictionary(fontDictionary, PdfName.Type3);
        fontProgram = new Type3FontProgram(false);
    }

    public Type3Glyph getType3Glyph(int unicode) {
        return fontProgram.getType3Glyph(unicode);
    }

    @Override
    public boolean isSubset() {
        return true;
    }

    @Override
    public boolean isEmbedded() {
        return true;
    }

    @Override
    public double[] getFontMatrix() {
        return this.fontMatrix;
    }

    public void setFontMatrix(double[] fontMatrix) {
        this.fontMatrix = fontMatrix;
    }

    /**
     * Defines a glyph. If the character was already defined it will return the same content
     *
     * @param c   the character to match this glyph.
     * @param wx  the advance this character will have
     * @param llx the X lower left corner of the glyph bounding box. If the <CODE>colorize</CODE> option is
     *            <CODE>true</CODE> the value is ignored
     * @param lly the Y lower left corner of the glyph bounding box. If the <CODE>colorize</CODE> option is
     *            <CODE>true</CODE> the value is ignored
     * @param urx the X upper right corner of the glyph bounding box. If the <CODE>colorize</CODE> option is
     *            <CODE>true</CODE> the value is ignored
     * @param ury the Y upper right corner of the glyph bounding box. If the <CODE>colorize</CODE> option is
     *            <CODE>true</CODE> the value is ignored
     * @return a content where the glyph can be defined
     */
    public Type3Glyph addGlyph(char c, int wx, int llx, int lly, int urx, int ury) {
        Type3Glyph glyph = getType3Glyph(c);
        if (glyph != null) {
            return glyph;
        }
        int code = fontEncoding.getFirstEmptyCode();
        glyph = new Type3Glyph(getDocument(), wx, llx, lly, urx, ury, fontProgram.isColorized());
        fontProgram.addGlyph(code, c, wx, new int[]{llx, lly, urx, ury}, glyph);
        fontEncoding.addSymbol((byte) code, c);

        if (!fontProgram.isColorized()) {
            Rectangle bbox = fontProgram.getFontMetrics().getBbox();
            if (fontProgram.countOfGlyphs() == 0) {
                bbox.setBbox(llx, lly, urx, ury);
            } else {
                float newLlx = Math.min(bbox.getLeft(), llx);
                float newLly = Math.min(bbox.getBottom(), lly);
                float newUrx = Math.max(bbox.getRight(), urx);
                float newUry = Math.max(bbox.getTop(), ury);
                bbox.setBbox(newLlx, newLly, newUrx, newUry);
            }
        }
        return glyph;
    }

    @Override
    public Glyph getGlyph(int unicode) {
        if (fontEncoding.canEncode(unicode)) {
            return getFontProgram().getGlyph(unicode);
        }
        return null;
    }

    @Override
    protected PdfDictionary getFontDescriptor(String fontName) {
        return null;
    }

    @Override
    protected void addFontStream(PdfDictionary fontDescriptor) {
    }

    @Override
    public void flush() {
        if (fontProgram.getGlyphsCount() < 1) {
            throw new PdfException("no.glyphs.defined.fo r.type3.font");
        }
        PdfDictionary charProcs = new PdfDictionary();
        for (int i = 0; i < 256; i++) {
            if (fontEncoding.canDecode((byte) i)) {
                Type3Glyph glyph = getType3Glyph(fontEncoding.getUnicode(i));
                charProcs.put(new PdfName(fontEncoding.getDifference(i)), glyph.getContentStream());
            }
        }
        getPdfObject().put(PdfName.CharProcs, charProcs);
        getPdfObject().put(PdfName.FontMatrix, new PdfArray(getFontMatrix()));
        getPdfObject().put(PdfName.FontBBox, new PdfArray(fontProgram.getFontMetrics().getBbox()));
        super.flushFontData(null, PdfName.Type3);
    }

//    protected void init() {
//        Rectangle fontBBoxRec = getPdfObject().getAsArray(PdfName.FontBBox).toRectangle();
//        PdfDictionary charProcsDic = getPdfObject().getAsDictionary(PdfName.CharProcs);
//        PdfArray fontMatrixArray = getPdfObject().getAsArray(PdfName.FontMatrix);
//        differences = getPdfObject().getAsDictionary(PdfName.Encoding).getAsArray(PdfName.Differences);
//        if (differences == null) {
//            differences = new PdfArray();
//        }
//        fontProgram.getFontMetrics().getBbox()
//                .setBbox(fontBBoxRec.getX(), fontBBoxRec.getY(), fontBBoxRec.getWidth(), fontBBoxRec.getHeight());
//        int width[] = getWidths();
//        double[] fontMatrix = new double[6];
//        for (int i = 0; i < fontMatrixArray.size(); i++) {
//            fontMatrix[i] = ((PdfNumber) fontMatrixArray.get(i)).getValue();
//        }
//        fontProgram.setFontMatrix(fontMatrix);
//
//        for (int i = 0; i < width.length; i++) {
//            if (width[i] != 0) {
//                fontProgram.addGlyph(i, width[i], null);
//                usedSlot[i] = true;
//            }
//        }
//
//        Iterator<Map.Entry<PdfName, PdfObject>> it = charProcsDic.entrySet().iterator();
//        //TODO switch to by name iteration
//        while (it.hasNext()) {
//            Map.Entry<PdfName, PdfObject> procsEntry = it.next();
//            Integer val = AdobeGlyphList.nameToUnicode(procsEntry.getKey().getValue());
//            if (val != null) {
//                charGlyphs.put(val, new Type3Glyph(((PdfStream) ((PdfIndirectReference) procsEntry.getValue()).getRefersTo())));
//            }
//
//        }
//    }
//
//    private int[] getWidths() {
//        PdfArray newWidths = getPdfObject().getAsArray(PdfName.Widths);
//        PdfNumber first = getPdfObject().getAsNumber(PdfName.FirstChar);
//        PdfNumber last = getPdfObject().getAsNumber(PdfName.LastChar);
//        int f = first.getIntValue();
//        int nSize = f + newWidths.size();
//        int[] tmp = new int[nSize];
//        for (int k = 0; k < newWidths.size(); ++k) {
//            tmp[f + k] = newWidths.getAsInt(k);
//        }
//        return tmp;
//    }
}

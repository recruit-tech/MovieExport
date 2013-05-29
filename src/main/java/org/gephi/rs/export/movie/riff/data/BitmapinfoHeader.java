package org.gephi.rs.export.movie.riff.data;

/**
 * BITMAPINFOHEADER.<br/>
 * WIN32APIのBITMAPINFOHEADER構造体のJavaイメージです。<br/>
 * <a href="http://msdn.microsoft.com/ja-jp/library/vstudio/dd183376.aspx">http://msdn.microsoft.com/ja-jp/library/vstudio/dd183376.aspx</a>
 * @author abe
 */
public class BitmapinfoHeader {
    private long size;
    private int width;
    private int height;
    private int planes;
    private int bitCount;
    private long compression;
    private long sizeImage;
    private int xPelsMeter;
    private int yPelsMeter;
    private long clrUsed;
    private long clrImportant;

    public long getSize() {
        return size;
    }
    public void setSize(long size) {
        this.size = size;
    }
    public int getWidth() {
        return width;
    }
    public void setWidth(int width) {
        this.width = width;
    }
    public int getHeight() {
        return height;
    }
    public void setHeight(int height) {
        this.height = height;
    }
    public int getPlanes() {
        return planes;
    }
    public void setPlanes(int planes) {
        this.planes = planes;
    }
    public int getBitCount() {
        return bitCount;
    }
    public void setBitCount(int bitCount) {
        this.bitCount = bitCount;
    }
    public long getCompression() {
        return compression;
    }
    public void setCompression(long compression) {
        this.compression = compression;
    }
    public long getSizeImage() {
        return sizeImage;
    }
    public void setSizeImage(long sizeImage) {
        this.sizeImage = sizeImage;
    }
    public int getxPelsMeter() {
        return xPelsMeter;
    }
    public void setxPelsMeter(int xPelsMeter) {
        this.xPelsMeter = xPelsMeter;
    }
    public int getyPelsMeter() {
        return yPelsMeter;
    }
    public void setyPelsMeter(int yPelsMeter) {
        this.yPelsMeter = yPelsMeter;
    }
    public long getClrUsed() {
        return clrUsed;
    }
    public void setClrUsed(long clrUsed) {
        this.clrUsed = clrUsed;
    }
    public long getClrImportant() {
        return clrImportant;
    }
    public void setClrImportant(long clrImportant) {
        this.clrImportant = clrImportant;
    }
}

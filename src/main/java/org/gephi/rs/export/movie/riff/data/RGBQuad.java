package org.gephi.rs.export.movie.riff.data;

/**
 * RGBQUAD.<br/>
 * WIN32APIのRGBQUAD構造体のJavaイメージです。<br/>
 * <a href="http://msdn.microsoft.com/ja-jp/library/vstudio/dd162938.aspx">http://msdn.microsoft.com/ja-jp/library/vstudio/dd162938.aspx</a>
 * @author abe
 */
public class RGBQuad {
    private byte blue;
    private byte green;
    private byte red;
    private byte reserved;

    public byte getBlue() {
        return blue;
    }
    public void setBlue(byte blue) {
        this.blue = blue;
    }
    public byte getGreen() {
        return green;
    }
    public void setGreen(byte green) {
        this.green = green;
    }
    public byte getRed() {
        return red;
    }
    public void setRed(byte red) {
        this.red = red;
    }
    public byte getReserved() {
        return reserved;
    }
    public void setReserved(byte reserved) {
        this.reserved = reserved;
    }
}

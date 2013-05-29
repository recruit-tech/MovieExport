package org.gephi.rs.export.movie.riff.data;

/**
 * BITMAPINFO.<br/>
 * WIN32APIのBITMAPINFO構造体のJavaイメージです。<br/>
 * <a href="http://msdn.microsoft.com/ja-jp/library/vstudio/z5731wbz.aspx">http://msdn.microsoft.com/ja-jp/library/vstudio/z5731wbz.aspx</a>
 * @author abe
 */
public class Bitmapinfo {
    private BitmapinfoHeader bmiHeader;
    private RGBQuad bmiColors[];

    public BitmapinfoHeader getBmiHeader() {
        return bmiHeader;
    }
    public void setBmiHeader(BitmapinfoHeader bmiHeader) {
        this.bmiHeader = bmiHeader;
    }
    public RGBQuad[] getBmiColors() {
        return bmiColors;
    }
    public void setBmiColors(RGBQuad[] bmiColors) {
        this.bmiColors = bmiColors;
    }
}

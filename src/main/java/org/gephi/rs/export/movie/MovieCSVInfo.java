/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie;

import java.awt.geom.Rectangle2D;
import java.util.Date;

/**
 * CSV用予備情報クラス。<br/>
 * @author abe
 */
public class MovieCSVInfo {
    public String strCur;
    public String strStart;
    public String strEnd;
    public double dCur;
    public double dStart;
    public double dEnd;
    public Date cur;
    public Date start;
    public Date end;
    public int nodeCnt;
    public int edgeCnt;
    public String svgFilePath;
    // SVGデータ(一部)
    public int width;		// px
    public int height;		// px
    public Rectangle2D.Double viewBox;
}

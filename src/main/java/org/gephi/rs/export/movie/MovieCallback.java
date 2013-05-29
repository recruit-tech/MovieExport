/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.gephi.rs.export.movie;

import org.gephi.filters.api.FilterController;
import org.gephi.filters.api.Query;
import org.gephi.graph.api.Graph;
import org.gephi.layout.spi.Layout;
import java.awt.Graphics;
import org.gephi.rs.export.movie.player.data.GephiBinary;
import org.gephi.rs.export.movie.player.data.GephiBinaryFrame;

/**
 *
 * @author abe
 */
public interface MovieCallback {
    /**
     * フレーム描画。<br/>
     * @param graph グラフ。
     * @param layout レイアウト
     * @param cur 現在タイミング
     * @param start 開始タイミング
     * @param end 終了タイミング
     * @param idx 連番
     * @return 動画処理を続行する場合はtrue、中断する場合はfalse
     */
    public boolean frame(Graph graph, Layout layout, double cur, double start, double end, String idx);

    /**
     * 画像出力直前。<br/>
     * @param graph グラフ。
     * @param graphics 画像を編集する場合に使用してください。
     * @param info 画像の予備情報。
     * @return 動画処理を続行する場合はtrue、中断する場合はfalse
     */
    public boolean frameImage(Graphics graphics, MovieCSVInfo info);

    /**
     * バイナリ出力直前。<br/>
     * @param gb バイナリヘッダ
     * @param gbf フレーム
     * @param info 画像の予備情報。
     * @return 動画処理を続行する場合はtrue、中断する場合はfalse
     */
    public boolean frameBinary(GephiBinary gb, GephiBinaryFrame gbf, MovieCSVInfo info);

    /**
     * Dynamic Rangeフィルタ設定。<br/>
     * Dynamic Rangeフィルタにサブフィルタを追加する場合に使用します。<br/>
     * outputImage()呼び出し時に、GEXFがDynamic形式の場合1回呼び出されます。<br/>
     * @param filterController フィルタコントローラ。
     * @param dynamicQuery Dynamic Rangeフィルタ。
     */
    public void addDynamicFilter(FilterController filterController, Query dynamicQuery);
}
